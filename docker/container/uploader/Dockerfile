ARG GO_VERSION=1.25.3
ARG ALPINE_VERSION=3.22

FROM golang:${GO_VERSION}-alpine${ALPINE_VERSION} AS base

RUN apk add bash make wget git
RUN mkdir /app
COPY compile.sh /app/compile.sh
WORKDIR /app
RUN bash /app/compile.sh

# Create provider image
FROM golang:${GO_VERSION}-alpine${ALPINE_VERSION} AS csaf_uploader

COPY --from=base /app/csaf_distribution/bin-linux-amd64/csaf_uploader /opt/csaf_uploader
COPY --from=base /version /version
