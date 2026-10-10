-- A token carries the version of the user it was issued for. Changing the password raises
-- the version, so every token issued before the change stops working.
ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
