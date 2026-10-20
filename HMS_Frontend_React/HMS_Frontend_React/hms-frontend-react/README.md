# CarePoint HMS: React frontend

React 18 + Vite 6 single-page app for the Spring Boot HMS API. It uses the backend's real routes and
fields and does not replace the backend or the database.

## Run

```bash
npm install
npm run dev            # http://localhost:5173, proxies /api and /swagger-ui to http://localhost:8080
npm run build          # production build in dist/
npm run preview
```

Set `VITE_BACKEND_URL` to point the dev proxy at another backend. Set `VITE_API_BASE_URL` (default `/api/v1`)
when the API is served from a different origin; that origin then needs CORS configured on the backend.

## Tests

```bash
npm test                                                  # offline: formatting, client, login page
HMS_LIVE=1 npx vitest run src/live.integration.test.jsx   # real app against the running backend
```

The live tests need the backend on port 8080, an `admin` account (`ChangeMe!12345`) and a
`rec1` / `password1` receptionist. Adjust the credentials in `src/live.integration.test.jsx` to match your
database.

## Structure

```
src/
  api/          axios client (adds the bearer token, handles 401) and one function per endpoint
  auth/         AuthContext (session, profile, avatar) and route guards (login, role)
  config/       resources.js: columns, form fields and API calls for each CRUD page
  components/   layout, shared UI, field renderer
  pages/        login, dashboard, generic resource page, prescriptions, profile, user accounts, status pages
  utils/        formatting helpers and tests
```

Adding a CRUD resource means adding an entry to `config/resources.js` and a route in `App.jsx`.

## Behaviour notes

- A 401 from any call except login signs the user out. A 403 shows a permission message and does not sign out.
- Delete buttons are shown to administrators only. Other roles get a permission error from the server if they try.
- Room status follows admissions: OCCUPIED is only offered for a room that is already occupied.
- Admission edits only offer free rooms, plus the room the admission currently uses.
- Appointment and admission filters apply to the current page only, because the API has no filter parameters for them.
- Prescriptions have no list endpoint, so the page offers create and lookup by ID.
