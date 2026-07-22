-- A reward-free completion still uses the settlement lifecycle with an empty active profile.
INSERT INTO reward_profiles (
    code, name, status, created_at, updated_at
) VALUES (
    'RP_NONE', 'No Reward Profile', 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
);
