TRUNCATE TABLE
    community_comments,
    community_post_likes,
    community_post_scraps,
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
