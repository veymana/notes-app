# Notes app (Spring Boot + Angular + PostgreSQL) on OpenShift

```
Browser -> Route (TLS) -> notes-ui (nginx, Angular) --/api--> notes-api (Spring Boot) -> notes-db (PostgreSQL)
```

- `backend/`  Spring Boot 3.5 / Java 17 REST API (`/api/notes`, `/api/tags`), built on OpenShift with S2I
- `notes-ui/` Angular 21 UI, built on OpenShift from its Dockerfile, served by nginx
- Replace `<user>` and `<repo>` below with your GitHub details. Use a public repo to keep it simple.

## Run locally

```bash
docker compose up -d                      # Postgres on :5432
cd backend && mvn spring-boot:run         # API on :8080, uses the dev profile by default, uses the dev profile by default
cd notes-ui && npm install && npm start   # UI on :4200 (proxies /api to :8080)
```

### Run from your IDE
Start Postgres (`docker compose up -d`), then run `NotesApiApplication`. The `dev` profile is the default, so no setup is needed
(IntelliJ: Run configuration > Active profiles = `dev` if you want it explicit). Config lives in
`application.properties` (shared), `application-dev.properties` (localhost DB) and `application-prod.properties` (OpenShift, reads env vars).
To test the prod profile locally: `SPRING_PROFILES_ACTIVE=prod SPRING_DATASOURCE_URL=... SPRING_DATASOURCE_USERNAME=... SPRING_DATASOURCE_PASSWORD=... mvn spring-boot:run`.

## GitHub Actions CI

The workflow in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push,
pull request, and manual dispatch from the GitHub Actions tab. It runs two parallel jobs:

- **Backend:** sets up Java 17 and runs `mvn --batch-mode --no-transfer-progress verify` in `backend/`.
- **Frontend:** sets up Node.js 22, installs the locked dependencies with `npm ci`, and builds Angular for production in `notes-ui/`.

Maven and npm dependencies are cached. No repository secrets are required.
There are currently no checked-in tests; Maven will run tests when they are added.
The workflow builds and verifies the code; OpenShift deployment uses the steps below.

## Deploy to OpenShift (Developer Sandbox or CRC)

```bash
# 0. Log in and pick your project
oc login ...            # copy the command from the console: your name > Copy login command
oc project              # confirm the namespace

# 1. PostgreSQL (persistent). Check the template exists first:
oc get templates -n openshift | grep -i postgres
oc new-app --template=postgresql-persistent \
  -p DATABASE_SERVICE_NAME=notes-db -p POSTGRESQL_USER=notes \
  -p POSTGRESQL_PASSWORD='<choose-a-password>' -p POSTGRESQL_DATABASE=notesdb \
  -p VOLUME_CAPACITY=1Gi

# 2. Backend settings (Secret for credentials, ConfigMap for the URL and the prod profile)
oc create secret generic notes-api-db \
  --from-literal=SPRING_DATASOURCE_USERNAME=notes \
  --from-literal=SPRING_DATASOURCE_PASSWORD='<same-password>'
oc create configmap notes-api-config \
  --from-literal=SPRING_DATASOURCE_URL=jdbc:postgresql://notes-db:5432/notesdb \
  --from-literal=SPRING_PROFILES_ACTIVE=prod

# 3. Backend from GitHub (S2I). List Java builder tags and pick a 17 one:
oc get imagestreamtags -n openshift | grep '^java:'
oc new-app java:openjdk-17-ubi9~https://github.com/<user>/<repo>.git \
  --context-dir=backend --name=notes-api
oc set env deployment/notes-api --from=configmap/notes-api-config
oc set env deployment/notes-api --from=secret/notes-api-db
oc set resources deployment/notes-api --requests=cpu=100m,memory=384Mi --limits=cpu=500m,memory=768Mi
oc set probe deployment/notes-api --liveness  --get-url=http://:8080/actuator/health/liveness  --initial-delay-seconds=40
oc set probe deployment/notes-api --readiness --get-url=http://:8080/actuator/health/readiness --initial-delay-seconds=20
oc logs -f bc/notes-api                  # watch the Maven build

# 4. Frontend from GitHub (Dockerfile build). Deploy after the backend Service exists.
oc new-app https://github.com/<user>/<repo>.git --context-dir=notes-ui --strategy=docker --name=notes-ui
oc set resources deployment/notes-ui --requests=cpu=50m,memory=64Mi --limits=cpu=200m,memory=128Mi
oc create route edge notes-ui --service=notes-ui --insecure-policy=Redirect
oc get route notes-ui                    # open the HOST/PORT URL in your browser

# 5. Verify
oc get pods
curl -s https://$(oc get route notes-ui -o jsonpath='{.spec.host}')/api/notes
```

### Redeploy on every `git push`

```bash
oc describe bc/notes-api | grep -A1 -i 'webhook github'   # copy the URL
```
Add it in GitHub: Settings > Webhooks > Add webhook (content type `application/json`).
Repeat for `bc/notes-ui`. Manual option: `oc start-build notes-api`.

### If something fails

| Symptom | Likely cause and fix |
|---|---|
| `notes-api` CrashLoopBackOff | `oc logs deploy/notes-api`. Usually missing or wrong `SPRING_DATASOURCE_*` env, or Postgres not ready yet |
| Pods rejected with "must specify cpu, memory" | Quota is on: run the `oc set resources` commands above |
| `notes-ui` crashes with "host not found in upstream" | The `notes-api` Service doesn't exist yet. Deploy the backend first, then `oc rollout restart deploy/notes-ui` |
| Build can't find a Java builder tag | Use a tag from `oc get imagestreamtags -n openshift \| grep '^java:'` |
| No Postgres template | `oc get templates -n openshift`, or deploy Postgres from the Developer Catalog in the console |
| UI loads but shows "Cannot reach the notes API" | `oc get svc notes-api`, then `oc logs deploy/notes-api` |
