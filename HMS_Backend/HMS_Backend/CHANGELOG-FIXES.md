# Fixes and verification log

## Backend (Spring Boot)

| # | Problem | Fix |
|---|---|---|
| 1 | Startup failed: 11 entity primary keys mapped to `*_id` columns that do not exist (`missing column [admission_id]`) | Removed the wrong `@Column(name=…)` from the `@Id` fields |
| 2 | Discharge did not free the room, and billing threw NPEs: public-field reads on lazy Hibernate proxies return null | Admissions are loaded with `findByIdWithDetails` (fetch joins) in discharge, delete, update and billing; prescriptions fetch their admission |
| 3 | Forbidden actions returned 401, so the frontend signed out logged-in users | Explicit 403 access-denied handler |
| 4 | Lacking a token returned 403 | Authentication entry point returns 401 |
| 5 | Login failures and access denials returned 500 | Global handler maps authentication → 401, access denied → 403, unique/foreign-key violations → 409, bad parameters and sort fields → 400, other errors → generic 500 (no internal details) |
| 6 | Appointment status endpoint skipped the state machine | Uses `HmsService.transition` |
| 7 | Discharge could run twice and overwrite `dischargedAt` | Uses `HmsService.discharge`, which rejects a second discharge |
| 8 | Deleting an active admission left its room occupied | Room released in `removeAdmission` |
| 9 | Moving an admission to another room did not update room status | `updateAdmission` releases the old room and occupies the new one |
| 10 | Editing a room with no `status` reset an occupied room to AVAILABLE | Room status is driven by admissions (`applyRoomStatus`) |
| 11 | Admission bills did not check the patient; cancelled appointments could be billed | Both checks added |
| 12 | Sort parameter ignored on appointments, admissions, bills, patients | `Paging` helper applies `field,asc|desc`, clamps page size to 100 |
| 13 | Patient search ran the query twice and returned all matches as one page | Real paged repository query |
| 14 | Double-booking check did not exclude the appointment being edited | `existsBy…AndIdNot` used on update |
| 15 | Duplicate username/email returned 500 | Checked first; returns 409 |
| 16 | Prescription items were not validated (null quantity → NPE) | `@Valid` on items with constraints |
| 17 | Any signed-in user could not remove their own profile photo (DELETE was admin-only) | Explicit rule for `DELETE /api/v1/auth/me/avatar` |
| 18 | `JwtFilter` ran twice (component plus chain) | Servlet registration disabled |
| 19 | `ClinicalControllerTest` did not compile against the current constructor and method | Test rewritten; added state-machine and paging tests |

## Verification

- All 84 main and test classes compile with `javac -parameters`.
- JUnit: 14 tests pass.
- Application boots on a clean PostgreSQL 16 database; Flyway applies V1–V4; Hibernate `validate` passes.
- `scripts/api-smoke-test.py`: 32/32 checks pass against the running backend.
- React app: production build succeeds; 11 offline unit tests pass; 4 live UI tests pass in jsdom against the backend (login, patient registration, appointment confirmation, receptionist restrictions, admin-only page).

## Not verified

- Maven itself was not run: Maven Central was unreachable from the build environment, so compilation used `javac` with the jars in the bundled repository.
- Browser (Chromium) testing was not possible; the UI tests run in jsdom.
- Appointment overlap checks compare the exact start time only.
