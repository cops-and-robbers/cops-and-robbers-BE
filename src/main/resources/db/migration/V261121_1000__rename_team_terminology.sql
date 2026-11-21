ALTER TABLE games
    RENAME COLUMN police_wait_minutes TO chaser_wait_minutes;

ALTER TABLE game_results
    RENAME COLUMN total_police_count TO total_chaser_count;
ALTER TABLE game_results
    RENAME COLUMN total_robber_count TO total_runner_count;
ALTER TABLE game_results
    RENAME COLUMN arrested_robber_count TO arrested_runner_count;

UPDATE participants
SET team = CASE team WHEN 'POLICE' THEN 'CHASER' WHEN 'ROBBER' THEN 'RUNNER' END
WHERE team IN ('POLICE', 'ROBBER');
UPDATE participants
SET status = 'CHASER_WAITING'
WHERE status = 'POLICE_WAITING';

UPDATE game_results
SET winner_team = CASE winner_team WHEN 'POLICE' THEN 'CHASER' WHEN 'ROBBER' THEN 'RUNNER' END
WHERE winner_team IN ('POLICE', 'ROBBER');
UPDATE game_results
SET end_reason = CASE end_reason WHEN 'POLICE_FORFEITED' THEN 'CHASER_FORFEITED' WHEN 'ROBBER_FORFEITED' THEN 'RUNNER_FORFEITED' END
WHERE end_reason IN ('POLICE_FORFEITED', 'ROBBER_FORFEITED');

UPDATE game_result_participants
SET team = CASE team WHEN 'POLICE' THEN 'CHASER' WHEN 'ROBBER' THEN 'RUNNER' END
WHERE team IN ('POLICE', 'ROBBER');
UPDATE game_result_participants
SET status = 'CHASER_WAITING'
WHERE status = 'POLICE_WAITING';
