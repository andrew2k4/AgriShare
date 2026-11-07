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
