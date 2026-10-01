# Build the Vite bundle, then serve it with nginx.
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm install

COPY . .

ARG VITE_API_BASE_URL
ARG VITE_API_USERNAME
ARG VITE_API_PASSWORD
RUN VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_API_USERNAME=$VITE_API_USERNAME \
    VITE_API_PASSWORD=$VITE_API_PASSWORD \
    npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
