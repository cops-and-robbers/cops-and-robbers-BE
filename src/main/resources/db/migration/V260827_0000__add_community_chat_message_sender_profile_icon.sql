ALTER TABLE community_chat_messages
    ADD COLUMN sender_profile_icon INTEGER;

UPDATE community_chat_messages m
SET sender_profile_icon = u.profile_icon
FROM users u
WHERE m.sender_id = u.id;

UPDATE community_chat_messages
SET sender_profile_icon = 1
WHERE sender_profile_icon IS NULL;

ALTER TABLE community_chat_messages
    ALTER COLUMN sender_profile_icon SET NOT NULL,
    ALTER COLUMN sender_profile_icon SET DEFAULT 1;
