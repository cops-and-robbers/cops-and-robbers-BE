ALTER TABLE users
    ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER';

UPDATE users
SET role = 'ADMIN'
WHERE nickname IN ('비정상', '도둑', '창희', '혜림무', '운영자', '경찰과도둑');