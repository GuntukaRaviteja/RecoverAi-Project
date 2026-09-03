# RecoverAI

RecoverAI is an AI-assisted failed-payment recovery platform with a Spring
Boot backend, MySQL persistence, and a React/Vite operations dashboard.

## Projects

- `recoverai-backend` — Spring Boot REST API and recovery engine
- `recoverai-frontend/recoverai-frontend` — React/Vite dashboard

See `recoverai-backend/README.md` for architecture, setup, recovery states,
analytics definitions, and API endpoints.

## Quick start

Start the backend on port `8080`:

```bash
cd recoverai-backend
./mvnw spring-boot:run
```

Start the frontend:

```bash
cd recoverai-frontend/recoverai-frontend
npm install
npm run dev
```

The frontend uses `http://localhost:8080` by default. Configure
`VITE_API_URL` in a local `.env.local` file when needed.

Do not commit real credentials. Use environment variables or a secret manager.
