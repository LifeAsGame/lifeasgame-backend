-- 2026-10-04 activation; historical source remained GATED. Existing content is untouched.

ALTER TABLE collection_logs MODIFY category ENUM ('BOOK','CARD','COIN','FIGURE','GAME','OTHER','STAMP','PROJECT') NOT NULL;

ALTER TABLE player_quest_routes ADD COLUMN role_id BIGINT NULL;

ALTER TABLE player_quest_routes ADD CONSTRAINT fk_player_quest_route_owned_role FOREIGN KEY (role_id, player_id) REFERENCES roles (id, player_id);

CREATE TABLE quest_journey_evidence (
    acceptance_id BIGINT NOT NULL PRIMARY KEY,
    kind VARCHAR(20) NOT NULL,
    life_log_id BIGINT NULL,
    memo VARCHAR(1000) NULL,
    url VARCHAR(2048) NULL,
    description VARCHAR(1000) NULL,
    linked_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_journey_evidence_acceptance FOREIGN KEY (acceptance_id) REFERENCES quest_acceptances(id) ON DELETE RESTRICT,
    CONSTRAINT ck_journey_evidence_kind CHECK (kind IN ('GOAL_MEMO','LIFE_LOG','PROJECT','DEPLOYMENT'))
);

INSERT INTO quests (reward_exp, definition_version, target_value, created_at, due_at, updated_at, code, title_id, reward_stats, reward_profile_code, category, semantic_category, description_md, repeat_rule, completion_policy, role_template_code, target_type, progress_source) VALUES (0, 1, 1, CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6), 'Q_DEV_DEFINE_BACKEND_GOAL', '백엔드 개발 목표 정하기', JSON_OBJECT(), 'RP_NONE', NULL, 'ROLE', '백엔드 개발 목표 정하기', 'ONCE', 'USER_CONFIRM', 'ROLE_BACKEND_DEVELOPER', 'COUNT', 'MANUAL_CHECK') ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO quests (reward_exp, definition_version, target_value, created_at, due_at, updated_at, code, title_id, reward_stats, reward_profile_code, category, semantic_category, description_md, repeat_rule, completion_policy, role_template_code, target_type, progress_source) VALUES (0, 1, 1, CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6), 'Q_DEV_RECORD_JAVA_STUDY', 'Java 학습 기록 남기기', JSON_OBJECT(), 'RP_EXP_TINY_10', NULL, 'ROLE', 'Java 학습 기록 남기기', 'ONCE', 'USER_CONFIRM', 'ROLE_BACKEND_DEVELOPER', 'COUNT', 'RECORD_CREATED') ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO quests (reward_exp, definition_version, target_value, created_at, due_at, updated_at, code, title_id, reward_stats, reward_profile_code, category, semantic_category, description_md, repeat_rule, completion_policy, role_template_code, target_type, progress_source) VALUES (0, 1, 1, CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6), 'Q_DEV_BUILD_SPRING_CRUD', 'Spring CRUD 기능 완성하기', JSON_OBJECT(), 'RP_EXP_TINY_10', NULL, 'ROLE', 'Spring CRUD 기능 완성하기', 'ONCE', 'USER_CONFIRM', 'ROLE_BACKEND_DEVELOPER', 'COUNT', 'RECORD_CREATED') ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO quests (reward_exp, definition_version, target_value, created_at, due_at, updated_at, code, title_id, reward_stats, reward_profile_code, category, semantic_category, description_md, repeat_rule, completion_policy, role_template_code, target_type, progress_source) VALUES (0, 1, 1, CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6), 'Q_DEV_MODEL_DATABASE', '데이터 모델 작성하기', JSON_OBJECT(), 'RP_EXP_TINY_10', NULL, 'ROLE', '데이터 모델 작성하기', 'ONCE', 'USER_CONFIRM', 'ROLE_BACKEND_DEVELOPER', 'COUNT', 'RECORD_CREATED') ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO quests (reward_exp, definition_version, target_value, created_at, due_at, updated_at, code, title_id, reward_stats, reward_profile_code, category, semantic_category, description_md, repeat_rule, completion_policy, role_template_code, target_type, progress_source) VALUES (0, 1, 1, CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6), 'Q_DEV_WRITE_DOMAIN_TEST', '도메인 테스트 작성하기', JSON_OBJECT(), 'RP_EXP_TINY_10', NULL, 'ROLE', '도메인 테스트 작성하기', 'ONCE', 'USER_CONFIRM', 'ROLE_BACKEND_DEVELOPER', 'COUNT', 'RECORD_CREATED') ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO quests (reward_exp, definition_version, target_value, created_at, due_at, updated_at, code, title_id, reward_stats, reward_profile_code, category, semantic_category, description_md, repeat_rule, completion_policy, role_template_code, target_type, progress_source) VALUES (0, 1, 1, CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6), 'Q_DEV_DEPLOY_SERVICE', '서비스 배포하기', JSON_OBJECT(), 'RP_EXP_AND_ITEM_FIRST_STEP_20', NULL, 'ROLE', '서비스 배포하기', 'ONCE', 'USER_CONFIRM', 'ROLE_BACKEND_DEVELOPER', 'COUNT', 'RECORD_CREATED') ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO quests (reward_exp, definition_version, target_value, created_at, due_at, updated_at, code, title_id, reward_stats, reward_profile_code, category, semantic_category, description_md, repeat_rule, completion_policy, role_template_code, target_type, progress_source) VALUES (0, 1, 1, CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6), 'Q_DEV_POLISH_README', 'README 정리하기', JSON_OBJECT(), 'RP_EXP_TINY_10', NULL, 'ROLE', 'README 정리하기', 'ONCE', 'USER_CONFIRM', 'ROLE_BACKEND_DEVELOPER', 'COUNT', 'RECORD_CREATED') ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO quest_routes (code, definition_version, title, description, primary_role_template_code, created_at, updated_at) VALUES ('ROUTE_BACKEND_DEVELOPER_START', 1, '백엔드 개발자의 길', '개발 목표부터 배포와 설명까지 단계적으로 기록한다', 'ROLE_BACKEND_DEVELOPER', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO quest_route_steps (route_id, step_code, step_order, title, description, criterion_type, required_evidence_count, user_advance_required, retroactive_evidence_allowed, skip_allowed) SELECT id, 'RS_DEV_01_DIRECTION', 1, '방향 정하기', '방향 정하기', 'QUEST_COMPLETION_SET', 1, b'1', b'1', b'0' FROM quest_routes WHERE code = 'ROUTE_BACKEND_DEVELOPER_START' ON DUPLICATE KEY UPDATE step_code = VALUES(step_code);

INSERT INTO quest_route_steps (route_id, step_code, step_order, title, description, criterion_type, required_evidence_count, user_advance_required, retroactive_evidence_allowed, skip_allowed) SELECT id, 'RS_DEV_02_JAVA', 2, 'Java 기반 다지기', 'Java 기반 다지기', 'QUEST_COMPLETION_SET', 1, b'1', b'1', b'0' FROM quest_routes WHERE code = 'ROUTE_BACKEND_DEVELOPER_START' ON DUPLICATE KEY UPDATE step_code = VALUES(step_code);

INSERT INTO quest_route_steps (route_id, step_code, step_order, title, description, criterion_type, required_evidence_count, user_advance_required, retroactive_evidence_allowed, skip_allowed) SELECT id, 'RS_DEV_03_SPRING', 3, 'Spring 서비스 만들기', 'Spring 서비스 만들기', 'QUEST_COMPLETION_SET', 1, b'1', b'1', b'0' FROM quest_routes WHERE code = 'ROUTE_BACKEND_DEVELOPER_START' ON DUPLICATE KEY UPDATE step_code = VALUES(step_code);

INSERT INTO quest_route_steps (route_id, step_code, step_order, title, description, criterion_type, required_evidence_count, user_advance_required, retroactive_evidence_allowed, skip_allowed) SELECT id, 'RS_DEV_04_DATABASE', 4, '데이터와 JPA 다루기', '데이터와 JPA 다루기', 'QUEST_COMPLETION_SET', 1, b'1', b'1', b'0' FROM quest_routes WHERE code = 'ROUTE_BACKEND_DEVELOPER_START' ON DUPLICATE KEY UPDATE step_code = VALUES(step_code);

INSERT INTO quest_route_steps (route_id, step_code, step_order, title, description, criterion_type, required_evidence_count, user_advance_required, retroactive_evidence_allowed, skip_allowed) SELECT id, 'RS_DEV_05_TEST', 5, '테스트 가능한 코드 만들기', '테스트 가능한 코드 만들기', 'QUEST_COMPLETION_SET', 1, b'1', b'1', b'0' FROM quest_routes WHERE code = 'ROUTE_BACKEND_DEVELOPER_START' ON DUPLICATE KEY UPDATE step_code = VALUES(step_code);

INSERT INTO quest_route_steps (route_id, step_code, step_order, title, description, criterion_type, required_evidence_count, user_advance_required, retroactive_evidence_allowed, skip_allowed) SELECT id, 'RS_DEV_06_DEPLOY', 6, '배포 가능한 서비스 만들기', '배포 가능한 서비스 만들기', 'QUEST_COMPLETION_SET', 1, b'1', b'1', b'0' FROM quest_routes WHERE code = 'ROUTE_BACKEND_DEVELOPER_START' ON DUPLICATE KEY UPDATE step_code = VALUES(step_code);

INSERT INTO quest_route_steps (route_id, step_code, step_order, title, description, criterion_type, required_evidence_count, user_advance_required, retroactive_evidence_allowed, skip_allowed) SELECT id, 'RS_DEV_07_PORTFOLIO', 7, '설명 가능한 결과물 만들기', '설명 가능한 결과물 만들기', 'QUEST_COMPLETION_SET', 1, b'1', b'1', b'0' FROM quest_routes WHERE code = 'ROUTE_BACKEND_DEVELOPER_START' ON DUPLICATE KEY UPDATE step_code = VALUES(step_code);

INSERT INTO quest_route_step_quests (step_id, quest_id, requirement_type) SELECT step.id, quest.id, 'REQUIRED' FROM quest_route_steps step JOIN quest_routes route ON route.id=step.route_id JOIN quests quest ON quest.code='Q_DEV_DEFINE_BACKEND_GOAL' WHERE route.code='ROUTE_BACKEND_DEVELOPER_START' AND step.step_code='RS_DEV_01_DIRECTION' ON DUPLICATE KEY UPDATE requirement_type = VALUES(requirement_type);

INSERT INTO quest_route_step_quests (step_id, quest_id, requirement_type) SELECT step.id, quest.id, 'REQUIRED' FROM quest_route_steps step JOIN quest_routes route ON route.id=step.route_id JOIN quests quest ON quest.code='Q_DEV_RECORD_JAVA_STUDY' WHERE route.code='ROUTE_BACKEND_DEVELOPER_START' AND step.step_code='RS_DEV_02_JAVA' ON DUPLICATE KEY UPDATE requirement_type = VALUES(requirement_type);

INSERT INTO quest_route_step_quests (step_id, quest_id, requirement_type) SELECT step.id, quest.id, 'REQUIRED' FROM quest_route_steps step JOIN quest_routes route ON route.id=step.route_id JOIN quests quest ON quest.code='Q_DEV_BUILD_SPRING_CRUD' WHERE route.code='ROUTE_BACKEND_DEVELOPER_START' AND step.step_code='RS_DEV_03_SPRING' ON DUPLICATE KEY UPDATE requirement_type = VALUES(requirement_type);

INSERT INTO quest_route_step_quests (step_id, quest_id, requirement_type) SELECT step.id, quest.id, 'REQUIRED' FROM quest_route_steps step JOIN quest_routes route ON route.id=step.route_id JOIN quests quest ON quest.code='Q_DEV_MODEL_DATABASE' WHERE route.code='ROUTE_BACKEND_DEVELOPER_START' AND step.step_code='RS_DEV_04_DATABASE' ON DUPLICATE KEY UPDATE requirement_type = VALUES(requirement_type);

INSERT INTO quest_route_step_quests (step_id, quest_id, requirement_type) SELECT step.id, quest.id, 'REQUIRED' FROM quest_route_steps step JOIN quest_routes route ON route.id=step.route_id JOIN quests quest ON quest.code='Q_DEV_WRITE_DOMAIN_TEST' WHERE route.code='ROUTE_BACKEND_DEVELOPER_START' AND step.step_code='RS_DEV_05_TEST' ON DUPLICATE KEY UPDATE requirement_type = VALUES(requirement_type);

INSERT INTO quest_route_step_quests (step_id, quest_id, requirement_type) SELECT step.id, quest.id, 'REQUIRED' FROM quest_route_steps step JOIN quest_routes route ON route.id=step.route_id JOIN quests quest ON quest.code='Q_DEV_DEPLOY_SERVICE' WHERE route.code='ROUTE_BACKEND_DEVELOPER_START' AND step.step_code='RS_DEV_06_DEPLOY' ON DUPLICATE KEY UPDATE requirement_type = VALUES(requirement_type);

INSERT INTO quest_route_step_quests (step_id, quest_id, requirement_type) SELECT step.id, quest.id, 'REQUIRED' FROM quest_route_steps step JOIN quest_routes route ON route.id=step.route_id JOIN quests quest ON quest.code='Q_DEV_POLISH_README' WHERE route.code='ROUTE_BACKEND_DEVELOPER_START' AND step.step_code='RS_DEV_07_PORTFOLIO' ON DUPLICATE KEY UPDATE requirement_type = VALUES(requirement_type);
