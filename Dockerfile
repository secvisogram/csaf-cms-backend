FROM eclipse-temurin:25.0.5.1_2-jdk@sha256:ce2ef9d2d67a68de5e196d4de9d743327c44a10b57fe02ec39f572c652cacfd3 AS build

COPY . /build
WORKDIR /build

# Normalize potential CRLF line endings (e.g. from a Windows checkout) so the mvnw shebang works.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

RUN ./mvnw dependency:copy-dependencies -DoutputDirectory=./target/dependency
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:25.0.5.1_2-jre@sha256:4ed132f6832e9804b123e89a849a7b4502756ae64a4ee08ebab939d9d5d0a9fa

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
