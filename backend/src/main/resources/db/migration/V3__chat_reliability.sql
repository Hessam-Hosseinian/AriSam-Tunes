ALTER TABLE chat_messages
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE chat_messages
    DROP CONSTRAINT chat_messages_client_message_id_key;

ALTER TABLE chat_messages
    ADD CONSTRAINT chat_messages_sender_client_message_unique
        UNIQUE (sender_id, client_message_id);

-- The original SONG check conflicted with song_id's ON DELETE SET NULL action.
-- Keep TEXT validation in the database while allowing a deleted shared track to
-- remain as an unavailable historical message.
DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    SELECT conname
    INTO constraint_name
    FROM pg_constraint
    WHERE conrelid = 'chat_messages'::regclass
      AND contype = 'c'
      AND pg_get_constraintdef(oid) LIKE '%message_type%';

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE chat_messages DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE chat_messages
    ADD CONSTRAINT chat_messages_payload_check CHECK (
        (message_type = 'TEXT' AND content IS NOT NULL AND LENGTH(BTRIM(content)) > 0)
        OR message_type = 'SONG'
    );

DROP INDEX IF EXISTS chat_messages_conversation_idx;

CREATE INDEX chat_messages_conversation_cursor_idx
    ON chat_messages (
        LEAST(sender_id, recipient_id),
        GREATEST(sender_id, recipient_id),
        created_at DESC,
        id DESC
    );

CREATE INDEX chat_messages_sender_updated_cursor_idx
    ON chat_messages (sender_id, updated_at ASC, id ASC);

CREATE INDEX chat_messages_recipient_updated_cursor_idx
    ON chat_messages (recipient_id, updated_at ASC, id ASC);
