FROM eclipse-temurin:21-jdk AS build

COPY . /build
WORKDIR /build

RUN ./mvnw dependency:copy-dependencies -DoutputDirectory=./target/dependency
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre

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


COPY  --from=build /build/target/csaf-cms-backend-1.0.0.jar csaf-cms-backend.jar
COPY  --from=build /build/target/dependency .
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "csaf-cms-backend.jar"]
