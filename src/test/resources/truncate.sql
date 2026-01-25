set session_replication_role = 'replica';

truncate table game_areas;
truncate table participants;
truncate table user_devices;
truncate table games;
truncate table users;

set session_replication_role = 'origin';
