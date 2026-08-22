TRUNCATE TABLE
    community_chat_messages,
    community_chat_members,
    community_posts,
    notices,
    reports,
    bug_reports,
    game_areas,
    participants,
    user_devices,
    game_results,
    games,
    users
RESTART IDENTITY CASCADE;
