ALTER TABLE chat_messages
    ADD COLUMN reply_to_id UUID REFERENCES chat_messages(id) ON DELETE SET NULL,
    ADD COLUMN edited_at TIMESTAMPTZ,
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chat_messages_payload_check;
ALTER TABLE chat_messages
    ADD CONSTRAINT chat_messages_payload_check CHECK (
        deleted_at IS NOT NULL
        OR (message_type = 'TEXT' AND content IS NOT NULL AND LENGTH(BTRIM(content)) > 0)
        OR message_type = 'SONG'
    );

CREATE TABLE chat_message_reactions (
    message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reaction VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (message_id, user_id, reaction),
    CHECK (LENGTH(reaction) BETWEEN 1 AND 16)
);

CREATE INDEX chat_message_reactions_message_idx
    ON chat_message_reactions(message_id, created_at);

CREATE INDEX chat_messages_search_idx
    ON chat_messages USING GIN (to_tsvector('simple', COALESCE(content, '')));
