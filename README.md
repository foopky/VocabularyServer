# Vocabulary App Backend

Backend for a vocabulary service that lets you save words you encounter in Netflix subtitles and review them later.
A Chrome extension (which captures words over the subtitle track) and a web client both consume this API.

- Production: `https://voca-app-backend.foopky.com`
- API docs: `/swagger-ui/index.html`

Japanese and English are supported. For Japanese, the service takes the inflected form and reading seen in the subtitle and **fills in the word's meaning automatically** (`POST /create_word_meaning`).

## Tech Stack

| Area | Technology |
| --- | --- |
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5.6 (Web, WebFlux, Data JPA, Security, AOP) |
| Database | PostgreSQL (HikariCP, `ddl-auto=update`) |
| Authentication | JWT access token + DB-backed refresh token (jjwt 0.11.5) |
| External API | OpenAI Chat Completions (WebClient) |
| Documentation | springdoc-openapi 2.7.0 |
| Build / deploy | Gradle, Docker (multi-stage), GitHub Actions → Docker Hub → EC2 |

## Architecture

```
Chrome Extension / Web  ──HTTPS──▶  Spring Boot (EC2, Docker)  ──▶  PostgreSQL
                                            │
                                            └──▶  OpenAI API (meanings · examples)
```

```
controller ──▶ service ──▶ strategy ──▶ repository ──▶ PostgreSQL
     │            │
     │            └── OpenAIService (WebClient)
     └── OwnershipGuard (ownership checks) · UserRateLimiter (rate limiting)
```

- **Strategy pattern** — `WordStrategy` is implemented per language (`japanese` / `english`), and Spring injects every implementation as a `Map<String, WordStrategy>` from which the requested language picks one. Adding a language touches neither the controllers nor the services. Required fields on save also diverge here: Japanese needs `ondoku`/`kundoku`, English needs `pronunciation`.
- **Ownership checks** — Being logged in used to be enough to reach someone else's data by changing the `userId`/`folderId` in the URL. `OwnershipGuard` consolidates those checks in one place and throws `AccessDeniedException` (→ 403) on violation.
- **Exception handling** — `GlobalExceptionHandler` converts every exception into a JSON response. Left unhandled, exceptions bubble up to the container's `/error` dispatch, where a server error turns into a 401 — and the frontend's axios interceptor reads that as an expired session and logs the user out.

## Features

### 1. Authentication — access token + refresh token rotation

- Access tokens (JWT, 1 hour by default) and refresh tokens (opaque random strings, 14 days by default) are separate. A refresh token must be revocable if stolen, so it is a 256-bit random value stored in the database rather than a JWT.
- **Rotation**: a refresh token used for reissue is deleted immediately and replaced with a new one. A revoked token no longer resolves, which blocks replay after theft. Clients must overwrite their stored refresh token with the one returned in the response.
- On authentication failure, `JwtAuthenticationEntryPoint` returns **401 with a reason** (`TOKEN_EXPIRED`, etc.) instead of 403, so the frontend can decide between reissuing and prompting a fresh login.
- Expired refresh tokens are swept daily at 4 AM by a scheduler (set `jwt.refresh-token-cleanup-cron` to `-` to disable).

### 2. Automatic word meanings (`POST /create_word_meaning`)

Fills the meaning field in the extension's "add word" modal. Three things drive the design.

**The reading disambiguates homographs.** Identical spellings with different readings are different words: 生物 read セイブツ means "living things", read ナマモノ it means "raw food"; 一日 is イチニチ ("one day") or ツイタチ ("the 1st of the month"). Merely listing the reading lets the model drift toward the more common sense, so the prompt states the rule explicitly — and the reading is part of the cache key.

**Dictionary form and inflected form are passed separately.** Subtitles show the inflected form (掛け), and looking that up directly fails. Lookups and caching key off the dictionary form (掛ける); the inflected form is passed for reference only. When an inflected form is present the prompt says so, otherwise the model objects that "掛ける is read カケル, so カケ is a contradiction" and answers UNKNOWN.

**A cross-user cache in the database.** Learners save heavily overlapping words, and since requests carry no sentence context the same input always yields the same answer. Entries are stored under a unique key of `(base_form, reading, pos, target_language)`. The cache lives in the database rather than in memory so it survives deploys. Failed lookups are not cached — better to ask again later than to make one failure permanent.

Distinguishing status codes matters. The extension has to react differently to "no meaning found" and "the server is down", and disguising an outage as a 200 makes it look to the user like the word simply has no meaning.

| Status | Meaning | Client behavior |
| --- | --- | --- |
| `200` + `{"meaning": "..."}` | Meaning found | Fill the field |
| `200` + `{"meaning": null}` | Could not determine | Leave the field empty |
| `400` | Required field missing | — |
| `401` | Token expired/invalid | Refresh, then retry once |
| `429` | Rate limit exceeded | Retry after `Retry-After` |
| `503` | OpenAI outage/timeout | Treat as a server problem |

Request/response example:

```jsonc
// POST /create_word_meaning
{
  "base_form": "掛ける",   // required — lookup and cache key
  "word": "掛け",          // inflected form seen in the subtitle
  "reading": "カケ",       // required — disambiguates homographs
  "pos": "動詞",
  "pos_detail": "自立",
  "target_language": "ko"  // required
}

// 200
{ "meaning": "걸다, 매달다" }
```

Model output is not used verbatim. Only the first line is kept, quotes/bullets/trailing periods are stripped, and the line is truncated at a comma boundary so it fits the modal's three-line textarea (`wordmeaning.max-length`).

### 3. Protecting the LLM path

On `/create_word_meaning`, one click is one OpenAI charge. `UserRateLimiter` applies a per-user fixed window (20 requests per 60 seconds by default) to stop repeated "+" clicks, returning 429 with a `Retry-After` header when exceeded. Counts are incremented atomically via `ConcurrentHashMap.compute`, so concurrent requests don't leak through. The goal is abuse prevention, so the state lives in instance memory with no external store.

### 4. Vocabulary folders and sharing

- Folders and words are joined through `WordInFolder`, a composite-key join entity. Removing a word from a folder also deletes the word itself if it no longer belongs to any folder.
- Words within a folder are paginated. Without an explicit sort the ordering shifts between pages, duplicating and dropping words, so `id` is the default sort. Responses use a fixed `PageResponseDTO` rather than serializing Spring's `Page` directly.
- Folders posted to the shared board (`SharedFolder`) are readable by others. Folder reads therefore use a readability check (`checkFolderReadable`) instead of an ownership check (`checkFolderOwner`) — owners and shared folders both pass.

## API

Every request requires authentication except `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, `POST /api/users`, `GET /healthCheck`, and the Swagger paths.

### Authentication
| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/auth/login` | Log in → `{ jwt, userId, refreshToken }` |
| POST | `/api/auth/refresh` | Reissue access token (rotates the refresh token) |
| POST | `/api/auth/logout` | Revoke only the supplied refresh token (other devices stay logged in) |

### Users
| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/users` | Sign up (`role` is always forced to `USER` server-side) |
| GET | `/api/users/{userId}` | Fetch (own account only) |
| PUT | `/api/users/{userId}` | Update profile |
| PUT | `/api/users/{userId}/password` | Change password (a wrong current password returns 400, not 401) |
| DELETE | `/api/users/{userId}` | Delete account |

### Words
| Method | Path | Description |
| --- | --- | --- |
| GET | `/words/{language}` | List all for a language |
| GET | `/words/{language}/learned?learned=` | Filter by learned status |
| POST | `/words` | Create |
| PUT | `/words` | Update |
| DELETE | `/words` | Remove from a folder (body carries `wordId`, `folderId`) |

### Folders
| Method | Path | Description |
| --- | --- | --- |
| GET | `/folder/getfolderLanguage/{language}` | Folders by language |
| GET | `/folder/getfolderUser/{userId}` | Folders by user |
| GET | `/folder/getwords/{folderId}?page=&size=&sort=` | Words in a folder (paginated) |
| POST | `/folder/wordfolder?user_id=` | Create a folder |
| POST | `/folder/addwordtofolder/{language}/{wordId}/{folderId}` | Add a word to a folder |
| POST | `/folder/SharedFolderToMyWordFolder?userId=` | Copy a shared folder into your own |
| DELETE | `/folder/{folderId}` | Delete a folder |

### Shared folders
| Method | Path | Description |
| --- | --- | --- |
| GET | `/sharedFolder` | List all |
| GET | `/sharedFolder/{userId}` | Shared folders by user |
| GET | `/sharedFolder/{userId}/{folderId}` | Detail |
| POST | `/sharedFolder` | Share (own folders only) |
| DELETE | `/sharedFolder/{userId}/{folderId}` | Unshare |

### Examples and meanings
| Method | Path | Description |
| --- | --- | --- |
| GET | `/create_sentences?word=&language=&style=` | Generate 3 example sentences |
| POST | `/create_word_meaning` | Automatic word meaning |
| GET | `/sentence/user/{userId}` | Sentences by user |
| GET | `/sentence/by-user-and-word?userId=&wordId=` | Sentences for a word |
| POST | `/sentence` | Save a sentence |
| DELETE | `/sentence/{sentenceId}` | Delete a sentence |
| GET | `/healthCheck` | Health check (for the load balancer) |

## Running

### Requirements
Java 21, PostgreSQL (or Docker)

### Environment variables

`.env` is not committed. The required values are:

```bash
DB_HOST=...
DB_USER=...
DB_PASSWORD=...
JWT_SECRET=...          # HS256 signing key
OPENAI_API_KEY=...
```

Optional values (defaults in parentheses):

| Variable | Description |
| --- | --- |
| `DB_PORT` (5432), `DB_NAME` (postgres) | Database connection |
| `JWT_ACCESS_TOKEN_VALIDITY_MS` (1 hour) | Access token lifetime |
| `JWT_REFRESH_TOKEN_VALIDITY_MS` (14 days) | Refresh token lifetime |
| `JWT_REFRESH_TOKEN_CLEANUP_CRON` (`0 0 4 * * *`) | Expired-token cleanup schedule |
| `OPENAI_MEANING_REASONING_EFFORT` (`low`) | Reasoning effort for meaning lookups |
| `OPENAI_MEANING_MAX_COMPLETION_TOKENS` (3000) | Token budget for meaning lookups |
| `WORD_MEANING_RATE_LIMIT_CAPACITY` (20) | Rate limit count |
| `WORD_MEANING_RATE_LIMIT_WINDOW_SECONDS` (60) | Rate limit window |
| `WORD_MEANING_MAX_LENGTH` (60) | Maximum meaning length |

> **The two `OPENAI_MEANING_*` values were tuned against real calls and should not be changed casually.**
> The gpt-5 family are reasoning models, so reasoning tokens draw down the same `max_completion_tokens` budget. At the default (medium), inputs where the dictionary form and reading appear to disagree — 掛ける with カケ — burn the entire budget on reasoning and come back with an empty body (observed even at 5000 tokens). Dropping to `minimal` removes the budget problem but loses homograph discrimination (生物/ナマモノ comes back as "living things"). `low` + 3000 satisfies both: across 18 calls, zero empty responses and correct reading-based disambiguation throughout. When switching to a non-reasoning model, leave `reasoning-effort` blank and the parameter is omitted entirely.

### Local

```bash
./gradlew bootRun
# http://localhost:8080/swagger-ui/index.html
```

### Tests

```bash
./gradlew test
```

### Docker

```bash
docker build -t vocabulary-backend .
docker run -p 8080:8080 --env-file .env vocabulary-backend
```

The build is multi-stage. Dependency resolution is pinned to its own layer so source-only changes rebuild less, and the runtime is a JRE alpine image running as a non-root user. Only `bootJar` runs, so no `plain.jar` is produced.

## CI/CD

`.github/workflows/deploy.yml` runs on push to `main` or manual dispatch (`workflow_dispatch`).

1. **build-and-push** — Builds the image tagged with the first 7 characters of the commit SHA and pushes to Docker Hub (`:<sha>`, `:latest`), using the GitHub Actions cache.
2. **deploy** — Connects to EC2 over SSH, pulls that tag, and replaces the container. Environment variables come from `/home/ec2-user/.env` on the instance, logs rotate at 10MB × 3 files, and stale images are pruned.

Deploys reference the **commit SHA tag**, not `latest`: `latest` hides which commit is running and cannot be rolled back. The tag is passed through job outputs — a string containing secrets gets masked by GitHub, which would drop the image reference, so the tag is emitted on its own.

Required secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`.

## Project Structure

```
src/main/java/vocabulary/app/
├── config/       SecurityConfig (CORS, authorization, JWT filter), OpenApiConfig, AppConfig
├── controller/   Auth, User, Word, WordFolder, SharedFolder, Sentence, Web (LLM), Etc
├── dto/          Request/response DTOs (paths that don't expose entities directly)
├── entity/       User, Word, WordFolder, WordInFolder, SharedFolder, Sentence,
│                 RefreshToken, WordMeaningCache, Role
├── exception/    GlobalExceptionHandler + domain exceptions
├── jwt/          JwtTokenProvider, JwtAuthenticationFilter, EntryPoint, PrincipalDetails
├── repository/   Spring Data JPA repositories
├── scheduler/    RefreshTokenCleanupScheduler
├── security/     OwnershipGuard, UserRateLimiter
├── service/      User, Word, WordFolder, SharedFolder, RefreshToken,
│                 WordMeaning, OpenAI
├── strategy/     WordStrategy + Japanese / English implementations
└── aspect/       ErrorLoggingAspect
```

## Known Limitations

- The schema is managed by `spring.jpa.hibernate.ddl-auto=update`, which does not apply column drops or type changes. A migration tool is needed.
- Rate limiting is held in instance memory, so scaling to multiple servers multiplies the per-user allowance by the instance count (the limit still holds, just loosely).
- `SharedFolder` uses `FetchType.EAGER` to work around a serialization issue, which causes N+1 queries on list endpoints.
