-- V2__csaf_as_text_with_list_columns.sql
-- Stores the CSAF document as text (keeps key order and formatting as written by the backend) and derives the
-- columns used for searching and listing from it with a trigger. Valid JSON is enforced by the trigger.

ALTER TABLE advisories
    ALTER COLUMN csaf TYPE TEXT USING csaf::text;
ALTER TABLE advisory_versions
    ALTER COLUMN csaf TYPE TEXT USING csaf::text;

-- Derived columns (tracking_id, document_title and current_release_date already exist). advisory_versions keeps
-- only those three: the version snapshots are listed by v1 only and not by the paged list.
ALTER TABLE advisories
    ADD COLUMN tracking_version VARCHAR(255),
    ADD COLUMN tracking_status  VARCHAR(20),
    ADD COLUMN category         TEXT,
    ADD COLUMN tlp              VARCHAR(20),
    ADD COLUMN cves             TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN vul_ids          TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN aliases          TEXT[] NOT NULL DEFAULT '{}';

-- All paths exist unchanged in CSAF 2.0 and 2.1. Absent optional values give NULL or an empty array.
-- An unparsable current_release_date gives NULL; invalid JSON in csaf raises an error and rejects the write.
CREATE FUNCTION csaf_release_date(j jsonb) RETURNS timestamptz
    LANGUAGE plpgsql
    STABLE
    SET timezone TO 'UTC'
AS
$$
DECLARE
    release_date text := j #>> '{document,tracking,current_release_date}';
BEGIN
    -- only an RFC 3339 date-time with offset: the timestamp input also accepts words like 'yesterday' or 'now'
    IF release_date ~ '^\d{4}-\d{2}-\d{2}[Tt]\d{2}:\d{2}:\d{2}(\.\d+)?([Zz]|[+-]\d{2}:\d{2})$' THEN
        BEGIN
            RETURN release_date::timestamptz;
        EXCEPTION
            WHEN others THEN
                RETURN NULL; -- e.g. month 13
        END;
    END IF;
    RETURN NULL;
END;
$$;

CREATE FUNCTION csaf_extract_list_columns() RETURNS trigger
    LANGUAGE plpgsql
AS
$$
DECLARE
    j jsonb := NEW.csaf::jsonb;
BEGIN
    NEW.tracking_id := j #>> '{document,tracking,id}';
    NEW.document_title := j #>> '{document,title}';
    NEW.current_release_date := csaf_release_date(j);
    NEW.tracking_version := j #>> '{document,tracking,version}';
    NEW.tracking_status := j #>> '{document,tracking,status}';
    NEW.category := j #>> '{document,category}';
    NEW.tlp := j #>> '{document,distribution,tlp,label}';
    NEW.cves := ARRAY(SELECT v #>> '{}'
                      FROM jsonb_path_query(j, '$.vulnerabilities[*].cve') v
                      WHERE jsonb_typeof(v) = 'string');
    NEW.vul_ids := ARRAY(SELECT v #>> '{}'
                         FROM jsonb_path_query(j, '$.vulnerabilities[*].ids[*].text') v
                         WHERE jsonb_typeof(v) = 'string');
    NEW.aliases := ARRAY(SELECT v #>> '{}'
                         FROM jsonb_path_query(j, '$.document.tracking.aliases[*]') v
                         WHERE jsonb_typeof(v) = 'string');
    RETURN NEW;
END;
$$;

-- UPDATE OF csaf: updates that do not assign csaf (e.g. a workflow state change) skip the parse
CREATE TRIGGER advisories_csaf_extract
    BEFORE INSERT OR UPDATE OF csaf
    ON advisories
    FOR EACH ROW
EXECUTE FUNCTION csaf_extract_list_columns();

-- advisory_versions has only the columns of V1; invalid JSON is rejected here as well
CREATE FUNCTION csaf_extract_version_columns() RETURNS trigger
    LANGUAGE plpgsql
AS
$$
DECLARE
    j jsonb := NEW.csaf::jsonb;
BEGIN
    NEW.tracking_id := j #>> '{document,tracking,id}';
    NEW.document_title := j #>> '{document,title}';
    NEW.current_release_date := csaf_release_date(j);
    RETURN NEW;
END;
$$;

CREATE TRIGGER advisory_versions_csaf_extract
    BEFORE INSERT OR UPDATE OF csaf
    ON advisory_versions
    FOR EACH ROW
EXECUTE FUNCTION csaf_extract_version_columns();

-- Fill the new columns for existing rows (fires the triggers)
UPDATE advisories SET csaf = csaf;
UPDATE advisory_versions SET csaf = csaf;

CREATE INDEX advisories_cves_idx ON advisories USING gin (cves);
CREATE INDEX advisories_vul_ids_idx ON advisories USING gin (vul_ids);
CREATE INDEX advisories_aliases_idx ON advisories USING gin (aliases);
