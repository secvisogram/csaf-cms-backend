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
    ADD COLUMN tracking_version        VARCHAR(255),
    ADD COLUMN tracking_status         VARCHAR(20),
    ADD COLUMN category                TEXT,
    ADD COLUMN tlp                     VARCHAR(20),
    ADD COLUMN cves                    TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN vul_ids                 TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN aliases                 TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN initial_release_date    TIMESTAMPTZ,
    ADD COLUMN publisher_name          TEXT,
    ADD COLUMN revision_history_length INTEGER,
    ADD COLUMN cvss_v2_score           NUMERIC,
    ADD COLUMN cvss_v3_score           NUMERIC,
    ADD COLUMN cvss_v4_score           NUMERIC,
    ADD COLUMN critical                NUMERIC;

-- Absent optional values give NULL or an empty array; invalid JSON in csaf raises an error and rejects the write.

-- A CSAF date-time as timestamp, or NULL if it is absent or not an RFC 3339 date-time with offset
CREATE FUNCTION csaf_timestamp(value text) RETURNS timestamptz
    LANGUAGE plpgsql
    STABLE
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

CREATE FUNCTION csaf_extract_list_columns() RETURNS trigger
    LANGUAGE plpgsql
AS
$$
DECLARE
    j jsonb := NEW.csaf::jsonb;
BEGIN
    NEW.tracking_id := j #>> '{document,tracking,id}';
    NEW.document_title := j #>> '{document,title}';
    NEW.current_release_date := csaf_timestamp(j #>> '{document,tracking,current_release_date}');
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
    NEW.initial_release_date := csaf_timestamp(j #>> '{document,tracking,initial_release_date}');
    NEW.publisher_name := j #>> '{document,publisher,name}';
    NEW.revision_history_length := CASE
                                       WHEN jsonb_typeof(j #> '{document,tracking,revision_history}') = 'array'
                                           THEN jsonb_array_length(j #> '{document,tracking,revision_history}')
        END;
    NEW.cvss_v2_score := csaf_max_score(j, 'cvss_v2');
    NEW.cvss_v3_score := csaf_max_score(j, 'cvss_v3');
    NEW.cvss_v4_score := csaf_max_score(j, 'cvss_v4');
    -- the score of the newest CVSS version present
    NEW.critical := COALESCE(NEW.cvss_v4_score, NEW.cvss_v3_score, NEW.cvss_v2_score);
    RETURN NEW;
END;
$$;

-- UPDATE OF csaf: updates that do not assign csaf (e.g. a workflow state change) skip the parse
CREATE TRIGGER advisories_csaf_extract
    BEFORE INSERT OR UPDATE OF csaf
    ON advisories
    FOR EACH ROW
EXECUTE FUNCTION csaf_extract_list_columns();

-- advisory_versions has only the columns of V1; the cast to jsonb still rejects a write with invalid JSON
CREATE FUNCTION csaf_extract_version_columns() RETURNS trigger
    LANGUAGE plpgsql
AS
$$
DECLARE
    j jsonb := NEW.csaf::jsonb;
BEGIN
    NEW.tracking_id := j #>> '{document,tracking,id}';
    NEW.document_title := j #>> '{document,title}';
    NEW.current_release_date := csaf_timestamp(j #>> '{document,tracking,current_release_date}');
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
