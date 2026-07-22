FROM node:20-alpine AS build

WORKDIR /workspace

ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
ARG NEXT_PUBLIC_AUTH_BASE_URL=http://localhost:9000
ARG NEXT_PUBLIC_AUTH_CLIENT_ID=proofvault-spa
ARG NEXT_PUBLIC_AUTH_REDIRECT_URI=http://localhost:5173/oauth2/callback
ARG NEXT_PUBLIC_AUTH_SCOPES="openid profile email proof:read proof:write"
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_AUTH_BASE_URL=$NEXT_PUBLIC_AUTH_BASE_URL
ENV NEXT_PUBLIC_AUTH_CLIENT_ID=$NEXT_PUBLIC_AUTH_CLIENT_ID
ENV NEXT_PUBLIC_AUTH_REDIRECT_URI=$NEXT_PUBLIC_AUTH_REDIRECT_URI
ENV NEXT_PUBLIC_AUTH_SCOPES=$NEXT_PUBLIC_AUTH_SCOPES

COPY package*.json ./
RUN npm ci

COPY app ./app
COPY lib ./lib
COPY next.config.mjs ./
RUN npm run build

FROM nginx:1.27-alpine

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/out /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost/healthz || exit 1
