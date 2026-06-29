ARG GO_VERSION=1.25.3
ARG ALPINE_VERSION=3.22

FROM golang:${GO_VERSION}-alpine${ALPINE_VERSION} AS base

RUN apk add bash make wget git
RUN mkdir /app
COPY compile.sh /app/compile.sh
WORKDIR /app
RUN bash /app/compile.sh

# Create provider image
FROM  nginx:alpine AS provider

COPY --from=base /usr/local/go/ /usr/local/go/
COPY --from=base /app/csaf_distribution/bin-linux-amd64/csaf_provider /usr/lib/cgi-bin/csaf_provider.go
COPY --from=base /version /version
COPY trustedprovider-nginx.conf /etc/nginx/conf.d/default.conf
COPY fcgiwarp /docker-entrypoint.d/40-fcgiwrap.sh

ENV PATH="/usr/local/go/bin:${PATH}"

RUN apk add --no-cache spawn-fcgi fcgiwrap

EXPOSE 80