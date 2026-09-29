-- Retain parent identity independently of outbox retention. The claim and child
-- outbox insertion commit in the same transaction; failed publication leaves no claim.
CREATE TABLE quest_reward_ready_publications (
    parent_event_id CHAR(36) NOT NULL,
    claim_token CHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (parent_event_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
