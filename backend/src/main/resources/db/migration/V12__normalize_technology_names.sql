-- Technology names are matched by exact comparison, so they must have one spelling.
-- This applies the same rules as TechnologyNames.normalize in the Java code:
-- trim, lower case, whitespace and underscores to one hyphen, a few well-known aliases.

CREATE FUNCTION pg_temp.tp_normalize_technology(name TEXT) RETURNS TEXT AS $$
    SELECT CASE cleaned
        WHEN 'node.js'  THEN 'nodejs'
        WHEN 'node'     THEN 'nodejs'
        WHEN 'postgres' THEN 'postgresql'
        WHEN 'k8s'      THEN 'kubernetes'
        WHEN 'vue.js'   THEN 'vue'
        WHEN 'vuejs'    THEN 'vue'
        WHEN 'react.js' THEN 'react'
        WHEN 'reactjs'  THEN 'react'
        WHEN 'js'       THEN 'javascript'
        ELSE cleaned
    END
    FROM (
        SELECT regexp_replace(
                   regexp_replace(
                       regexp_replace(lower(btrim(name)), '[[:space:]_]+', '-', 'g'),
                       '-{2,}', '-', 'g'),
                   '^-|-$', '', 'g') AS cleaned
    ) AS t
$$ LANGUAGE SQL IMMUTABLE;

-- Rows that become equal after normalizing are merged first, so the update
-- cannot break the unique constraint on (user_id, technology_name).
DELETE FROM user_technologies a
USING user_technologies b
WHERE a.user_id = b.user_id
  AND pg_temp.tp_normalize_technology(a.technology_name) = pg_temp.tp_normalize_technology(b.technology_name)
  AND a.ctid > b.ctid;

UPDATE user_technologies
SET technology_name = pg_temp.tp_normalize_technology(technology_name);

DELETE FROM threat_technologies a
USING threat_technologies b
WHERE a.threat_id = b.threat_id
  AND pg_temp.tp_normalize_technology(a.technology_name) = pg_temp.tp_normalize_technology(b.technology_name)
  AND a.ctid > b.ctid;

UPDATE threat_technologies
SET technology_name = pg_temp.tp_normalize_technology(technology_name);

UPDATE alert_rules
SET technologies_filter = ARRAY(
    SELECT DISTINCT pg_temp.tp_normalize_technology(t)
    FROM unnest(technologies_filter) AS t
    WHERE pg_temp.tp_normalize_technology(t) <> ''
)
WHERE technologies_filter IS NOT NULL
  AND cardinality(technologies_filter) > 0;

DROP FUNCTION pg_temp.tp_normalize_technology(TEXT);
