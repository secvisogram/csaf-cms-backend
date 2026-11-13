FROM eclipse-temurin:25-jdk@sha256:83f4a7fc27df94dcc7b8de04d14cbbba8da14406f921e966969c3b09000aa507 AS build

COPY . /build
WORKDIR /build

# Normalize potential CRLF line endings (e.g. from a Windows checkout) so the mvnw shebang works.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

RUN ./mvnw dependency:copy-dependencies -DoutputDirectory=./target/dependency
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:25-jre@sha256:ecdc0adb37bde1a281c4d245a3cd535004b1fc8322d560f5b09463b248c81df0

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

ENTRYPOINT ["java", "-jar", "csaf-cms-backend.jar"]
