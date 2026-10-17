# Stage 1: build the Angular app
FROM registry.access.redhat.com/ubi9/nodejs-22 AS build
WORKDIR /opt/app-root/src
COPY --chown=1001:0 package*.json ./
# (npm ci can fail on this lockfile with some npm versions, so use install)
RUN npm install --no-audit --no-fund || npm install --legacy-peer-deps --no-audit --no-fund
COPY --chown=1001:0 . .
RUN npm run build

# Stage 2: serve it with nginx (runs as non-root on 8080, fine for OpenShift)
FROM registry.access.redhat.com/ubi9/nginx-126
COPY --from=build /opt/app-root/src/dist/notes-ui/browser/ /opt/app-root/src/
COPY nginx-default-cfg/ /opt/app-root/etc/nginx.default.d/
CMD ["nginx", "-g", "daemon off;"]
