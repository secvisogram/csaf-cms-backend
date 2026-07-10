# Build Stage 1
# This build created a staging docker image
#
FROM node:24-alpine AS build
WORKDIR /usr/src/csaf-validator-service
RUN apk add git; \
    git clone https://github.com/secvisogram/csaf-validator-service.git .; \
    npm ci; \
    npm run dist

# Build Stage 2
# This build takes the production build from staging build
#
FROM node:24-alpine
WORKDIR /usr/src/app

RUN apk add hunspell hunspell-en hunspell-de-de && \
    ln -s /usr/share/hunspell/en_US.aff /usr/share/hunspell/en.aff && \
    ln -s /usr/share/hunspell/en_US.dic /usr/share/hunspell/en.dic && \
    ln -s /usr/share/hunspell/de_DE.aff /usr/share/hunspell/de.aff && \
    ln -s /usr/share/hunspell/de_DE.dic /usr/share/hunspell/de.dic

ENV NODE_ENV=production

COPY --from=build /usr/src/csaf-validator-service/dist /usr/src/app
COPY --from=build /usr/src/csaf-validator-service/backend/config/development.json /usr/src/app/config/local-production.json

ENV LANG=en

USER node
EXPOSE 8082
CMD ["node", "backend/server.js"]
