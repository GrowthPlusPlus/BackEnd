-- 초기 테스트 데이터 삽입
-- 작성자: 정성우

-- 사용자 데이터 삽입
INSERT INTO users (email, password, role, nickname, status, created_at) VALUES
('user1@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pL5O', 'ROLE_USER', '테스트유저1', 'ACTIVE', CURRENT_TIMESTAMP),
('user2@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pL5O', 'ROLE_USER', '테스트유저2', 'ACTIVE', CURRENT_TIMESTAMP),
('admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pL5O', 'ROLE_ADMIN', '관리자', 'ACTIVE', CURRENT_TIMESTAMP);

-- 챌린지 데이터 삽입
INSERT INTO challenge (user_id, title, content, max_participant_number, participant_number, challenge_visibility, start_date, end_date, required_weekly_count, created_at) VALUES 
(1, '매일 운동하기', '하루 30분씩 운동하는 챌린지입니다. 함께 건강한 습관을 만들어봐요!', 10, 1, 'PUBLIC', CURRENT_DATE, CURRENT_DATE + INTERVAL '30' DAY, 3, CURRENT_TIMESTAMP),
(2, '독서 습관 만들기', '하루에 책 한 권씩 읽는 챌린지입니다. 지식을 쌓아봅시다!', 5, 1, 'PUBLIC', CURRENT_DATE, CURRENT_DATE + INTERVAL '21' DAY, 5, CURRENT_TIMESTAMP),
(1, '친구만 보기 챌린지', '친구들만 볼 수 있는 비공개 챌린지입니다.', 3, 1, 'FRIENDS_ONLY', CURRENT_DATE, CURRENT_DATE + INTERVAL '14' DAY, 2, CURRENT_TIMESTAMP);

-- 게시글 데이터 삽입
INSERT INTO posts (title, content, user_id, challenge_id, post_date, like_number, comment_number, created_at, updated_at) VALUES 
('첫 번째 인증글', '오늘 운동 완료했습니다! 힘들었지만 뿌듯해요.', 1, 1, CURRENT_DATE, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('독서 인증', '오늘 "스프링 부트" 책을 읽었습니다. 정말 유익했어요!', 2, 2, CURRENT_DATE, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('운동 인증 2일차', '오늘도 30분 운동 완료! 조금씩 나아지고 있어요.', 1, 1, CURRENT_DATE - INTERVAL '1' DAY, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
