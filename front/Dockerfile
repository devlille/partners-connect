# Utiliser une image Node.js officielle comme base
FROM node:22-alpine AS base

# Installer les dépendances système nécessaires et pnpm
RUN apk add --no-cache libc6-compat && \
    corepack enable && \
    corepack prepare pnpm@latest --activate

# Définir le répertoire de travail
WORKDIR /app

# Stage de construction
FROM base AS builder

# Définir les variables d'environnement pour le build
ENV NODE_ENV=production
ENV NODE_OPTIONS="--max-old-space-size=4096"

# Copier les fichiers de dépendances
COPY package.json pnpm-lock.yaml ./

# Installer toutes les dépendances (y compris devDependencies)
RUN pnpm install --frozen-lockfile

# Copier le reste des fichiers de l'application
COPY . .

# Construire l'application Nuxt
RUN pnpm run build

# Stage de production
FROM base AS runner

# Définir les variables d'environnement
ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PORT=8080

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nuxtjs -u 1001

# Copier les fichiers nécessaires depuis le builder
COPY --from=builder --chown=nuxtjs:nodejs /app/.output /app/.output
COPY --from=builder --chown=nuxtjs:nodejs /app/public /app/public

# Changer vers l'utilisateur non-root
USER nuxtjs

# Exposer le port
EXPOSE 8080

# Démarrer l'application
CMD ["node", ".output/server/index.mjs"]
