<#
.SYNOPSIS
    Run a JMeter test plan via Docker (no local JMeter install needed).

.DESCRIPTION
    PowerShell equivalent of run.sh for Windows users.
    Supports Rancher Desktop (dockerd or containerd/nerdctl) and Docker Desktop.
    Runs JMeter in non-GUI mode inside a container, writing
    results to jmeter/results/<plan>-<timestamp>/.

.EXAMPLE
    # Default smoke test
    .\jmeter\run.ps1

    # Specific test plan
    .\jmeter\run.ps1 -TestPlan test-plans/create-advisory-performance-test.jmx

    # With extra JMeter properties
    .\jmeter\run.ps1 -TestPlan test-plans/create-advisory-performance-test.jmx -Threads 5 -Loops 20

    # Force a specific container CLI
    .\jmeter\run.ps1 -ContainerCli nerdctl

    # Custom host/port
    .\jmeter\run.ps1 -ApiHost 192.168.1.10 -ApiPort 8080
#>

[CmdletBinding()]
param(
    [string]$TestPlan = "test-plans/smoke-test.jmx",
    [string]$JMeterImage = "justb4/jmeter:5.5",
    [string]$JMeterHeap = "8g",

    # Container CLI: auto-detected by default (docker or nerdctl)
    [ValidateSet("auto", "docker", "nerdctl")]
    [string]$ContainerCli = "auto",

    # Target hosts (auto-detected based on container runtime)
    [string]$ApiHost = "",
    [int]$ApiPort = 8081,
    [string]$KeycloakHost = "",
    [int]$KeycloakPort = 9000,

    # Test parameters
    [int]$Threads = 1,
    [int]$Loops = 1,
    [int]$Rampup = 0,
    [string]$Username = "publisher",
    [string]$Password = "publisher",
    [string]$ClientSecret = "dev-only-3f8a1c7e2b9d4f6a0e5c8b3d7a1f9e2c",

    # Pass-through: any extra -J properties for JMeter (e.g. @("-Jfoo=bar","-Jbaz=1"))
    [string[]]$ExtraArgs = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# --- Resolve paths -----------------------------------------------------------
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$TestPlanFile = Join-Path $ScriptDir $TestPlan

if (-not (Test-Path $TestPlanFile)) {
    Write-Error "Test plan not found: $TestPlanFile"
    exit 1
}

$PlanName = [System.IO.Path]::GetFileNameWithoutExtension($TestPlan)
$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$ResultDir = "results/${PlanName}-${Timestamp}"
$ResultPath = Join-Path $ScriptDir $ResultDir

New-Item -ItemType Directory -Path $ResultPath -Force | Out-Null

# --- Detect container runtime -------------------------------------------------
# Rancher Desktop provides either 'docker' (dockerd/moby) or 'nerdctl' (containerd).
# Docker Desktop provides 'docker'. We auto-detect which CLI is available.

$Cli = $null
$RuntimeName = ""

if ($ContainerCli -eq "auto") {
    # Prefer docker (works with both Docker Desktop and Rancher Desktop dockerd)
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        $Cli = "docker"
    } elseif (Get-Command nerdctl -ErrorAction SilentlyContinue) {
        $Cli = "nerdctl"
    } else {
        Write-Error ("No container CLI found. Install Rancher Desktop or Docker Desktop, " +
                     "and ensure 'docker' or 'nerdctl' is on your PATH.")
        exit 1
    }
} else {
    if (-not (Get-Command $ContainerCli -ErrorAction SilentlyContinue)) {
        Write-Error "'$ContainerCli' not found on PATH."
        exit 1
    }
    $Cli = $ContainerCli
}

# Detect which runtime/desktop is behind the CLI
$DockerNetworkArgs = @()
$HostOverride = "host.docker.internal"

# Temporarily allow errors so 'docker info' stderr doesn't abort the script
$prevEAP = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$cliInfo = $null
try {
    $cliInfo = cmd /c "$Cli info 2>&1"
    $cliInfo = $cliInfo -join "`n"
} catch { }
$ErrorActionPreference = $prevEAP

if ([string]::IsNullOrWhiteSpace($cliInfo)) {
    Write-Error "$Cli is not running. Start Rancher Desktop first."
    exit 1
}

if ($cliInfo -match "(?i)rancher.?desktop|Version:.*-rd\b") {
    $RuntimeName = "Rancher Desktop"
    $DockerNetworkArgs += "--add-host=host.docker.internal:host-gateway"
} elseif ($cliInfo -match "(?i)docker desktop|operating system.*desktop") {
    $RuntimeName = "Docker Desktop"
    $DockerNetworkArgs += "--add-host=host.docker.internal:host-gateway"
} else {
    # Native Docker engine (e.g. plain WSL2 dockerd) — use host network
    $RuntimeName = "Native Docker"
    $DockerNetworkArgs += "--network=host"
    $HostOverride = "localhost"
}

Write-Host "  Runtime:    $RuntimeName ($Cli)" -ForegroundColor DarkGray

if (-not $ApiHost)      { $ApiHost      = $HostOverride }
if (-not $KeycloakHost) { $KeycloakHost = $HostOverride }

# --- Health check -------------------------------------------------------------
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " JMeter API Test Runner (PowerShell)"    -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Image:      $JMeterImage"
Write-Host "  Test plan:  $TestPlan"
Write-Host "  Results:    jmeter/$ResultDir/"
Write-Host "  CLI:        $Cli ($RuntimeName)"
Write-Host "  API:        http://${ApiHost}:${ApiPort}"
Write-Host "  Keycloak:   http://${KeycloakHost}:${KeycloakPort}"
Write-Host "  Threads:    $Threads  Loops: $Loops  Ramp-up: ${Rampup}s"
Write-Host "========================================" -ForegroundColor Cyan

# Determine the compose command for the hint message
$ComposeCmd = if ($Cli -eq "nerdctl") { "nerdctl compose" } else { "$Cli compose" }

try {
    $response = Invoke-WebRequest -Uri "http://localhost:${ApiPort}/api/v1/about" `
        -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
} catch {
    Write-Host ""
    Write-Host "  WARNING: Backend at http://localhost:${ApiPort} is not responding." -ForegroundColor Yellow
    Write-Host "           Make sure '$ComposeCmd -f docker/compose.yaml up -d' is running." -ForegroundColor Yellow
    Write-Host ""
}

# --- Build docker run command -------------------------------------------------
# Volume mount: map the jmeter/ directory into the container at /jmeter
$VolumeMount = "${ScriptDir}:/jmeter:rw"

$DockerArgs = @(
    "run", "--rm"
) + $DockerNetworkArgs + @(
    "-e", "JVM_ARGS=-Xms512m -Xmx${JMeterHeap}"
    "-v", $VolumeMount
    $JMeterImage
    "-n"
    "-t", "/jmeter/$TestPlan"
    "-l", "/jmeter/$ResultDir/results.jtl"
    "-j", "/jmeter/$ResultDir/jmeter.log"
    "-e", "-o", "/jmeter/$ResultDir/report"
    "-JCookieManager.save.cookies=true"
    "-Japi.host=$ApiHost"
    "-Japi.port=$ApiPort"
    "-Jkeycloak.host=$KeycloakHost"
    "-Jkeycloak.port=$KeycloakPort"
    "-Jthreads=$Threads"
    "-Jloops=$Loops"
    "-Jrampup=$Rampup"
    "-Jusername=$Username"
    "-Jpassword=$Password"
    "-Jclient.secret=$ClientSecret"
) + $ExtraArgs

Write-Host ""
Write-Host "Starting JMeter..." -ForegroundColor Green
Write-Host ""

# --- Execute ------------------------------------------------------------------
& $Cli @DockerArgs
$ExitCode = $LASTEXITCODE

# --- Report -------------------------------------------------------------------
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($ExitCode -eq 0) {
    Write-Host " JMeter finished successfully." -ForegroundColor Green
} else {
    Write-Host " JMeter exited with code $ExitCode." -ForegroundColor Red
}
Write-Host "  Results:     jmeter/$ResultDir/results.jtl"
Write-Host "  HTML report: jmeter/$ResultDir/report/index.html"
Write-Host "  Log:         jmeter/$ResultDir/jmeter.log"
Write-Host "========================================" -ForegroundColor Cyan

# Open HTML report in default browser if test succeeded
if ($ExitCode -eq 0) {
    $ReportHtml = Join-Path $ResultPath "report/index.html"
    if (Test-Path $ReportHtml) {
        Write-Host ""
        Write-Host "Opening HTML report in browser..." -ForegroundColor DarkGray
        Start-Process $ReportHtml
    }
}

exit $ExitCode
