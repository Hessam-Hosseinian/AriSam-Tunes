CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_url TEXT,
    bio VARCHAR(500),
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX users_email_lower_unique ON users (LOWER(email));

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ,
    replaced_by_token_id UUID REFERENCES refresh_tokens(id) ON DELETE SET NULL
);
CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens(user_id);
CREATE INDEX refresh_tokens_expiry_idx ON refresh_tokens(expires_at);

CREATE TABLE artists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    image_url TEXT,
    biography TEXT,
    extra_metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX artists_name_lower_unique ON artists (LOWER(name));

CREATE TABLE songs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    artist_id UUID REFERENCES artists(id) ON DELETE SET NULL,
    title VARCHAR(500) NOT NULL,
    artist_name VARCHAR(255) NOT NULL,
    album VARCHAR(500),
    album_artist VARCHAR(255),
    track_number INTEGER,
    disc_number INTEGER,
    genre VARCHAR(255),
    duration_seconds INTEGER NOT NULL DEFAULT 0 CHECK (duration_seconds >= 0),
    bitrate_kbps INTEGER,
    sample_rate_hz INTEGER,
    channels VARCHAR(100),
    codec VARCHAR(255),
    file_format VARCHAR(30),
    release_year INTEGER,
    release_date DATE,
    language VARCHAR(100),
    lyrics TEXT,
    composer VARCHAR(500),
    producer VARCHAR(500),
    copyright TEXT,
    publisher VARCHAR(500),
    mood VARCHAR(255),
    tags TEXT[] NOT NULL DEFAULT '{}',
    is_explicit BOOLEAN NOT NULL DEFAULT FALSE,
    popularity INTEGER NOT NULL DEFAULT 0,
    play_count BIGINT NOT NULL DEFAULT 0,
    is_local BOOLEAN NOT NULL DEFAULT FALSE,
    is_demo BOOLEAN NOT NULL DEFAULT FALSE,
    source_file_name TEXT,
    source_relative_path TEXT,
    audio_file_size BIGINT,
    cover_file_name TEXT,
    cover_relative_path TEXT,
    audio_url TEXT NOT NULL,
    cover_image_url TEXT NOT NULL,
    artist_image_url TEXT,
    album_cover_url TEXT,
    extra_metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX songs_artist_id_idx ON songs(artist_id);
CREATE INDEX songs_title_lower_idx ON songs(LOWER(title));
CREATE INDEX songs_artist_name_lower_idx ON songs(LOWER(artist_name));
CREATE INDEX songs_created_at_idx ON songs(created_at DESC);
CREATE INDEX songs_popularity_idx ON songs(popularity DESC, play_count DESC);

CREATE TYPE playlist_scope AS ENUM ('GLOBAL', 'LOCAL', 'USER');

CREATE TABLE playlists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    cover_image_url TEXT,
    scope playlist_scope NOT NULL DEFAULT 'USER',
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX playlists_owner_id_idx ON playlists(owner_id);
CREATE INDEX playlists_scope_public_idx ON playlists(scope, is_public);

CREATE TABLE playlist_songs (
    playlist_id UUID NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
    song_id UUID NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position >= 0),
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (playlist_id, song_id),
    UNIQUE (playlist_id, position)
);

CREATE TABLE user_follows (
    follower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    followed_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, followed_id),
    CHECK (follower_id <> followed_id)
);
CREATE INDEX user_follows_followed_id_idx ON user_follows(followed_id);

CREATE TYPE chat_message_type AS ENUM ('TEXT', 'SONG');
CREATE TYPE chat_message_status AS ENUM ('SENT', 'DELIVERED', 'READ');

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_message_id UUID NOT NULL UNIQUE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_type chat_message_type NOT NULL DEFAULT 'TEXT',
    content TEXT,
    song_id UUID REFERENCES songs(id) ON DELETE SET NULL,
    status chat_message_status NOT NULL DEFAULT 'SENT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    CHECK (sender_id <> recipient_id),
    CHECK (
        (message_type = 'TEXT' AND content IS NOT NULL) OR
        (message_type = 'SONG' AND song_id IS NOT NULL)
    )
);
CREATE INDEX chat_messages_conversation_idx
    ON chat_messages(LEAST(sender_id, recipient_id), GREATEST(sender_id, recipient_id), created_at DESC);
CREATE INDEX chat_messages_recipient_status_idx ON chat_messages(recipient_id, status);
