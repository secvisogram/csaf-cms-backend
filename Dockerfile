FROM eclipse-temurin:25-jdk@sha256:884a2f8692cc1e2c7e21f95914adf0ae0eafee9a7eb663a12169cc44468256ec AS build

COPY . /build
WORKDIR /build

# Normalize potential CRLF line endings (e.g. from a Windows checkout) so the mvnw shebang works.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

RUN ./mvnw dependency:copy-dependencies -DoutputDirectory=./target/dependency
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:25-jre@sha256:945247f9d092703e0c6be92243ef3c51640229e23ab34d353c8dcdc73e6cd4e6

ARG MAINTAINER
ARG REPO_URL=https://github.com/secvisogram/csaf-cms-backend
LABEL org.opencontainers.image.authors="${MAINTAINER}"
LABEL org.opencontainers.image.source="${REPO_URL}"

RUN mkdir /app
WORKDIR /app

RUN apt-get -y update && \
	apt-get -y --no-install-recommends install weasyprint pandoc && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/*


COPY  --from=build /build/target/*.jar csaf-cms-backend.jar
COPY  --from=build /build/target/dependency .
EXPOSE 8081

# Interim mitigation for CVE-2026-114938 (Java4Shell, OpenJDK AVX-512 Base64 decode intrinsic);
# remove once the eclipse-temurin:25-jre base image ships 25.0.5.1 or later.
ENV JAVA_TOOL_OPTIONS="-XX:-UseBASE64Intrinsics"

ENTRYPOINT ["java", "-jar", "csaf-cms-backend.jar"]
