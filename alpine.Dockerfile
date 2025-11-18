FROM eclipse-temurin:21-jdk-alpine AS build

COPY . /build
WORKDIR /build

RUN ./mvnw dependency:copy-dependencies -DoutputDirectory=./target/dependency
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine

ARG MAINTAINER
ARG REPO_URL=https://github.com/secvisogram/csaf-cms-backend
LABEL org.opencontainers.image.authors="${MAINTAINER}"
LABEL org.opencontainers.image.source="${REPO_URL}"

RUN mkdir /app
WORKDIR /app

RUN apk add --no-cache weasyprint pandoc 

COPY  --from=build /build/target/csaf-cms-backend-1.0.0.jar csaf-cms-backend.jar
COPY  --from=build /build/target/dependency .
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "csaf-cms-backend.jar"]
