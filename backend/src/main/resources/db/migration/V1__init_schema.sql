-- QuickChat 초기 스키마. domain-entities.md 참고.
-- 로그인 잠금 카운터, Refresh Token은 Redis에 저장하므로 여기에 테이블이 없다 (nfr-requirements.md).

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(50)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE channels (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(10)  NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    visibility  VARCHAR(20)  NOT NULL CHECK (visibility IN ('PUBLIC', 'INVITE_ONLY')),
    owner_id    UUID         NOT NULL REFERENCES users(id),
    status      VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE channel_members (
    channel_id  UUID        NOT NULL REFERENCES channels(id),
    user_id     UUID        NOT NULL REFERENCES users(id),
    role        VARCHAR(10) NOT NULL CHECK (role IN ('OWNER', 'MEMBER')),
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (channel_id, user_id)
);

CREATE TABLE messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id  UUID         NOT NULL REFERENCES channels(id),
    sender_id   UUID         NOT NULL REFERENCES users(id),
    content     VARCHAR(4000) NOT NULL,
    sent_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_channel_sentat ON messages (channel_id, sent_at DESC);
CREATE INDEX idx_channel_members_user ON channel_members (user_id);
