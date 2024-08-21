# Build Stage 1
# This build created a staging docker image
#
FROM node:20-alpine AS build
WORKDIR /usr/src
RUN apk add git; \
    git clone https://github.com/secvisogram/secvisogram.git; \
    cd secvisogram; \
    npm ci; \
    npm run build

# Build Stage 2
# This build takes the production build from staging build
#

FROM nginx:1.23-alpine
COPY --from=build /usr/src/secvisogram/app/dist /usr/share/nginx/html
EXPOSE 80
