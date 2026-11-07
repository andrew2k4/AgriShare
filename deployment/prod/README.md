# Production deployment (agrishare.farm)

Stack: Postgres + Spring Boot backend + Next.js frontend, behind Nginx terminating
TLS with a Cloudflare Origin Certificate. `app.agrishare.farm` → frontend,
`api.agrishare.farm` → backend.

## One-time setup on the VPS

1. Clone the repo to `/opt/agrishare/repo`.
2. Create `/opt/agrishare/repo/secrets/` (gitignored) containing:
   - `firebase-service-account.json` — Firebase Admin SDK service account key.
   - `cloudflare-origin.pem` — Cloudflare Origin Certificate (public cert).
   - `cloudflare-origin.key` — its private key.
3. Copy `deployment/prod/.env.example` to `deployment/prod/.env` and fill in real values
   (DB password, `CORS_ALLOWED_ORIGINS`, `NEXT_PUBLIC_API_URL`).
4. `cd deployment/prod && docker compose up -d --build`

## Redeploying after a push to `main`

```bash
/opt/agrishare/repo/deployment/prod/deploy.sh
```

## Notes

- `NEXT_PUBLIC_API_URL` is baked into the frontend at **build time** (Next.js
  inlines `NEXT_PUBLIC_*` vars) — changing it requires rebuilding the frontend
  image, not just restarting the container.
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` in prod: schema changes go through
  Flyway migrations (`services/backend/src/main/resources/db/migration`), never
  through Hibernate auto-DDL.
- Nginx only accepts the Cloudflare Origin Certificate; the VPS firewall should
  ideally also restrict ports 80/443 to Cloudflare's published IP ranges so the
  origin can't be hit directly, bypassing Cloudflare.

## Moving accounts off Firebase

1. Set `FIREBASE_WEB_API_KEY` in `.env` (the public `apiKey` of the Firebase project) and
   redeploy.
2. As an admin, call `POST /api/admin/firebase/import` once (Swagger, with a Firebase
   bearer token). It creates the missing `app_users` rows, copies the "e-mail verified"
   state and suspends accounts disabled in Firebase. It is idempotent and never touches
   passwords.
3. Each Firebase user keeps their password: at their first password login the backend
   checks it with Firebase, then stores its own hash.
4. After the transition period (60 days by default), remove `FIREBASE_WEB_API_KEY`.
   Accounts that never logged in again use "forgot password".

Notes:
- The key is called from the server, which sends no Referer. If the frontend's key is
  restricted to HTTP referrers in Google Cloud, Firebase refuses it: logins of accounts still
  to migrate then answer 503 and the backend logs an ERROR naming the cause. Use a key with
  no referrer restriction, limited to the "Identity Toolkit API".
- During the transition, a login for an account still to migrate takes a little longer (one
  call to Firebase), so response times differ for those addresses. Firebase's own public API
  already tells, with the same public key, whether an address has an account (unless e-mail
  enumeration protection is turned on in the Firebase console, which is recommended).
- Re-run the import after the transition starts if needed: it also copies e-mail changes
  made in Firebase. Its report lists accounts disabled in Firebase but active here, and
  e-mails already used by another account: those are left for an admin to decide.
