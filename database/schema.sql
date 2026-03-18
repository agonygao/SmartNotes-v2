-- ============================================================================
-- SmartNotes v2.0 - Database Schema
-- MySQL 8.4
-- ============================================================================
-- This file contains the full DDL for the SmartNotes v2.0 application database.
-- Target engine: InnoDB with utf8mb4 character set.
-- Tables: users, notes, word_books, words, word_progress,
--         dictation_records, wrong_words, documents, sync_logs,
--         sync_cursors, conflict_logs
-- ============================================================================

USE smartnotes;

-- ----------------------------------------------------------------------------
-- 1. users
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    email           VARCHAR(100)    DEFAULT NULL,
    role            ENUM('USER','ADMIN')          NOT NULL DEFAULT 'USER',
    status          ENUM('ACTIVE','DISABLED')     NOT NULL DEFAULT 'ACTIVE',
    token_version   INT             NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE INDEX uq_users_username (username),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. notes
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notes (
    id                  BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    title               VARCHAR(255)    NOT NULL DEFAULT '',
    content             TEXT,
    type                ENUM('NORMAL','CHECKLIST','REMINDER','SECRET') NOT NULL DEFAULT 'NORMAL',
    checklist_items     JSON            DEFAULT NULL,
    reminder_time       DATETIME        DEFAULT NULL,
    reminder_repeat_rule VARCHAR(100)   DEFAULT NULL,
    reminder_ringtone   VARCHAR(255)    DEFAULT NULL,
    is_completed        BOOLEAN         NOT NULL DEFAULT FALSE,
    is_pinned           BOOLEAN         NOT NULL DEFAULT FALSE,
    is_encrypted        BOOLEAN         NOT NULL DEFAULT FALSE,
    client_id           VARCHAR(64)     DEFAULT NULL,
    version             INT             NOT NULL DEFAULT 1,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_notes_user (user_id),
    INDEX idx_notes_user_deleted (user_id, deleted),
    INDEX idx_notes_user_type (user_id, type),
    INDEX idx_notes_user_reminder (user_id, reminder_time),
    INDEX idx_notes_client_id (user_id, client_id),
    UNIQUE INDEX uq_notes_client (user_id, client_id),

    CONSTRAINT fk_notes_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. word_books
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS word_books (
    id              BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    user_id         BIGINT          DEFAULT NULL,
    name            VARCHAR(100)    NOT NULL,
    description     TEXT,
    type            ENUM('CET4','CET6','CUSTOM')   NOT NULL DEFAULT 'CUSTOM',
    word_count      INT             NOT NULL DEFAULT 0,
    is_default      BOOLEAN         NOT NULL DEFAULT FALSE,
    client_id       VARCHAR(64)     DEFAULT NULL,
    version         INT             NOT NULL DEFAULT 1,
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_wordbooks_user (user_id),
    INDEX idx_wordbooks_type (type),

    CONSTRAINT fk_wordbooks_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. words
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS words (
    id              BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    book_id         BIGINT          NOT NULL,
    word            VARCHAR(100)    NOT NULL,
    phonetic        VARCHAR(255)    DEFAULT NULL,
    meaning         TEXT,
    example_sentence TEXT,
    sort_order      INT             NOT NULL DEFAULT 0,
    client_id       VARCHAR(64)     DEFAULT NULL,
    version         INT             NOT NULL DEFAULT 1,
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_words_book (book_id),
    INDEX idx_words_word (word),
    INDEX idx_words_book_sort (book_id, sort_order),

    CONSTRAINT fk_words_book
        FOREIGN KEY (book_id) REFERENCES word_books (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. word_progress
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS word_progress (
    id              BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    word_id         BIGINT          NOT NULL,
    mastery_level   TINYINT         NOT NULL DEFAULT 0,
    review_count    INT             NOT NULL DEFAULT 0,
    correct_count   INT             NOT NULL DEFAULT 0,
    wrong_count     INT             NOT NULL DEFAULT 0,
    last_reviewed_at DATETIME       DEFAULT NULL,
    next_review_at  DATETIME        DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE INDEX uq_progress_user_word (user_id, word_id),
    INDEX idx_progress_next_review (user_id, next_review_at),

    CONSTRAINT fk_progress_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_progress_word
        FOREIGN KEY (word_id) REFERENCES words (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 6. dictation_records
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dictation_records (
    id              BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    word_id         BIGINT          NOT NULL,
    is_correct      BOOLEAN         NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_dictation_user (user_id),
    INDEX idx_dictation_word (word_id),

    CONSTRAINT fk_dictation_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_dictation_word
        FOREIGN KEY (word_id) REFERENCES words (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 7. wrong_words
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wrong_words (
    id              BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    word_id         BIGINT          NOT NULL,
    wrong_count     INT             NOT NULL DEFAULT 1,
    last_wrong_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mastered        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE INDEX uq_wrong_user_word (user_id, word_id),
    INDEX idx_wrong_mastered (user_id, mastered),

    CONSTRAINT fk_wrongwords_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_wrongwords_word
        FOREIGN KEY (word_id) REFERENCES words (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 8. documents
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS documents (
    id                  BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    filename            VARCHAR(255)    NOT NULL,
    original_filename   VARCHAR(255)    NOT NULL,
    file_type           VARCHAR(20)     NOT NULL,
    file_size           BIGINT          NOT NULL DEFAULT 0,
    file_path           VARCHAR(500)    NOT NULL,
    mime_type           VARCHAR(100)    DEFAULT NULL,
    preview_available   BOOLEAN         NOT NULL DEFAULT FALSE,
    client_id           VARCHAR(64)     DEFAULT NULL,
    version             INT             NOT NULL DEFAULT 1,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_documents_user (user_id),
    INDEX idx_documents_type (user_id, file_type),
    INDEX idx_documents_client (user_id, client_id),

    CONSTRAINT fk_documents_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 9. sync_logs
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sync_logs (
    id              BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       BIGINT          NOT NULL,
    action          ENUM('CREATE','UPDATE','DELETE') NOT NULL,
    sync_cursor     BIGINT          NOT NULL,
    client_id       VARCHAR(64)     DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_synclogs_user_cursor (user_id, sync_cursor),
    INDEX idx_synclogs_user_type (user_id, entity_type),

    CONSTRAINT fk_synclogs_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 10. sync_cursors
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sync_cursors (
    id              BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    `cursor`        BIGINT          NOT NULL DEFAULT 0,
    last_synced_at  DATETIME        DEFAULT NULL,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         BIGINT          NOT NULL DEFAULT 0,

    UNIQUE INDEX uq_sync_cursors_user (user_id),

    CONSTRAINT fk_sync_cursors_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 11. conflict_logs
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS conflict_logs (
    id              BIGINT          AUTO_INCREMENT  PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       BIGINT          NOT NULL,
    client_id       VARCHAR(64)     DEFAULT NULL,
    local_version   INT             NOT NULL,
    server_version  INT             NOT NULL,
    local_data      JSON            DEFAULT NULL,
    server_data     JSON            DEFAULT NULL,
    resolved        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_conflicts_user (user_id),
    INDEX idx_conflicts_resolved (user_id, resolved),

    CONSTRAINT fk_conflicts_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
