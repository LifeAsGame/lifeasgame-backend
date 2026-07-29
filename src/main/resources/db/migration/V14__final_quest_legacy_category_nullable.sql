ALTER TABLE quests
    MODIFY COLUMN category ENUM (
        'GUILD',
        'MAIN',
        'PARTY',
        'RECOMMENDED',
        'REPEAT'
    ) NULL;
