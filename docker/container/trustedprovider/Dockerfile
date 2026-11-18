FROM golang:1.27.3-alpine3.24@sha256:fa3d596898d9f126af7c01d14ee7597bab3f39cccf392ef36b69fe77cd681f18 AS base

RUN apk add bash make wget git
RUN mkdir /app
COPY compile.sh /app/compile.sh
WORKDIR /app
RUN bash /app/compile.sh

# Create provider image
FROM nginx:1.31-alpine@sha256:713fc2ebfd92e8217db451477182ee91944b123d2b1ee9fd5a8c6e91ca6c3a11 AS provider

COPY --from=base /usr/local/go/ /usr/local/go/
COPY --from=base /app/csaf/bin-linux-amd64/csaf_provider /usr/lib/cgi-bin/csaf_provider.go
COPY --from=base /version /version
COPY trustedprovider-nginx.conf /etc/nginx/conf.d/default.conf
COPY fcgiwarp /docker-entrypoint.d/40-fcgiwrap.sh
RUN chmod +x /docker-entrypoint.d/40-fcgiwrap.sh

ENV PATH="/usr/local/go/bin:${PATH}"

RUN apk add --no-cache spawn-fcgi fcgiwrap

EXPOSE 80