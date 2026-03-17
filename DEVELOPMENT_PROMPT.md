# SmartNotes v2.0 — Development Instructions for Agent Teams

You are the Team Leader of a multi-agent software engineering team responsible for developing SmartNotes v2.0.

## 1. Project Overview

SmartNotes is a full-stack productivity system with three major modules:
1. **Notes** — standard notes, reminders, checklists, secret/private records
2. **Vocabulary Learning** — CET4/CET6 word books, review, dictation, wrong words tracking
3. **Document Center** — supports md, txt, pdf, doc, docx, xls, xlsx

## 2. Tech Stack

- **Backend**: Spring Boot 3 + Java 17 + Maven
- **Database**: MySQL 8.4 (localhost:3306, root/20030912ww, database: smartnotes)
- **Android**: Kotlin + Compose + Hilt + Retrofit
- **Build**: Maven for backend, Gradle for Android

## 3. Project Structure

```
SmartNotes-v2/
├── backend-api/          # Spring Boot REST API
├── android-app/          # Android client
├── database/             # Migration scripts, schema docs
├── docs/                 # API docs, architecture docs
├── DEVELOPMENT_PROMPT.md
└── README.md
```

## 4. Data Modes

- **Local Mode**: data stored locally only, fully offline-capable
- **Cloud Sync Mode**: local-first, incremental sync with cloud, push/pull + cursor + conflict log

## 5. Existing v2.0 Capabilities (preserve and improve)

- Account system: guest mode, registration/login, JWT + refresh token
- Multi-user isolation by user_id for all entities
- Sync APIs: POST /api/sync/push, GET /api/sync/pull, GET /api/sync/status, GET /api/sync/conflicts
- Secret records: end-to-end encrypted (AES-GCM on Android), cloud stores ciphertext only
- Note reminders: repeat rules, ringtone/vibration, notification actions (snooze/complete)
- Document pipeline: import fixes, previewable state, unified fallback error handling
- Default CET4/CET6 word books auto-injected for new users
- Console pages: /console/index.html (user), /console/admin.html (admin)

## 6. Required Team Roles (Agents)

Create and coordinate these agents:
- **A. Team Leader / Architecture Coordinator** — understand whole system, break work into parallel tracks, manage dependencies
- **B. Android Client Agent** — Kotlin + Compose + Hilt + Retrofit, local/sync mode UX, all UI flows
- **C. Backend API Agent** — Spring Boot 3 REST API, auth, CRUD, sync endpoints, console support
- **D. Database & Sync Strategy Agent** — schema, indexes, migrations, sync tokens/cursors, conflict log
- **E. Document Capability Agent** — document pipeline, import/preview, compatibility matrix
- **F. QA / Testing Agent** — test strategy, backend tests, integration tests
- **G. Product & UX Optimization Agent** — usability, empty/error states, onboarding

## 7. Development Priorities

**Priority A — Stable v2.0 baseline:**
1. Register/login/refresh token/current user
2. Multi-user isolated notes CRUD
3. Word books/words/review/dictation workflow
4. Default CET4/CET6 word books for new users
5. Document import + basic preview pipeline
6. Local mode and cloud sync mode switching
7. Sync APIs working baseline
8. Secret records encrypted end-to-end
9. Reminders with snooze/complete
10. Console pages accessible

**Priority B — Improve weak areas:**
- Unified API response structure and error codes
- Database indexes and constraints
- Sync conflict transparency
- Android offline/sync/loading feedback
- Authentication safety

**Priority C — Sustainable v2.x foundation:**
- Clear module boundaries, maintainable data model, extensible sync design

## 8. Execution Phases

**Phase 1 — Discovery and contract freezing:**
- Inspect project structure
- Standardize DTOs, API response format, error codes
- Define sync protocol and conflict model
- Freeze backend-Android contracts

**Phase 2 — Core implementation (parallel where safe):**
- Auth and user system
- Notes CRUD
- Word books/words/review/dictation
- Documents baseline pipeline
- Default word-book initialization
- Console support
- Local/sync framework

**Phase 3 — Enhancement and hardening:**
- Secret record UX
- Reminder behavior
- Sync conflict handling
- Document compatibility
- UI/UX details
- Exception handling

**Phase 4 — Testing and acceptance:**
- Backend tests
- Integration verification
- Defect report

## 9. File Ownership Rules

1. Assign file ownership before editing
2. Only one agent edits a critical file per phase
3. Shared DTO/API/DB changes must be announced first
4. Freeze interfaces before parallel implementation
5. Database migrations reviewed with backend model changes

## 10. Engineering Standards

- Clear code structure, consistent naming, moderate comments
- Runnable code, practical tests, solid error handling
- No security holes, no unnecessary complexity
- High-value improvements only

## 11. Instructions

Start NOW by doing:
1. Analyze the SmartNotes v2.0 scope
2. Create the multi-agent task breakdown table
3. Define the first round of parallel development
4. State which tasks are parallel vs sequential
5. Define file ownership rules
6. Begin Phase 1 design and implementation

Work like a real engineering team. Produce runnable, testable, extensible code.
Do NOT simply restate requirements. Implement them.
