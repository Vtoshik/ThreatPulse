CREATE EXTENSION IF NOT EXISTS vector;

CREATE TYPE category_enum AS ENUM ('RCE', 'XSS', 'SQLI', 'DATA_BREACH', 'OTHER');

CREATE TABLE IF NOT EXISTS threats (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(255) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    ai_summary TEXT,
    severity severity_level_enum,
    category category_enum,
    source_url VARCHAR(2048) NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL,
    collected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    analyzed_at TIMESTAMP WITH TIME ZONE,
    embedding vector(384),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_external_id UNIQUE (external_id)
    );

CREATE TABLE IF NOT EXISTS threat_technologies (
    id BIGSERIAL PRIMARY KEY,
    threat_id BIGINT NOT NULL REFERENCES threats(id) ON DELETE CASCADE,
    technology_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);