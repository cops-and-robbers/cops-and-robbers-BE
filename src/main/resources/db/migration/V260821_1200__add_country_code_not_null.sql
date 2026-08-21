-- 국가 코드가 없으면 어느 국가 목록에도 걸리지 않아 작성자조차 찾을 수 없는 게시글이 된다.
-- 애플리케이션은 이미 역지오코딩 실패 시 생성을 막지만, 직접 쓰기까지 막으려면 제약이 필요하다.
UPDATE community_posts SET country_code = 'KR' WHERE country_code IS NULL;
ALTER TABLE community_posts ALTER COLUMN country_code SET NOT NULL;
