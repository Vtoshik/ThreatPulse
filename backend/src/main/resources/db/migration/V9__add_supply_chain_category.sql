-- The AI prompt can answer SUPPLY_CHAIN, so the database type must accept it.
-- Keep OTHER as the last value. A migration with only this statement is safe
-- inside Flyway's transaction because the new value is not used in it.
ALTER TYPE category_enum ADD VALUE IF NOT EXISTS 'SUPPLY_CHAIN' BEFORE 'OTHER';
