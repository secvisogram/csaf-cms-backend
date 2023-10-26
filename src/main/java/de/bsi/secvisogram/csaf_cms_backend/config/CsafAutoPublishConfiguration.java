package de.bsi.secvisogram.csaf_cms_backend.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class CsafAutoPublishConfiguration {

	private boolean enabled = false;
	private boolean enableInsecureTLS = false;
    private String url = "";
    private String password = "";
    private String cron = "0 * * * * *";
    
    public boolean isEnabled() {
		return enabled;
	}

	public CsafAutoPublishConfiguration setEnabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public CsafAutoPublishConfiguration setUrl(String url) {
		this.url = url;
		return this;
	}

	public String getPassword() {
		return password;
	}

	public CsafAutoPublishConfiguration setPassword(String password) {
		this.password = password;
		return this;
	}

	public String getCron() {
		return cron;
	}

	public CsafAutoPublishConfiguration setCron(String cron) {
		this.cron = cron;
		return this;
	}

	public boolean isEnableInsecureTLS() {
		return enableInsecureTLS;
	}

	public CsafAutoPublishConfiguration setEnableInsecureTLS(boolean enableInsecureTLS) {
		this.enableInsecureTLS = enableInsecureTLS;
		return this;
	}
}
