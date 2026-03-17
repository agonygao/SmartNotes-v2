# SmartNotes v2.0

Full-stack productivity system with notes, vocabulary learning, and document management.

## Modules

- **Notes** — standard notes, reminders, checklists, secret/private records (AES-GCM encrypted)
- **Vocabulary Learning** — CET4/CET6 word books, review, dictation, wrong-word tracking
- **Document Center** — supports md, txt, pdf, doc, docx, xls, xlsx

## Tech Stack

- **Backend**: Spring Boot 3.2.5 + Java 17 + Maven
- **Database**: MySQL 8.4 (utf8mb4)
- **Auth**: JWT + refresh token
- **Sync**: Cursor-based incremental sync with conflict detection

## Quick Start

### Database Setup

```sql
CREATE DATABASE smartnotes DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SOURCE database/schema.sql;
```

### Backend

```bash
cd backend-api
mvn spring-boot:run
```

### Configuration

Edit `backend-api/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smartnotes
    username: root
    password: your_password
```

## API Endpoints

| Module | Endpoints |
|--------|-----------|
| Auth | POST /api/auth/register, POST /api/auth/login, POST /api/auth/refresh, GET /api/auth/me |
| Notes | CRUD /api/notes with NORMAL/CHECKLIST/REMINDER/SECRET types |
| Word Books | CRUD /api/wordbooks + default CET4/CET6 auto-injection |
| Words | CRUD /api/words with search |
| Review | POST /api/review/today, POST /api/review/result, POST /api/dictation |
| Documents | POST /api/documents/upload, GET /api/documents, GET /api/documents/{id}/download |
| Sync | POST /api/sync/push, GET /api/sync/pull, GET /api/sync/status, GET /api/sync/conflicts |
| Console | /console/index.html (user), /console/admin.html (admin) |

## Data Modes

- **Local Mode**: fully offline, data stored locally only
- **Cloud Sync Mode**: local-first with incremental cursor-based sync

## License

MIT
