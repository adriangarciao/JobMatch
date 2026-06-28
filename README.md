# JobMatch

[![CI](https://github.com/adriangarciao/JobMatch/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/adriangarciao/JobMatch/actions/workflows/maven.yml)

A full-stack Spring Boot application that helps job seekers see how well their resume matches a job posting. It scores the match with a deterministic, weighted skill-matching engine and heuristic text parsing — no external LLM is called.

**Live demo:** https://adriangarciao-jobmatch.vercel.app

## How the matching works

There is no hosted LLM behind this project. The scoring is produced by a deterministic engine (`FakeLLMService`) that compares parsed skills and text overlap. It sits behind an `LLMService` interface, so a real model-backed implementation can be dropped in later without changing the controllers or frontend.

## Features

### In the web app (no sign-in required)
- **Resume vs. job analysis** via `POST /api/ai/analyze`:
  - Match score on a 0–100 scale (70% skill overlap, 30% text overlap)
  - Strengths (matched core skills) and weaknesses (missing core skills)
  - Actionable suggestions for improvement
  - Optional cover-letter suggestions (a tailoring hint, not a generated letter)
- **Resume import**: paste text, or import a `.txt`/`.md` file, or a PDF (parsed in-browser with `pdfjs-dist`, with a server-side fallback)
- **Job metadata extraction**: best-effort location and compensation detection from the posting
- **Smart skill recognition**: core technical skills with context-aware matching (e.g. `golang` is recognized, plain `go` is ignored as a common verb)

### Backend API only (require authentication; not yet surfaced in the demo UI)
- **Resume management**: upload, store, list, download, and delete resumes (server-side text extraction via Apache Tika / PDFBox / POI)
- **Job application tracking**: create, update, list (paged), search/filter, and delete applications with status management
- **Authentication**: JWT access tokens with rotating refresh tokens (HttpOnly cookie)

## Demo

Screenshots of the frontend and analysis results live under `frontend/public/images`.

- **Frontpage**: the landing page.

  ![Frontpage](frontend/public/images/frontpage.png)

- **Frontpage (Filled Out)**: the form with resume and job posting filled in.

  ![Frontpage Filled Out](frontend/public/images/frontpageFilledOut.png)

- **Results (example 1)**: sample analysis output.

  ![Results 1](frontend/public/images/result1.png)

- **Results (example 2)**: another example of analysis output.

  ![Results 2](frontend/public/images/result2.png)

## Tech Stack

**Backend:**
- Java 21
- Spring Boot 3.5.6 (Web, Data JPA, Security, Validation)
- PostgreSQL
- Flyway (database migrations)
- JWT authentication (jjwt) with refresh-token rotation
- MapStruct (object mapping) and Lombok
- Apache Tika / PDFBox / POI (server-side resume text extraction)

**Frontend:**
- React 19 + Vite
- `pdfjs-dist` for in-browser PDF parsing
- Modern CSS with design tokens

## Getting Started

### Prerequisites

- Java 21 or higher
- PostgreSQL 17+ running locally on port 5432
- Maven 3.9+ (or use the included Maven wrapper)
- Node.js 18+ (for the frontend)

### Database Setup

Create a PostgreSQL database:
```sql
CREATE DATABASE jobassistant;
```

The datasource reads `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD` (defaults: `localhost:5432/jobassistant`, user `postgres`). A `JWT_SECRET` environment variable is required to start the backend. See `src/main/resources/application.properties`.

### Running the Backend

```powershell
# Windows
.\mvnw.cmd spring-boot:run
```
```bash
# Mac/Linux
./mvnw spring-boot:run
```

The backend starts on `http://localhost:8080`.

### Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on `http://localhost:5173`. Set `VITE_API_URL` to point at the backend (e.g. `http://localhost:8080`).

### Running Tests

```powershell
# Run all tests (Windows)
.\mvnw.cmd test

# Run a specific test class (Windows)
.\mvnw.cmd test -Dtest=FakeLLMServiceTest

# Run with coverage (Windows)
.\mvnw.cmd clean test
```
```bash
# Run all tests (Mac/Linux)
./mvnw test

# Run a specific test class (Mac/Linux)
./mvnw test -Dtest=FakeLLMServiceTest

# Run with coverage (Mac/Linux)
./mvnw clean test
```

Tests use an in-memory H2 database. **Test suite: 85 tests** across unit and integration coverage, run on every push/PR via GitHub Actions. JaCoCo runs during the `test` phase; the coverage report is written to `target/site/jacoco/index.html`.

## API Endpoints

### Analysis (public)
- `POST /api/ai/analyze` — Analyze a resume against a job posting

### Authentication (public)
- `POST /api/auth/register` — Register a new user
- `POST /api/auth/login` — Log in
- `POST /api/auth/refresh` — Rotate the refresh token and issue a new access token
- `POST /api/auth/logout` — Revoke refresh tokens
- `GET /api/auth/verify` — Introspect a bearer token

### Resumes (authenticated)
- `POST /api/resumes` — Upload and store a resume (multipart)
- `POST /api/resumes/parse` — Extract text from an uploaded PDF/DOCX without storing it
- `GET /api/resumes` — List the current user's resumes
- `GET /api/resumes/{id}` — Get a resume by ID
- `GET /api/resumes/{id}/download` — Download a stored resume
- `DELETE /api/resumes/{id}` — Delete a resume

### Applications (authenticated)
- `POST /api/applications` — Create an application
- `GET /api/applications/{id}` — Get an application by ID
- `GET /api/applications/me/paged` — List the current user's applications (paged)
- `GET /api/applications/me/search` — Search/filter the current user's applications
- `PUT /api/applications/{id}` — Replace an application
- `PATCH /api/applications/{id}` — Partially update an application
- `DELETE /api/applications/{id}` — Delete an application

### Users (authenticated)
- `GET /api/me`, `GET /api/users/me`, `PUT/PATCH /api/users/me`, `PUT /api/users/me/password`, plus admin-scoped user management under `/api/users`

## Architecture

```
jobmatch/
├── src/main/java/com/adriangarciao/jobmatch/
│   ├── config/           # Security, CORS, AI bean wiring, uploads
│   ├── controller/       # REST controllers
│   ├── dto/              # Data transfer objects (records)
│   ├── exception/        # Custom exceptions + global handler
│   ├── JWTUtility/       # JWT auth filter
│   ├── mapper/           # MapStruct mappers
│   ├── model/            # JPA entities
│   ├── repository/       # Spring Data repositories
│   ├── security/         # Authentication principal / authorization
│   ├── service/          # Business logic
│   │   └── ai/           # Parsing + analysis services
│   │       └── llm/      # LLMService abstraction + deterministic impl
│   └── specifications/   # JPA Specifications for application search
├── src/main/resources/
│   └── db/migration/     # Flyway SQL migrations
├── src/test/             # Unit and integration tests
└── frontend/             # React frontend application
```

## Skill Matching Engine

The scoring engine uses a deterministic weighted algorithm to compare a resume against a job posting:

- **70% weight**: Core-skill overlap (extracted from required + nice-to-have skills)
- **30% weight**: General text-token overlap (capped at 60)
- **Smart parsing**: Extracts skills, location, and compensation from job postings
- **Context-aware**: Distinguishes technical terminology from common words

### Algorithm

1. Parses skills from the resume and the job posting
2. Reduces both to a set of core technical skills (Java, Python, React, etc.)
3. Computes the matched / missing core skills and a weighted match score
4. Generates strengths, weaknesses, and suggestions
5. Extracts job metadata (location, compensation)

## License

Released under the [MIT License](LICENSE).

## Contact

Adrian Garcia - [@adriangarciao](https://github.com/adriangarciao)
