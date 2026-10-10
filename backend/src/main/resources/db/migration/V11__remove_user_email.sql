-- Users sign in with a nickname and a password. The application no longer
-- stores any personal data, so the email address is removed.
-- Dropping the column also drops its unique constraint.
ALTER TABLE users DROP COLUMN email;
