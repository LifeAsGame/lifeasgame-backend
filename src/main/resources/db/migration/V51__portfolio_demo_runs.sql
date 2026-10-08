CREATE TABLE portfolio_demo_capacity (
    id TINYINT PRIMARY KEY,
    last_creation_day DATE NOT NULL,
    creations_today INT NOT NULL
);
INSERT INTO portfolio_demo_capacity VALUES (1, '1970-01-01', 0);

CREATE TABLE portfolio_demo_proofs (
    id CHAR(36) PRIMARY KEY,
    cookie_hash BINARY(32) NOT NULL,
    header_hash BINARY(32) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    UNIQUE KEY uq_portfolio_demo_proof_cookie (cookie_hash)
);

CREATE TABLE portfolio_demo_runs (
    id CHAR(36) PRIMARY KEY,
    proof_id CHAR(36) NOT NULL,
    attempt_key VARCHAR(80) NOT NULL,
    manager_hash BINARY(32) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_code VARCHAR(40) NULL,
    lease_until DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    UNIQUE KEY uq_portfolio_demo_attempt (proof_id, attempt_key),
    UNIQUE KEY uq_portfolio_demo_manager (manager_hash),
    KEY idx_portfolio_demo_expiry (status, expires_at)
);

CREATE TABLE portfolio_demo_actors (
    run_id CHAR(36) NOT NULL,
    actor VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    player_id BIGINT NULL,
    PRIMARY KEY (run_id, actor),
    UNIQUE KEY uq_portfolio_demo_actor_user (user_id),
    UNIQUE KEY uq_portfolio_demo_actor_player (player_id)
);

CREATE TABLE portfolio_demo_peer_links (
    code_hash BINARY(32) PRIMARY KEY,
    run_id CHAR(36) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    redeemed_at DATETIME(6) NULL
);

CREATE TABLE portfolio_demo_balance_grants (
    run_id CHAR(36) PRIMARY KEY,
    player_id BIGINT NOT NULL
);

CREATE TABLE portfolio_demo_targets (
    run_id CHAR(36) PRIMARY KEY,
    listing_id BIGINT NULL,
    role_id BIGINT NULL,
    project_life_log_id BIGINT NULL,
    channel_id BIGINT NULL
);
