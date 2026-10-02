-- Threats saved while the AI analysis failed hold made-up values (severity INFO, category OTHER,
-- no summary). Mark them as waiting for a new analysis instead of treating them as final.
ALTER TABLE threats
    ADD COLUMN analysis_status VARCHAR(20) NOT NULL DEFAULT 'ANALYZED',
    ADD COLUMN analysis_attempted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE threats
    ADD CONSTRAINT chk_threats_analysis_status
    CHECK (analysis_status IN ('ANALYZED', 'PENDING_ANALYSIS'));

-- The made-up severity and category are removed: unknown is NULL, not INFO or OTHER
UPDATE threats
SET analysis_status = 'PENDING_ANALYSIS',
    severity = NULL,
    category = NULL,
    analyzed_at = NULL
WHERE ai_summary IS NULL OR btrim(ai_summary) = '';

-- Lets the re-analysis job find the threats that were tried least recently without scanning the table
CREATE INDEX idx_threats_pending_analysis
    ON threats (analysis_attempted_at NULLS FIRST, collected_at)
    WHERE analysis_status = 'PENDING_ANALYSIS';
