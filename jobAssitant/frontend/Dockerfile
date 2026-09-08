# Build the Vite bundle, then serve it with nginx (also proxies /api to the backend).
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
# npm ci fails because the committed lock file is out of sync with package.json
# (missing transitive peer @floating-ui/dom); npm install resolves it.
RUN npm install

COPY . .
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
