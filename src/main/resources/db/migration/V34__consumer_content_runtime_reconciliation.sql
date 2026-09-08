-- Reconcile the installed Consumer journey without replacing referenced rows.

UPDATE quests
SET title_id = '첫 흔적 남기기',
    description_md = '오늘의 생각·행동·기억 중 하나를 짧게 남겨보세요.',
    category = NULL,
    target_type = 'COUNT',
    target_value = 1,
    repeat_rule = 'ONCE',
    completion_policy = 'AUTO',
    semantic_category = 'RECORD',
    progress_source = 'RECORD_CREATED',
    definition_version = 1,
    reward_profile_code = 'RP_EXP_TINY_10',
    reward_exp = 0,
    reward_stats = JSON_OBJECT(),
    due_at = NULL,
    role_template_code = NULL,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code = 'Q_RECORD_FIRST_TRACE';

UPDATE quests
SET title_id = '흔적 세 개 이어보기',
    description_md = '서로 다른 순간의 기록을 세 개 남겨 작은 흐름을 만들어보세요.',
    category = NULL,
    target_type = 'COUNT',
    target_value = 3,
    repeat_rule = 'ONCE',
    completion_policy = 'AUTO',
    semantic_category = 'RECORD',
    progress_source = 'RECORD_CREATED',
    definition_version = 1,
    reward_profile_code = 'RP_EXP_AND_ITEM_FIRST_STEP_20',
    reward_exp = 0,
    reward_stats = JSON_OBJECT(),
    due_at = NULL,
    role_template_code = NULL,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code = 'Q_RECORD_THREE_TRACES';

UPDATE quests
SET title_id = '이번 주 흔적 돌아보기',
    description_md = '이번 주 기록 중 하나를 골라 지금의 나에게 남길 한 줄을 적어보세요.',
    category = NULL,
    target_type = 'COUNT',
    target_value = 1,
    repeat_rule = 'WEEKLY',
    completion_policy = 'AUTO',
    semantic_category = 'RECORD',
    progress_source = 'RECORD_CREATED',
    definition_version = 1,
    reward_profile_code = 'RP_NONE',
    reward_exp = 0,
    reward_stats = JSON_OBJECT(),
    due_at = NULL,
    role_template_code = NULL,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code = 'Q_RECORD_WEEKLY_LOOKBACK';

UPDATE quest_routes
SET definition_version = 1,
    title = '기록을 시작하는 길',
    description = '하루의 한 장면을 남기고, 여러 흔적을 이어, 다시 돌아보는 가장 작은 기록 여정.',
    primary_role_template_code = NULL,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code = 'ROUTE_RECORD_START';

UPDATE quest_route_steps step
JOIN quest_routes route ON route.id = step.route_id
SET step.step_order = 1,
    step.title = '한 장면 남기기',
    step.description = '첫 LifeLog를 남겨 여정의 출발점을 만듭니다.',
    step.criterion_type = 'QUEST_COMPLETION_SET',
    step.required_evidence_count = 1,
    step.user_advance_required = b'1',
    step.retroactive_evidence_allowed = b'1',
    step.skip_allowed = b'0'
WHERE route.code = 'ROUTE_RECORD_START'
  AND step.step_code = 'RS_RECORD_01_LEAVE_TRACE';

UPDATE quest_route_steps step
JOIN quest_routes route ON route.id = step.route_id
SET step.step_order = 2,
    step.title = '흔적 이어보기',
    step.description = '서로 다른 기록 세 개를 이어 작은 흐름을 만듭니다.',
    step.criterion_type = 'QUEST_COMPLETION_SET',
    step.required_evidence_count = 1,
    step.user_advance_required = b'1',
    step.retroactive_evidence_allowed = b'1',
    step.skip_allowed = b'0'
WHERE route.code = 'ROUTE_RECORD_START'
  AND step.step_code = 'RS_RECORD_02_CONNECT_TRACES';

UPDATE quest_route_steps step
JOIN quest_routes route ON route.id = step.route_id
SET step.step_order = 3,
    step.title = '돌아보고 다음 장 열기',
    step.description = '쌓인 기록을 돌아보고 한 줄을 남긴 뒤 Route를 마칩니다.',
    step.criterion_type = 'QUEST_COMPLETION_SET',
    step.required_evidence_count = 1,
    step.user_advance_required = b'1',
    step.retroactive_evidence_allowed = b'1',
    step.skip_allowed = b'0'
WHERE route.code = 'ROUTE_RECORD_START'
  AND step.step_code = 'RS_RECORD_03_LOOK_BACK';

UPDATE quest_route_step_quests link
JOIN quest_route_steps step ON step.id = link.step_id
JOIN quest_routes route ON route.id = step.route_id
JOIN quests required_quest ON required_quest.code = CASE step.step_code
    WHEN 'RS_RECORD_01_LEAVE_TRACE' THEN 'Q_RECORD_FIRST_TRACE'
    WHEN 'RS_RECORD_02_CONNECT_TRACES' THEN 'Q_RECORD_THREE_TRACES'
    WHEN 'RS_RECORD_03_LOOK_BACK' THEN 'Q_RECORD_WEEKLY_LOOKBACK'
END
SET link.quest_id = required_quest.id,
    link.requirement_type = 'REQUIRED'
WHERE route.code = 'ROUTE_RECORD_START';

-- Preserve legacy profiles/definitions for historical references while the
-- approved Consumer profiles move to their authority identities and amounts.
UPDATE reward_definitions
SET code = 'EXP_PLAYER',
    name = 'Player EXP',
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code = 'RD_EXP_10';

UPDATE reward_definitions
SET code = 'ITEM_DEFINITION',
    name = 'Item Definition',
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code = 'RD_ITEM_FIRST_STEP_FRAGMENT_1';

UPDATE reward_definitions
SET active = FALSE, updated_at = CURRENT_TIMESTAMP(6)
WHERE code = 'RD_EXP_20';

UPDATE reward_profile_lines line
JOIN reward_profiles profile ON profile.id = line.reward_profile_id
JOIN reward_definitions current_definition
    ON current_definition.id = line.reward_definition_id
   AND current_definition.reward_type = 'ITEM'
JOIN reward_definitions definition
    ON definition.code = 'ITEM_DEFINITION'
SET line.reward_definition_id = definition.id,
    line.sort_order = 2,
    line.amount_override = 1,
    line.updated_at = CURRENT_TIMESTAMP(6)
WHERE profile.code = 'RP_EXP_AND_ITEM_FIRST_STEP_20'
  AND line.sort_order = 1;

UPDATE reward_profile_lines line
JOIN reward_profiles profile ON profile.id = line.reward_profile_id
JOIN reward_definitions definition
    ON definition.code = 'EXP_PLAYER'
SET line.reward_definition_id = definition.id,
    line.sort_order = 1,
    line.amount_override = CASE profile.code
        WHEN 'RP_EXP_TINY_10' THEN 10
        WHEN 'RP_EXP_AND_ITEM_FIRST_STEP_20' THEN 20
    END,
    line.updated_at = CURRENT_TIMESTAMP(6)
WHERE profile.code IN (
    'RP_EXP_TINY_10',
    'RP_EXP_AND_ITEM_FIRST_STEP_20'
)
  AND line.sort_order = 0;

UPDATE items
SET name = '첫걸음의 조각',
    category = 'QUEST',
    type = 'ETC',
    equipment_compatibility_kind = NULL,
    rarity = 'COMMON',
    base_attrs = JSON_OBJECT(),
    stackable = TRUE,
    max_stack = 99,
    max_durability = NULL,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE code = 'IT_FIRST_STEP_FRAGMENT';
