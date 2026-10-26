ALTER TABLE game_result_participants
    ALTER COLUMN user_id DROP NOT NULL;

-- 이 배포 전에 이미 탈퇴한 기록도 탈퇴 처리와 똑같이 비움
-- 행과 수치는 같은 판 다른 참가자의 결과 화면에 쓰이므로 남김
UPDATE game_result_participants
SET user_id = NULL, nickname = '알수없음'
WHERE user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = game_result_participants.user_id);

UPDATE game_results
SET host_user_id = NULL
WHERE host_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = game_results.host_user_id);
