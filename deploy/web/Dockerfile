FROM node:22.14.0-alpine AS web-build
ARG CREHN_ADMIN_NODE_OPTIONS="--max-old-space-size=3072"
ARG CREHN_PORTAL_NODE_OPTIONS="--max-old-space-size=1024"
RUN corepack enable
WORKDIR /src/frontend
COPY frontend/package.json frontend/pnpm-lock.yaml frontend/pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY frontend/ .
RUN NODE_OPTIONS="${CREHN_ADMIN_NODE_OPTIONS}" pnpm test \
    && NODE_OPTIONS="${CREHN_ADMIN_NODE_OPTIONS}" pnpm build:prod

WORKDIR /src/portal
COPY portal/package.json portal/pnpm-lock.yaml portal/pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY portal/ .
RUN NODE_OPTIONS="${CREHN_PORTAL_NODE_OPTIONS}" pnpm build

FROM nginx:1.27.4-alpine
ARG CREHN_VERSION=local
ARG CREHN_SOURCE_REVISION=UNVERSIONED
LABEL org.opencontainers.image.title="CREHN Web" \
      org.opencontainers.image.version="${CREHN_VERSION}" \
      org.opencontainers.image.revision="${CREHN_SOURCE_REVISION}"
COPY deploy/web/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=web-build /src/frontend/dist /usr/share/nginx/html/admin
COPY --from=web-build /src/portal/dist /usr/share/nginx/html
EXPOSE 8080
