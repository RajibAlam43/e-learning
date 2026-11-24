FROM node:24-bookworm-slim AS dependencies
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

FROM dependencies AS build
ARG NEXT_PUBLIC_APP_URL
ARG NEXT_PUBLIC_API_URL
ARG NEXT_PUBLIC_GOOGLE_CLIENT_ID=""
ARG NEXT_PUBLIC_BUILD_ID=local
ARG IMAGE_REMOTE_HOSTS=""
ENV NEXT_TELEMETRY_DISABLED=1 \
    NEXT_PUBLIC_APP_URL=${NEXT_PUBLIC_APP_URL} \
    NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL} \
    NEXT_PUBLIC_GOOGLE_CLIENT_ID=${NEXT_PUBLIC_GOOGLE_CLIENT_ID} \
    NEXT_PUBLIC_BUILD_ID=${NEXT_PUBLIC_BUILD_ID} \
    API_URL=${NEXT_PUBLIC_API_URL} \
    IMAGE_REMOTE_HOSTS=${IMAGE_REMOTE_HOSTS}
# Copy only source inputs; CI output in .next is used by the separate ci target.
COPY next.config.ts tsconfig.json postcss.config.mjs ./
COPY src ./src
COPY messages ./messages
COPY public ./public
RUN test -n "$NEXT_PUBLIC_APP_URL" && test -n "$NEXT_PUBLIC_API_URL" && npm run build

FROM node:24-bookworm-slim AS runtime
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0
RUN mkdir -p .next && chown node:node .next
USER node
EXPOSE 3000
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=4 \
    CMD node -e "fetch('http://127.0.0.1:3000/robots.txt').then(r => process.exit(r.ok ? 0 : 1)).catch(() => process.exit(1))"
CMD ["node", "server.js"]

# GitHub's Linux amd64 runner builds/checks the app before packaging it here.
FROM runtime AS ci
COPY --chown=node:node .next/standalone ./
COPY --chown=node:node .next/static ./.next/static
COPY --chown=node:node public ./public

# Default target supports a complete build without local npm dependencies/output.
FROM runtime AS standalone
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
COPY --from=build --chown=node:node /app/public ./public
