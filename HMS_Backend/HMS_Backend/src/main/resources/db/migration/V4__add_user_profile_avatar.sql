ALTER TABLE users
    ADD COLUMN avatar_image BYTEA,
    ADD COLUMN avatar_content_type VARCHAR(50);
