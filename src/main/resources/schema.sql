-- Medique SQLite Database Schema

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    salt TEXT NOT NULL,
    full_name TEXT NOT NULL,
    email TEXT,
    phone TEXT,
    theme_preference TEXT DEFAULT 'LIGHT',
    font_scale TEXT DEFAULT 'NORMAL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS emergency_contacts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_name TEXT NOT NULL,
    relationship TEXT,
    email TEXT NOT NULL,
    phone TEXT,
    is_primary INTEGER DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS prescriptions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    medicine_name TEXT NOT NULL,
    medicine_type TEXT NOT NULL, -- 'TABLET', 'SYRUP', 'INJECTION', 'CAPSULE', 'DROPS', 'OTHER'
    dosage_amount TEXT NOT NULL, -- e.g. "1 tablet", "10 ml"
    intake_instructions TEXT,   -- e.g. "After breakfast", "With warm water"
    dose_times TEXT NOT NULL,   -- JSON array or comma-separated times, e.g. '["08:00","14:00","20:00"]'
    start_date TEXT NOT NULL,   -- ISO format 'YYYY-MM-DD'
    end_date TEXT NOT NULL,     -- ISO format 'YYYY-MM-DD'
    doctor_name TEXT,
    notes TEXT,
    is_active INTEGER DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS adherence_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    prescription_id INTEGER NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scheduled_time TEXT NOT NULL, -- ISO timestamp 'YYYY-MM-DDTHH:MM:SS'
    action_time TEXT,             -- ISO timestamp when Taken/Skipped
    status TEXT NOT NULL,         -- 'TAKEN', 'MISSED', 'SKIPPED', 'SNOOZED'
    notes TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sync_queue (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entity_type TEXT NOT NULL,  -- 'USER', 'PRESCRIPTION', 'ADHERENCE', 'CONTACT'
    entity_id INTEGER NOT NULL,
    operation TEXT NOT NULL,    -- 'CREATE', 'UPDATE', 'DELETE'
    payload_json TEXT NOT NULL,
    status TEXT DEFAULT 'PENDING', -- 'PENDING', 'SYNCED', 'FAILED'
    retry_count INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prescriptions_user_active ON prescriptions(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_adherence_user_scheduled ON adherence_logs(user_id, scheduled_time);
CREATE INDEX IF NOT EXISTS idx_adherence_prescription ON adherence_logs(prescription_id);
