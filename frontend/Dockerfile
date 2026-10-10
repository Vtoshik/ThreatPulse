FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
# The postinstall script (quasar prepare) needs the sources, which are not copied yet.
# The build below prepares everything it needs.
RUN npm ci --quiet --ignore-scripts
COPY . .
# The browser, not the container, calls the backend, so these are the addresses
# as seen from the user's machine. They are baked into the bundle at build time.
ARG VITE_API_URL=http://localhost:8080
ARG VITE_WS_URL=ws://localhost:8080/ws
ENV VITE_API_URL=$VITE_API_URL VITE_WS_URL=$VITE_WS_URL
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist/spa /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
