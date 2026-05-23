CREATE TABLE user_bookmarks (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    threat_id   BIGINT NOT NULL REFERENCES threats(id) ON DELETE CASCADE,
    bookmarked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, threat_id)
);