FROM golang:1.27.4-alpine3.24@sha256:70bd7ddea8b6cbe5d6943cde279d6b67feb10cc924fb9100d1d8757c1575b280 AS base

RUN apk add bash make wget git
RUN mkdir /app
COPY compile.sh /app/compile.sh
WORKDIR /app
ARG TAG=v3.7.1
ENV TAG=${TAG}
RUN bash /app/compile.sh

# Create provider image
FROM nginx:1.31-alpine@sha256:0563c36f94a6d3ec3584161b77859f740f7e20b4853f52c796a864ce6908e8ea AS provider

COPY --from=base /usr/local/go/ /usr/local/go/
COPY --from=base /app/csaf/bin-linux-amd64/csaf_provider /usr/lib/cgi-bin/csaf_provider.go
COPY --from=base /version /version
COPY trustedprovider-nginx.conf /etc/nginx/conf.d/default.conf
COPY fcgiwarp /docker-entrypoint.d/40-fcgiwrap.sh
RUN chmod +x /docker-entrypoint.d/40-fcgiwrap.sh

ENV PATH="/usr/local/go/bin:${PATH}"

RUN apk add --no-cache spawn-fcgi fcgiwrap

EXPOSE 80