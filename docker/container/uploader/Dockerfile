FROM golang:1.27.3-alpine3.24@sha256:fa3d596898d9f126af7c01d14ee7597bab3f39cccf392ef36b69fe77cd681f18 AS base

RUN apk add bash make wget git
RUN mkdir /app
COPY compile.sh /app/compile.sh
WORKDIR /app
RUN bash /app/compile.sh

# Create provider image
FROM golang:1.27.3-alpine3.24@sha256:fa3d596898d9f126af7c01d14ee7597bab3f39cccf392ef36b69fe77cd681f18 AS csaf_uploader

COPY --from=base /app/csaf/bin-linux-amd64/csaf_uploader /opt/csaf_uploader
COPY --from=base /version /version
