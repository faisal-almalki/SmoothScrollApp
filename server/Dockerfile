# syntax=docker/dockerfile:1

# ---- build ----------------------------------------------------------------
# Compiles TypeScript with the dev dependencies, which are then thrown away.
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY tsconfig.json drizzle.config.ts ./
COPY src ./src
COPY scripts ./scripts
RUN npm run build

# The migration runner resolves its SQL relative to its own directory, so from
# dist/scripts/ it looks for dist/drizzle/. Putting the files there is what lets
# the deployed image run migrations without tsx or the TypeScript sources.
COPY drizzle ./dist/drizzle

# ---- runtime --------------------------------------------------------------
FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production

# Production dependencies only: no tsc, no tsx, no PGlite. That is roughly a
# quarter of the image and none of it is reachable at runtime.
COPY package.json package-lock.json ./
RUN npm ci --omit=dev && npm cache clean --force

COPY --from=build /app/dist ./dist

# Node's own unprivileged user, so a remote-code bug does not land as root.
USER node

EXPOSE 8787
ENV PORT=8787

# Two reasons not to run migrations here: a rollout starts several containers
# at once and they would race, and a migration that fails should stop a deploy
# rather than crash-loop it. `fly deploy` runs them once, as a release command.
CMD ["node", "dist/src/index.js"]
