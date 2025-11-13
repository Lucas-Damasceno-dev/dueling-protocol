-- Migration: Add blockchain_token_id column to cards table
-- This migration adds support for tracking blockchain token IDs for cards

-- Connect to dueling_db
\c dueling_db

-- Add blockchain_token_id column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'cards' 
        AND column_name = 'blockchain_token_id'
    ) THEN
        ALTER TABLE cards ADD COLUMN blockchain_token_id BIGINT;
        RAISE NOTICE 'Column blockchain_token_id added to cards table';
    ELSE
        RAISE NOTICE 'Column blockchain_token_id already exists in cards table';
    END IF;
END $$;

-- Create index for better performance on blockchain_token_id lookups
CREATE INDEX IF NOT EXISTS idx_cards_blockchain_token_id 
ON cards(blockchain_token_id) 
WHERE blockchain_token_id IS NOT NULL;

-- Display migration status
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'cards' 
            AND column_name = 'blockchain_token_id'
        ) THEN '✅ Migration completed successfully'
        ELSE '❌ Migration failed'
    END AS status;
