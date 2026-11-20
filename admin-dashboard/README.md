# Codigos Client Remote Admin Dashboard

A mobile-friendly web dashboard inside the repository for publishing configuration updates to GitHub (`remote-config/app-config.json`).

## Security
- Never place your GitHub Personal Access Token (PAT) in the Android APK or frontend source files.
- Run the dashboard server with environment variables from `.env.example` so `GITHUB_TOKEN` stays strictly server-side, protected behind `ADMIN_PASSWORD`.

## Running Locally
```bash
cd admin-dashboard
cp .env.example .env
# Edit .env with your GITHUB_TOKEN, GITHUB_OWNER, GITHUB_REPO, and ADMIN_PASSWORD
node --env-file=.env server.mjs
```
Then open `http://localhost:4173` on desktop or mobile.

## Connecting the Android App
In **Codigos Client** -> **Settings** -> **Remote Config URL**, set your public raw GitHub URL:
`https://raw.githubusercontent.com/<owner>/<repo>/main/remote-config/app-config.json`
and tap **Sync** (or reopen the app).
