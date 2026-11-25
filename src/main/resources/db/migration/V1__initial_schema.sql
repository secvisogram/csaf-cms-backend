-- V1__initial_schema.sql
-- Initial PostgreSQL schema for CSAF CMS Backend
-- Replaces single-database CouchDB document model with normalized relational tables
--
-- The CSAF document is stored twice: csaf_text is the document as the backend wrote it (keeps key order) and is the
-- only column the application reads and writes; csaf is the same document as jsonb, set from csaf_text by a trigger,
-- for searching. The columns used for listing and searching are generated from csaf. Absent optional values give
-- NULL or an empty array; invalid JSON in csaf_text rejects the write. Both document columns use lz4 compression,
-- which (de)compresses several times faster than the default pglz; the v1 JSON path decompresses csaf of each row.

-- A CSAF date-time as timestamp, or NULL if it is absent or not an RFC 3339 date-time with offset. IMMUTABLE because
-- only values with an explicit offset are accepted, so the result does not depend on the TimeZone setting.
CREATE FUNCTION csaf_timestamp(value text) RETURNS timestamptz
    LANGUAGE plpgsql
    IMMUTABLE
    SET timezone TO 'UTC'
AS
$$
BEGIN
    -- only an RFC 3339 date-time with offset: the timestamp input also accepts words like 'yesterday' or 'now'
    IF value ~ '^\d{4}-\d{2}-\d{2}[Tt]\d{2}:\d{2}:\d{2}(\.\d+)?([Zz]|[+-]\d{2}:\d{2})$' THEN
        BEGIN
            RETURN value::timestamptz;
        EXCEPTION
            WHEN others THEN
                RETURN NULL; -- e.g. month 13
        END;
    END IF;
    RETURN NULL;
END;
$$;

-- The JSON strings the path matches, as an array; other JSON types are skipped
CREATE FUNCTION csaf_strings(j jsonb, path jsonpath) RETURNS text[]
    LANGUAGE sql
    IMMUTABLE
AS
$$
SELECT ARRAY(SELECT v #>> '{}' FROM jsonb_path_query(j, path) v WHERE jsonb_typeof(v) = 'string');
$$;

-- The highest baseScore of the given CVSS version ('cvss_v2', 'cvss_v3' or 'cvss_v4') over all vulnerabilities, or
-- NULL if there is none. CSAF 2.0 has the scores in vulnerabilities[].scores[], CSAF 2.1 in
-- vulnerabilities[].metrics[].content. Only JSON numbers count, so a malformed score does not reject the write.
CREATE FUNCTION csaf_max_score(j jsonb, cvss_version text) RETURNS numeric
    LANGUAGE sql
    IMMUTABLE
AS
$$
SELECT max((s -> cvss_version ->> 'baseScore')::numeric)
FROM (SELECT jsonb_path_query(j, '$.vulnerabilities[*].scores[*]') AS s
      UNION ALL
      SELECT jsonb_path_query(j, '$.vulnerabilities[*].metrics[*].content')) AS scores
WHERE jsonb_typeof(s -> cvss_version -> 'baseScore') = 'number';
$$;

-- Sets csaf from csaf_text; the cast rejects invalid JSON. csaf cannot be a generated column itself: the columns
-- generated from it could then not read it. Generated columns are computed after BEFORE triggers, so they see the
-- new csaf.
CREATE FUNCTION csaf_from_text() RETURNS trigger
    LANGUAGE plpgsql
AS
$$
BEGIN
    NEW.csaf := NEW.csaf_text::jsonb;
    RETURN NEW;
END;
$$;

-- Advisories (was ObjectType.Advisory)
CREATE TABLE advisories
(
    id                      UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    workflow_state          VARCHAR(20)  NOT NULL DEFAULT 'Draft',
    owner                   VARCHAR(255) NOT NULL,
    csaf_text               TEXT COMPRESSION lz4 NOT NULL,
    csaf                    JSONB COMPRESSION lz4 NOT NULL,
    tracking_id             VARCHAR(255) GENERATED ALWAYS AS (csaf #>> '{document,tracking,id}') STORED,
    document_title          TEXT GENERATED ALWAYS AS (csaf #>> '{document,title}') STORED,
    current_release_date    TIMESTAMPTZ GENERATED ALWAYS AS
        (csaf_timestamp(csaf #>> '{document,tracking,current_release_date}')) STORED,
    tracking_version        VARCHAR(255) GENERATED ALWAYS AS (csaf #>> '{document,tracking,version}') STORED,
    tracking_status         VARCHAR(20) GENERATED ALWAYS AS (csaf #>> '{document,tracking,status}') STORED,
    category                TEXT GENERATED ALWAYS AS (csaf #>> '{document,category}') STORED,
    tlp                     VARCHAR(20) GENERATED ALWAYS AS (csaf #>> '{document,distribution,tlp,label}') STORED,
    cves                    TEXT[] NOT NULL GENERATED ALWAYS AS
        (csaf_strings(csaf, '$.vulnerabilities[*].cve')) STORED,
    vul_ids                 TEXT[] NOT NULL GENERATED ALWAYS AS
        (csaf_strings(csaf, '$.vulnerabilities[*].ids[*].text')) STORED,
    aliases                 TEXT[] NOT NULL GENERATED ALWAYS AS
        (csaf_strings(csaf, '$.document.tracking.aliases[*]')) STORED,
    initial_release_date    TIMESTAMPTZ GENERATED ALWAYS AS
        (csaf_timestamp(csaf #>> '{document,tracking,initial_release_date}')) STORED,
    publisher_name          TEXT GENERATED ALWAYS AS (csaf #>> '{document,publisher,name}') STORED,
    revision_history_length INTEGER GENERATED ALWAYS AS
        (CASE WHEN jsonb_typeof(csaf #> '{document,tracking,revision_history}') = 'array'
                  THEN jsonb_array_length(csaf #> '{document,tracking,revision_history}') END) STORED,
    cvss_v2_score           NUMERIC GENERATED ALWAYS AS (csaf_max_score(csaf, 'cvss_v2')) STORED,
    cvss_v3_score           NUMERIC GENERATED ALWAYS AS (csaf_max_score(csaf, 'cvss_v3')) STORED,
    cvss_v4_score           NUMERIC GENERATED ALWAYS AS (csaf_max_score(csaf, 'cvss_v4')) STORED,
    -- the score of the newest CVSS version present
    critical                NUMERIC GENERATED ALWAYS AS (COALESCE(csaf_max_score(csaf, 'cvss_v4'),
        csaf_max_score(csaf, 'cvss_v3'), csaf_max_score(csaf, 'cvss_v2'))) STORED,
    versioning_type         VARCHAR(10)  NOT NULL DEFAULT 'Semantic',
    last_major_version      VARCHAR(50),
    tmp_tracking_id         VARCHAR(255),
    advisory_reference      UUID,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                 BIGINT       NOT NULL DEFAULT 0
);

-- Advisory version snapshots (was ObjectType.AdvisoryVersion)
CREATE TABLE advisory_versions
(
    id                   UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    advisory_id          UUID         NOT NULL REFERENCES advisories (id) ON DELETE CASCADE,
    workflow_state       VARCHAR(20)  NOT NULL,
    owner                VARCHAR(255) NOT NULL,
    csaf_text            TEXT COMPRESSION lz4 NOT NULL,
    csaf                 JSONB COMPRESSION lz4 NOT NULL,
    -- only the columns the list v1 shows for auditors; the paged list does not list versions
    tracking_id          VARCHAR(255) GENERATED ALWAYS AS (csaf #>> '{document,tracking,id}') STORED,
    document_title       TEXT GENERATED ALWAYS AS (csaf #>> '{document,title}') STORED,
    current_release_date TIMESTAMPTZ GENERATED ALWAYS AS
        (csaf_timestamp(csaf #>> '{document,tracking,current_release_date}')) STORED,
    versioning_type      VARCHAR(10)  NOT NULL,
    last_major_version   VARCHAR(50),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- UPDATE OF csaf_text: updates that do not assign csaf_text (e.g. a workflow state change) skip the parse
CREATE TRIGGER advisories_csaf_from_text
    BEFORE INSERT OR UPDATE OF csaf_text
    ON advisories
    FOR EACH ROW
EXECUTE FUNCTION csaf_from_text();

CREATE TRIGGER advisory_versions_csaf_from_text
    BEFORE INSERT OR UPDATE OF csaf_text
    ON advisory_versions
    FOR EACH ROW
EXECUTE FUNCTION csaf_from_text();

COMMENT ON COLUMN advisories.csaf IS 'Set from csaf_text by a trigger; write csaf_text only';
COMMENT ON COLUMN advisory_versions.csaf IS 'Set from csaf_text by a trigger; write csaf_text only';

-- Audit trail for document changes (was ObjectType.AuditTrailDocument)
CREATE TABLE audit_trail_documents
(
    id              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    advisory_id     UUID         NOT NULL REFERENCES advisories (id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    "user"          VARCHAR(255) NOT NULL,
    change_type     VARCHAR(20)  NOT NULL,
    diff            JSONB,
    old_doc_version VARCHAR(50),
    doc_version     VARCHAR(50)
);

-- Audit trail for workflow transitions (was ObjectType.AuditTrailWorkflow)
CREATE TABLE audit_trail_workflows
(
    id              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    advisory_id     UUID         NOT NULL REFERENCES advisories (id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    "user"          VARCHAR(255) NOT NULL,
    change_type     VARCHAR(20)  NOT NULL,
    old_state       VARCHAR(20),
    new_state       VARCHAR(20),
    old_doc_version VARCHAR(50),
    doc_version     VARCHAR(50)
);

-- Comments and answers (was ObjectType.Comment)
CREATE TABLE comments
(
    id           UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    advisory_id  UUID         NOT NULL REFERENCES advisories (id) ON DELETE CASCADE,
    owner        VARCHAR(255) NOT NULL,
    comment_text TEXT         NOT NULL,
    csaf_node_id VARCHAR(255),
    field_name   VARCHAR(255),
    answer_to    UUID REFERENCES comments (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version      BIGINT       NOT NULL DEFAULT 0
);

-- Audit trail for comment changes (was ObjectType.CommentAuditTrail)
CREATE TABLE audit_trail_comments
(
    id           UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    comment_id   UUID         NOT NULL REFERENCES comments (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    "user"       VARCHAR(255) NOT NULL,
    change_type  VARCHAR(20)  NOT NULL,
    comment_text TEXT
);

-- Sequential counters (was ObjectType.Counter)
CREATE TABLE counters
(
    id    VARCHAR(100) PRIMARY KEY,
    count BIGINT NOT NULL DEFAULT 0
);

-- Indexes for common query patterns
CREATE INDEX idx_advisories_workflow_state ON advisories (workflow_state);
CREATE INDEX idx_advisories_owner ON advisories (owner);
CREATE UNIQUE INDEX idx_advisories_tracking_id ON advisories (tracking_id);
CREATE INDEX idx_advisories_workflow_state_release_date
    ON advisories (workflow_state, current_release_date);
CREATE INDEX advisories_cves_idx ON advisories USING gin (cves);
CREATE INDEX advisories_vul_ids_idx ON advisories USING gin (vul_ids);
CREATE INDEX advisories_aliases_idx ON advisories USING gin (aliases);
CREATE INDEX idx_advisory_versions_advisory_id ON advisory_versions (advisory_id);
CREATE INDEX idx_audit_trail_documents_advisory_id ON audit_trail_documents (advisory_id);
CREATE INDEX idx_audit_trail_workflows_advisory_id ON audit_trail_workflows (advisory_id);
CREATE INDEX idx_comments_advisory_id ON comments (advisory_id);
CREATE INDEX idx_comments_answer_to ON comments (answer_to);
CREATE INDEX idx_audit_trail_comments_comment_id ON audit_trail_comments (comment_id);
