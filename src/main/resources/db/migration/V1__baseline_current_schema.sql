-- CLEAN INSTALL ONLY. Do not execute this migration against an existing database.
-- Existing databases must follow docs/database/flyway_baseline_runbook.md.
--
-- Source: current Hibernate/JPA mappings, exported with Hibernate 6.6 MySQLDialect.
-- Creation order: all application and collection tables first (alphabetical),
-- then secondary indexes/unique constraints, and finally foreign keys.

SET NAMES utf8mb4;

CREATE TABLE achievements (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(60) NOT NULL,
    category ENUM ('COLLECTION','COMBAT','DAILY','ECONOMY','EXPLORATION','SKILL','SOCIAL','STORY') NOT NULL,
    desc_md TINYTEXT,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE certification (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category ENUM ('CLOUD','DATA','DATABASE','DESIGN','FINANCE','LANGUAGE','MANAGEMENT','NETWORK','OTHER','PROGRAMMING','SECURITY') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE channel_participants (
    channel_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    version BIGINT,
    role ENUM ('ADMIN','MEMBER','MODERATOR','OWNER') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE chat_channels (
    read_only BIT NOT NULL,
    context_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT,
    name VARCHAR(60),
    type ENUM ('ADMIN','FRIEND','GLOBAL','GUILD','PARTY','SYSTEM') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE chat_messages (
    edited BIT NOT NULL,
    channel_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT,
    content LONGTEXT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE collection_log_tags (
    collection_log_id BIGINT NOT NULL,
    tag VARCHAR(50)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE collection_logs (
    quantity_value INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    acquired_from VARCHAR(100),
    condition_note VARCHAR(100),
    title_original VARCHAR(255),
    title_value VARCHAR(255) NOT NULL,
    category ENUM ('BOOK','CARD','COIN','FIGURE','GAME','OTHER','STAMP') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE combat_events (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    occurred_at DATETIME(6) NOT NULL,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    payload JSON NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE coop_history (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    partner_id BIGINT,
    party_id BIGINT,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    context JSON,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE equipment_slots (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(40) NOT NULL,
    category ENUM ('CHEST','FEET','HANDS','HEAD','LEGS','NECK','RING','TRINKET','WEAPON') NOT NULL,
    role ENUM ('LEFT','MAIN','OFFHAND','RIGHT','SINGLE') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE exercise_logs (
    calories INTEGER,
    distance_km FLOAT(53),
    duration_minutes INTEGER NOT NULL,
    exercised_on DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    memo VARCHAR(200),
    category ENUM ('CYCLING','GYM','OTHER','RUNNING','SWIMMING','WALKING','YOGA') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE exp_history (
    delta INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    context JSON,
    reason ENUM ('ADMIN_ADJUST','COMBAT','CRAFTING','DAILY_BONUS','ETC','PURCHASE_REFUND','QUEST_REWARD') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE follows (
    blocked BIT NOT NULL,
    muted BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    follow_id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    target_player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    state ENUM ('FOLLOWING','STOPPED') NOT NULL,
    PRIMARY KEY (follow_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE guild_members (
    created_at DATETIME(6) NOT NULL,
    guild_id BIGINT NOT NULL,
    guild_member_id BIGINT NOT NULL AUTO_INCREMENT,
    joined_at DATETIME(6) NOT NULL,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    role ENUM ('LEADER','MEMBER','OFFICER') NOT NULL,
    PRIMARY KEY (guild_member_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE guild_tags (
    guild_id BIGINT NOT NULL,
    tag_value VARCHAR(64) NOT NULL,
    PRIMARY KEY (guild_id, tag_value)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE guild_wait_members (
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6),
    guild_id BIGINT NOT NULL,
    guild_wait_member_id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    message VARCHAR(1000),
    status ENUM ('APPROVED','CANCELLED','PENDING','REJECTED') NOT NULL,
    type ENUM ('INVITATION','JOIN_REQUEST') NOT NULL,
    PRIMARY KEY (guild_wait_member_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE guilds (
    max_members INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    guild_id BIGINT NOT NULL AUTO_INCREMENT,
    leader_player_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    emblem_bg_color VARCHAR(16),
    code_value VARCHAR(32) NOT NULL,
    name_original VARCHAR(128) NOT NULL,
    name_value VARCHAR(128) NOT NULL,
    emblem_image_url VARCHAR(512),
    description_md TEXT,
    join_policy ENUM ('APPROVAL','INVITE_ONLY','OPEN') NOT NULL,
    status ENUM ('ACTIVE','DISBANDED','INACTIVE') NOT NULL,
    visibility ENUM ('PRIVATE','PUBLIC') NOT NULL,
    PRIMARY KEY (guild_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE hobbies (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category ENUM ('ARTS','BAKING','BOARD_GAMES','COOKING','CRAFTS','FITNESS','GAMING','LANGUAGE','MUSIC','OUTDOORS','PHOTOGRAPHY','READING','SPORTS','TECH','TRAVEL','VOLUNTEERING','WELLNESS','WRITING') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE inventory_entries (
    bound BIT NOT NULL,
    durability INTEGER,
    quantity INTEGER NOT NULL,
    slot_index INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    inst_attrs JSON,
    rarity ENUM ('COMMON','EPIC','LEGENDARY','RARE','UNCOMMON') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE items (
    max_durability INTEGER,
    max_stack INTEGER NOT NULL,
    stackable BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    name VARCHAR(80) NOT NULL,
    base_attrs JSON,
    category ENUM ('ACCESSORY','ARMOR','CONSUMABLE','MATERIAL','MISC','QUEST','WEAPON') NOT NULL,
    rarity ENUM ('COMMON','EPIC','LEGENDARY','RARE','UNCOMMON') NOT NULL,
    type ENUM ('BOW','CHEST','ETC','HELMET','HERB','KEY','ORE','POTION','RING','SCROLL','SHIELD','STAFF','SWORD') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE listings (
    active_flag INTEGER,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT,
    item_inst_id BIGINT NOT NULL,
    price BIGINT,
    reservation_expires_at DATETIME(6),
    reserved_by BIGINT,
    seller_player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT,
    reservation_token VARCHAR(36),
    reserved_hold_id VARCHAR(36),
    currency ENUM ('GEM','GOLD'),
    status ENUM ('CANCELED','EXPIRED','OPEN','RESERVED','SOLD') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE mailbox_entries (
    bound BIT NOT NULL,
    durability INTEGER,
    quantity INTEGER NOT NULL,
    slot_index INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    inst_attrs JSON,
    rarity ENUM ('COMMON','EPIC','LEGENDARY','RARE','UNCOMMON') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE media_log_tags (
    media_log_id BIGINT NOT NULL,
    tag VARCHAR(50)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE media_logs (
    finished_on DATE,
    progress_current INTEGER NOT NULL,
    progress_total INTEGER NOT NULL,
    rating_score FLOAT(53) NOT NULL,
    rewatch_count INTEGER NOT NULL,
    started_on DATE,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    title_original VARCHAR(255),
    title_value VARCHAR(255) NOT NULL,
    category ENUM ('ANIME','BOOK','GAME','MOVIE','MUSIC','SERIES','WEBTOON') NOT NULL,
    status ENUM ('COMPLETED','DROPPED','ON_HOLD','PLANNED','WATCHING') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE parties (
    max_members INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    leader_player_id BIGINT NOT NULL,
    party_id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    banner_bg_color VARCHAR(16),
    code_value VARCHAR(32) NOT NULL,
    name_original VARCHAR(128) NOT NULL,
    name_value VARCHAR(128) NOT NULL,
    banner_image_url VARCHAR(512),
    description_md TEXT,
    join_policy ENUM ('APPROVAL','INVITE_ONLY','OPEN') NOT NULL,
    status ENUM ('ACTIVE','DISBANDED','INACTIVE') NOT NULL,
    visibility ENUM ('PRIVATE','PUBLIC') NOT NULL,
    PRIMARY KEY (party_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE party_members (
    created_at DATETIME(6) NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    party_id BIGINT NOT NULL,
    party_member_id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    role ENUM ('LEADER','MEMBER','OFFICER') NOT NULL,
    PRIMARY KEY (party_member_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE party_tags (
    party_id BIGINT NOT NULL,
    tag_value VARCHAR(64) NOT NULL,
    PRIMARY KEY (party_id, tag_value)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE party_wait_members (
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6),
    party_id BIGINT NOT NULL,
    party_wait_member_id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    message VARCHAR(1000),
    status ENUM ('APPROVED','CANCELLED','PENDING','REJECTED') NOT NULL,
    type ENUM ('INVITATION','JOIN_REQUEST') NOT NULL,
    PRIMARY KEY (party_wait_member_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE player (
    agi_stat INTEGER NOT NULL,
    dex_stat INTEGER NOT NULL,
    hp_cap INTEGER NOT NULL,
    hp_cur INTEGER NOT NULL,
    int_stat INTEGER NOT NULL,
    level INTEGER NOT NULL,
    luc_stat INTEGER NOT NULL,
    mp_cap INTEGER NOT NULL,
    mp_cur INTEGER NOT NULL,
    str_stat INTEGER NOT NULL,
    vit_stat INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    exp BIGINT NOT NULL,
    guild_id BIGINT,
    id BIGINT NOT NULL AUTO_INCREMENT,
    title_id BIGINT,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    version BIGINT,
    gender VARCHAR(20),
    job VARCHAR(30),
    name VARCHAR(40) NOT NULL,
    extra_stats JSON,
    status_effects VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE player_achievements (
    achievement_id BIGINT NOT NULL,
    acquired_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE player_certifications (
    acquired_date DATE,
    expires_date DATE,
    certification_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    granted_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE player_equipment (
    created_at DATETIME(6) NOT NULL,
    equipped_at DATETIME(6),
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_inst_id BIGINT,
    player_id BIGINT NOT NULL,
    slot_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE player_hobbies (
    proficiency INTEGER NOT NULL,
    started_on DATE,
    hobby_id BIGINT NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    xp BIGINT NOT NULL,
    custom_name VARCHAR(60) NOT NULL,
    detail VARCHAR(200),
    status ENUM ('ACTIVE','DROPPED','PAUSED') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE player_inventory (
    capacity_slots INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT,
    PRIMARY KEY (player_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE player_mailbox (
    capacity_slots INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT,
    PRIMARY KEY (player_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE player_skill_points (
    available_sp INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT,
    PRIMARY KEY (player_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE player_skills (
    level INTEGER NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    version BIGINT,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE player_titles (
    acquired_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    title_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE price_snapshots (
    sample_size INTEGER NOT NULL,
    avg_price BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    taken_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE quest_acceptances (
    period_end DATE NOT NULL,
    period_start DATE NOT NULL,
    progress_value INTEGER NOT NULL,
    completed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    guild_id BIGINT,
    id BIGINT NOT NULL AUTO_INCREMENT,
    party_id BIGINT,
    player_id BIGINT NOT NULL,
    quest_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT,
    idempotency_key VARCHAR(120),
    status ENUM ('CANCELED','DONE','IN_PROGRESS') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE quest_clear_log (
    completed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    quest_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE quests (
    reward_exp INTEGER NOT NULL,
    target_value INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    due_at DATETIME(6),
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    code VARCHAR(80) NOT NULL,
    title_id VARCHAR(120) NOT NULL,
    reward_stats JSON NOT NULL,
    category ENUM ('GUILD','MAIN','PARTY','RECOMMENDED','REPEAT') NOT NULL,
    description_md TINYTEXT,
    repeat_rule ENUM ('DAILY','MONTHLY','NONE','WEEKLY'),
    target_type ENUM ('COUNT','MINUTES','SCORE') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE shop_items (
    available BIT NOT NULL,
    global_stock_limit INTEGER,
    per_player_limit INTEGER,
    reservation_ttl_sec INTEGER,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    price BIGINT,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT,
    currency ENUM ('GEM','GOLD'),
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE shop_purchases (
    quantity INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    reservation_expires_at DATETIME(6),
    shop_item_id BIGINT NOT NULL,
    total_price BIGINT,
    updated_at DATETIME(6) NOT NULL,
    reservation_token VARCHAR(36),
    wallet_hold_id VARCHAR(36),
    currency ENUM ('GEM','GOLD'),
    status ENUM ('CANCELED','COMPLETED','EXPIRED','REQUESTED','RESERVED') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE skill_edges (
    req_level INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    from_skill_id BIGINT NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    to_skill_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE skills (
    max_level INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(80) NOT NULL,
    base_effect JSON,
    kind ENUM ('ACTIVE','PASSIVE') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE stat_change_log (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    reason VARCHAR(40) NOT NULL,
    changes JSON NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE titles (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(60) NOT NULL,
    category ENUM ('ACHIEVEMENT','EVENT','OTHER','QUEST','RANKED','SPECIAL') NOT NULL,
    desc_md TINYTEXT,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE trades (
    fee_bps INTEGER NOT NULL,
    buyer_player_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    fee BIGINT,
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_inst_id BIGINT NOT NULL,
    listing_id BIGINT NOT NULL,
    price BIGINT,
    seller_player_id BIGINT NOT NULL,
    seller_proceeds BIGINT,
    updated_at DATETIME(6) NOT NULL,
    currency ENUM ('GEM','GOLD'),
    fee_currency ENUM ('GEM','GOLD'),
    seller_proceeds_currency ENUM ('GEM','GOLD'),
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_settings (
    volume INTEGER NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    flags JSON,
    ui_layout JSON,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE users (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    nickname VARCHAR(20) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status ENUM ('ACTIVE','BANNED','DELETED','PENDING_EMAIL_VERIFICATION') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wallet_balances (
    amount BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    wallet_id BIGINT NOT NULL,
    currency ENUM ('GEM','GOLD') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wallet_holds (
    amount BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    wallet_id BIGINT NOT NULL,
    hold_id VARCHAR(36) NOT NULL,
    reason VARCHAR(100),
    currency ENUM ('GEM','GOLD') NOT NULL,
    status ENUM ('CANCELED','COMMITTED','EXPIRED','OPEN') NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wallets (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

-- Secondary indexes and unique constraints.
ALTER TABLE achievements
    ADD CONSTRAINT UK9qqq83lafche8dr9o229kc9tg
    UNIQUE (code);

CREATE INDEX idx_channel_user
    ON channel_participants (user_id);

ALTER TABLE channel_participants
    ADD CONSTRAINT uq_channel_user
    UNIQUE (channel_id, user_id);

CREATE INDEX idx_channel_type
    ON chat_channels (type);

CREATE INDEX idx_channel_context
    ON chat_channels (context_id);

CREATE INDEX idx_message_channel_seq
    ON chat_messages (channel_id, id);

CREATE INDEX idx_message_channel_time
    ON chat_messages (channel_id, created_at, id);

CREATE INDEX idx_message_sender
    ON chat_messages (sender_id);

CREATE INDEX idx_collection_player
    ON collection_logs (player_id);

CREATE INDEX idx_collection_category
    ON collection_logs (category);

CREATE INDEX idx_player_time4
    ON combat_events (player_id, occurred_at);

CREATE INDEX idx_party_time3
    ON coop_history (party_id, created_at);

CREATE INDEX idx_player_time3
    ON coop_history (player_id, created_at);

ALTER TABLE equipment_slots
    ADD CONSTRAINT UK3580sud7mp8k4j0gobcyqot4l
    UNIQUE (code);

CREATE INDEX idx_exercise_player
    ON exercise_logs (player_id);

CREATE INDEX idx_exercise_category
    ON exercise_logs (category);

CREATE INDEX idx_exercise_exercised_on
    ON exercise_logs (exercised_on);

CREATE INDEX idx_player_time
    ON exp_history (player_id, created_at);

CREATE INDEX idx_follow_player
    ON follows (player_id);

CREATE INDEX idx_follow_target_player
    ON follows (target_player_id);

ALTER TABLE follows
    ADD CONSTRAINT uk_follower_followee
    UNIQUE (player_id, target_player_id);

CREATE INDEX idx_guild_member_guild
    ON guild_members (guild_id);

CREATE INDEX idx_guild_member_player
    ON guild_members (player_id);

ALTER TABLE guild_members
    ADD CONSTRAINT uk_guild_member_guild_player
    UNIQUE (guild_id, player_id);

CREATE INDEX idx_wait_guild
    ON guild_wait_members (guild_id);

CREATE INDEX idx_wait_guild_player
    ON guild_wait_members (player_id);

ALTER TABLE guilds
    ADD CONSTRAINT UKc44qhgdprwxapo7co98a0fk3
    UNIQUE (code_value);

ALTER TABLE inventory_entries
    ADD CONSTRAINT uq_inventory_slot
    UNIQUE (player_id, slot_index);

CREATE INDEX idx_item_name
    ON items (name);

ALTER TABLE items
    ADD CONSTRAINT UKmnhl79u3u6jdvutuoeq54stne
    UNIQUE (name);

CREATE INDEX idx_status_price
    ON listings (status, price);

CREATE INDEX idx_seller
    ON listings (seller_player_id);

CREATE INDEX idx_item
    ON listings (item_id);

CREATE INDEX idx_reservation_exp
    ON listings (reservation_expires_at);

ALTER TABLE listings
    ADD CONSTRAINT uq_active_item
    UNIQUE (item_inst_id, active_flag);

ALTER TABLE mailbox_entries
    ADD CONSTRAINT uq_mailbox_slot
    UNIQUE (player_id, slot_index);

CREATE INDEX idx_media_player
    ON media_logs (player_id);

CREATE INDEX idx_media_category
    ON media_logs (category);

CREATE INDEX idx_media_status
    ON media_logs (status);

ALTER TABLE parties
    ADD CONSTRAINT UKoix8c6oc2ouo5n3xxbka6ne69
    UNIQUE (code_value);

CREATE INDEX idx_party_member_party
    ON party_members (party_id);

CREATE INDEX idx_party_member_player
    ON party_members (player_id);

CREATE INDEX idx_wait_party
    ON party_wait_members (party_id);

CREATE INDEX idx_wait_party_player
    ON party_wait_members (player_id);

CREATE INDEX player_idx_user
    ON player (user_id);

ALTER TABLE player
    ADD CONSTRAINT UKfpxwfe7n29rwsbyu5p1wl2mq1
    UNIQUE (user_id);

ALTER TABLE player_achievements
    ADD CONSTRAINT uq_player_achv
    UNIQUE (player_id, achievement_id);

CREATE INDEX idx_cert_player
    ON player_certifications (player_id);

ALTER TABLE player_certifications
    ADD CONSTRAINT uq_player_cert
    UNIQUE (player_id, certification_id);

ALTER TABLE player_equipment
    ADD CONSTRAINT uq_player_slot
    UNIQUE (player_id, slot_id);

CREATE INDEX idx_hobby_player
    ON player_hobbies (player_id);

ALTER TABLE player_hobbies
    ADD CONSTRAINT uq_player_hobby
    UNIQUE (player_id, hobby_id);

ALTER TABLE player_skills
    ADD CONSTRAINT pk_player_skill
    UNIQUE (player_id, skill_id);

ALTER TABLE player_titles
    ADD CONSTRAINT uq_player_title
    UNIQUE (player_id, title_id);

CREATE INDEX idx_snapshot_item_time
    ON price_snapshots (item_id, taken_at);

ALTER TABLE price_snapshots
    ADD CONSTRAINT uq_item_time
    UNIQUE (item_id, taken_at);

CREATE INDEX idx_qa_player
    ON quest_acceptances (player_id);

CREATE INDEX idx_qa_quest
    ON quest_acceptances (quest_id);

CREATE INDEX idx_qa_status
    ON quest_acceptances (status);

ALTER TABLE quest_acceptances
    ADD CONSTRAINT uq_repeat
    UNIQUE (player_id, quest_id, period_start, period_end);

CREATE INDEX idx_player_quest
    ON quest_clear_log (player_id, quest_id, completed_at);

ALTER TABLE quests
    ADD CONSTRAINT UKfiypv7mlexb02d6cw4ym91k5h
    UNIQUE (code);

ALTER TABLE shop_items
    ADD CONSTRAINT uq_shop_item
    UNIQUE (item_id, currency);

CREATE INDEX idx_shop_item
    ON shop_purchases (shop_item_id);

CREATE INDEX idx_shop_player
    ON shop_purchases (player_id);

CREATE INDEX idx_shop_status
    ON shop_purchases (status);

ALTER TABLE shop_purchases
    ADD CONSTRAINT uq_shop_res_token
    UNIQUE (reservation_token);

ALTER TABLE skill_edges
    ADD CONSTRAINT uq_edge
    UNIQUE (from_skill_id, to_skill_id);

ALTER TABLE skills
    ADD CONSTRAINT UK2b0y7lhl8xlfp9t6hcncyxjqq
    UNIQUE (code);

CREATE INDEX idx_player_time2
    ON stat_change_log (player_id, created_at);

ALTER TABLE titles
    ADD CONSTRAINT UKp66yoxalyk0n7xgubadcbvw0f
    UNIQUE (code);

CREATE INDEX idx_buyer_time
    ON trades (buyer_player_id, created_at);

CREATE INDEX idx_seller_time
    ON trades (seller_player_id, created_at);

ALTER TABLE users
    ADD CONSTRAINT UK6dotkott2kjsp8vw4d0m25fb7
    UNIQUE (email);

CREATE INDEX idx_balance_wallet
    ON wallet_balances (wallet_id);

ALTER TABLE wallet_balances
    ADD CONSTRAINT uq_wallet_balance_wallet_currency
    UNIQUE (wallet_id, currency);

CREATE INDEX idx_hold_wallet
    ON wallet_holds (wallet_id);

CREATE INDEX idx_hold_expires
    ON wallet_holds (expires_at);

ALTER TABLE wallet_holds
    ADD CONSTRAINT UK4pvblbpln9x8iqhrs5h6l8no7
    UNIQUE (hold_id);

ALTER TABLE wallets
    ADD CONSTRAINT uq_wallet_owner
    UNIQUE (owner_id);

-- Foreign keys are added after every table exists.
ALTER TABLE channel_participants
    ADD CONSTRAINT FK19jte0ydrq8jfj0ylgs05dwd1
    FOREIGN KEY (channel_id)
    REFERENCES chat_channels (id);

ALTER TABLE chat_messages
    ADD CONSTRAINT FK7d773200rrat1tg8s6vw5g36f
    FOREIGN KEY (channel_id)
    REFERENCES chat_channels (id);

ALTER TABLE collection_log_tags
    ADD CONSTRAINT FKcdy8f3gu9e57widftko0psw5x
    FOREIGN KEY (collection_log_id)
    REFERENCES collection_logs (id);

ALTER TABLE guild_members
    ADD CONSTRAINT FK5spmkoxf20vqdvbj4vu4guvx
    FOREIGN KEY (guild_id)
    REFERENCES guilds (guild_id);

ALTER TABLE guild_tags
    ADD CONSTRAINT FK97q23g851x128csvgawkncubg
    FOREIGN KEY (guild_id)
    REFERENCES guilds (guild_id);

ALTER TABLE guild_wait_members
    ADD CONSTRAINT FKgpmu7ivagctltjbnds4jbrwk4
    FOREIGN KEY (guild_id)
    REFERENCES guilds (guild_id);

ALTER TABLE inventory_entries
    ADD CONSTRAINT FKko5xy9pakstl4edw92hbbhn12
    FOREIGN KEY (player_id)
    REFERENCES player_inventory (player_id);

ALTER TABLE mailbox_entries
    ADD CONSTRAINT FKdvtej8whvyqmtwn1i4m65w47
    FOREIGN KEY (player_id)
    REFERENCES player_mailbox (player_id);

ALTER TABLE media_log_tags
    ADD CONSTRAINT FKcxui9ll75ymw610a3wfrfe76q
    FOREIGN KEY (media_log_id)
    REFERENCES media_logs (id);

ALTER TABLE party_members
    ADD CONSTRAINT FK8t7tw0lrh904ld026ludm2vm3
    FOREIGN KEY (party_id)
    REFERENCES parties (party_id);

ALTER TABLE party_tags
    ADD CONSTRAINT FKbsutdo5uyjhxhy1xn9cyo27dv
    FOREIGN KEY (party_id)
    REFERENCES parties (party_id);

ALTER TABLE party_wait_members
    ADD CONSTRAINT FK5imx8cxjmvsywtvikj5qfj0k2
    FOREIGN KEY (party_id)
    REFERENCES parties (party_id);

ALTER TABLE wallet_balances
    ADD CONSTRAINT FKb7vxgyry3jrwtycpd4nipqu38
    FOREIGN KEY (wallet_id)
    REFERENCES wallets (id);

ALTER TABLE wallet_holds
    ADD CONSTRAINT FK5rberb7jrra19mks5xau4964l
    FOREIGN KEY (wallet_id)
    REFERENCES wallets (id);
