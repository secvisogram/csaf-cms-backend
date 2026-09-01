-- V1__initial_schema.sql
-- Initial PostgreSQL schema for CSAF CMS Backend
-- Replaces single-database CouchDB document model with normalized relational tables

-- Advisories (was ObjectType.Advisory)
CREATE TABLE advisories (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_state     VARCHAR(20)  NOT NULL DEFAULT 'Draft',
    owner              VARCHAR(255) NOT NULL,
    csaf               JSONB        NOT NULL,
    versioning_type    VARCHAR(10)  NOT NULL DEFAULT 'Semantic',
    last_major_version VARCHAR(50),
    tmp_tracking_id    VARCHAR(255),
    advisory_reference UUID,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version            BIGINT       NOT NULL DEFAULT 0
);

-- Advisory version snapshots (was ObjectType.AdvisoryVersion)
CREATE TABLE advisory_versions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    advisory_id        UUID         NOT NULL REFERENCES advisories(id) ON DELETE CASCADE,
    workflow_state     VARCHAR(20)  NOT NULL,
    owner              VARCHAR(255) NOT NULL,
    csaf               JSONB        NOT NULL,
    versioning_type    VARCHAR(10)  NOT NULL,
    last_major_version VARCHAR(50),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Audit trail for document changes (was ObjectType.AuditTrailDocument)
CREATE TABLE audit_trail_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    advisory_id     UUID         NOT NULL REFERENCES advisories(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    "user"          VARCHAR(255) NOT NULL,
    change_type     VARCHAR(20)  NOT NULL,
    diff            JSONB,
    old_doc_version VARCHAR(50),
    doc_version     VARCHAR(50)
);

-- Audit trail for workflow transitions (was ObjectType.AuditTrailWorkflow)
CREATE TABLE audit_trail_workflows (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    advisory_id     UUID         NOT NULL REFERENCES advisories(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    "user"          VARCHAR(255) NOT NULL,
    change_type     VARCHAR(20)  NOT NULL,
    old_state       VARCHAR(20),
    new_state       VARCHAR(20),
    old_doc_version VARCHAR(50),
    doc_version     VARCHAR(50)
);

-- Comments and answers (was ObjectType.Comment)
CREATE TABLE comments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    advisory_id  UUID         NOT NULL REFERENCES advisories(id) ON DELETE CASCADE,
    owner        VARCHAR(255) NOT NULL,
    comment_text TEXT         NOT NULL,
    csaf_node_id VARCHAR(255),
    field_name   VARCHAR(255),
    answer_to    UUID         REFERENCES comments(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version      BIGINT       NOT NULL DEFAULT 0
);

-- Audit trail for comment changes (was ObjectType.CommentAuditTrail)
CREATE TABLE audit_trail_comments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comment_id   UUID         NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    "user"       VARCHAR(255) NOT NULL,
    change_type  VARCHAR(20)  NOT NULL,
    comment_text TEXT
);

-- Sequential counters (was ObjectType.Counter)
CREATE TABLE counters (
    id    VARCHAR(100) PRIMARY KEY,
    count BIGINT NOT NULL DEFAULT 0
);

-- Indexes for common query patterns
CREATE INDEX idx_advisories_workflow_state ON advisories(workflow_state);
CREATE INDEX idx_advisories_owner ON advisories(owner);
CREATE UNIQUE INDEX idx_advisories_tracking_id
    ON advisories ((csaf -> 'document' -> 'tracking' ->> 'id'));
CREATE INDEX idx_advisories_csaf ON advisories USING GIN (csaf jsonb_path_ops);
CREATE INDEX idx_advisory_versions_advisory_id ON advisory_versions(advisory_id);
CREATE INDEX idx_audit_trail_documents_advisory_id ON audit_trail_documents(advisory_id);
CREATE INDEX idx_audit_trail_workflows_advisory_id ON audit_trail_workflows(advisory_id);
CREATE INDEX idx_comments_advisory_id ON comments(advisory_id);
CREATE INDEX idx_comments_answer_to ON comments(answer_to);
CREATE INDEX idx_audit_trail_comments_comment_id ON audit_trail_comments(comment_id);
