-- ============================================================================
-- KYNECTIA DATABASE INITIAL SCHEMA
-- Migración: 001_initial_schema.sql
-- ============================================================================

-- Extensión UUID si se requiere
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Función de actualización automática de `updated_at`
CREATE OR REPLACE FUNCTION set_updated_at_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ----------------------------------------------------------------------------
-- 1. ROLES Y PERMISOS
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TRIGGER trg_roles_updated_at
BEFORE UPDATE ON roles
FOR EACH ROW EXECUTE FUNCTION set_updated_at_timestamp();

CREATE TABLE IF NOT EXISTS permissions (
    id SERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    module VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id INT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id INT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ----------------------------------------------------------------------------
-- 2. USUARIOS ADMINISTRADORES
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_users (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_id INT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TRIGGER trg_admin_users_updated_at
BEFORE UPDATE ON admin_users
FOR EACH ROW EXECUTE FUNCTION set_updated_at_timestamp();

CREATE INDEX idx_admin_users_email ON admin_users(email);
CREATE INDEX idx_admin_users_is_active ON admin_users(is_active);

-- ----------------------------------------------------------------------------
-- 3. AUDITORÍA ADMINISTRATIVA
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    admin_id INT REFERENCES admin_users(id) ON DELETE SET NULL,
    admin_email VARCHAR(150),
    action VARCHAR(50) NOT NULL,           -- CREATE, UPDATE, DELETE, LOGIN, STATUS_CHANGE
    entity_type VARCHAR(50) NOT NULL,      -- users, exercises, routines, etc.
    entity_id VARCHAR(50),
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_admin_id ON audit_logs(admin_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- ----------------------------------------------------------------------------
-- 4. USUARIOS DE LA PLATAFORMA (ATLETAS / CLIENTES)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    phone VARCHAR(30),
    avatar_url TEXT,
    birth_date DATE,
    gender VARCHAR(20),                   -- male, female, other, unspecified
    height_cm NUMERIC(5,2),
    weight_kg NUMERIC(5,2),
    fitness_goal VARCHAR(100),            -- hypertrophy, weight_loss, endurance, health
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    email_verified BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at_timestamp();

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- ----------------------------------------------------------------------------
-- 5. GRUPOS MUSCULARES
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS muscle_groups (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    image_url TEXT,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TRIGGER trg_muscle_groups_updated_at
BEFORE UPDATE ON muscle_groups
FOR EACH ROW EXECUTE FUNCTION set_updated_at_timestamp();

CREATE INDEX idx_muscle_groups_name ON muscle_groups(name);
CREATE INDEX idx_muscle_groups_is_active ON muscle_groups(is_active);

-- ----------------------------------------------------------------------------
-- 6. IMPLEMENTOS Y EQUIPAMIENTO DEPORTIVO
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS equipment (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,         -- free_weights, machines, bodyweight, accessories, resistance
    image_url TEXT,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TRIGGER trg_equipment_updated_at
BEFORE UPDATE ON equipment
FOR EACH ROW EXECUTE FUNCTION set_updated_at_timestamp();

CREATE INDEX idx_equipment_name ON equipment(name);
CREATE INDEX idx_equipment_category ON equipment(category);
CREATE INDEX idx_equipment_is_active ON equipment(is_active);

-- ----------------------------------------------------------------------------
-- 7. CATÁLOGO CENTRALIZADO DE EJERCICIOS
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exercises (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) UNIQUE NOT NULL,
    description TEXT,
    execution_instructions TEXT,
    difficulty_level VARCHAR(30) NOT NULL DEFAULT 'intermediate', -- beginner, intermediate, advanced
    exercise_type VARCHAR(50) NOT NULL DEFAULT 'strength',        -- strength, cardio, flexibility, hiit, plyometric
    demo_image_url TEXT,
    demo_video_url TEXT,
    safety_recommendations TEXT,
    publication_status VARCHAR(30) NOT NULL DEFAULT 'published',  -- draft, published, archived
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TRIGGER trg_exercises_updated_at
BEFORE UPDATE ON exercises
FOR EACH ROW EXECUTE FUNCTION set_updated_at_timestamp();

CREATE INDEX idx_exercises_name ON exercises(name);
CREATE INDEX idx_exercises_difficulty ON exercises(difficulty_level);
CREATE INDEX idx_exercises_type ON exercises(exercise_type);
CREATE INDEX idx_exercises_status ON exercises(publication_status);
CREATE INDEX idx_exercises_is_active ON exercises(is_active);

-- Tabla intermedia Ejercicios <-> Grupos Musculares
CREATE TABLE IF NOT EXISTS exercise_muscles (
    exercise_id INT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    muscle_group_id INT NOT NULL REFERENCES muscle_groups(id) ON DELETE RESTRICT,
    is_primary BOOLEAN DEFAULT FALSE NOT NULL,
    PRIMARY KEY (exercise_id, muscle_group_id)
);

CREATE INDEX idx_exercise_muscles_group ON exercise_muscles(muscle_group_id);

-- Tabla intermedia Ejercicios <-> Implementos
CREATE TABLE IF NOT EXISTS exercise_equipment (
    exercise_id INT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    equipment_id INT NOT NULL REFERENCES equipment(id) ON DELETE RESTRICT,
    PRIMARY KEY (exercise_id, equipment_id)
);

CREATE INDEX idx_exercise_equipment_equipment ON exercise_equipment(equipment_id);

-- ----------------------------------------------------------------------------
-- 8. RUTINAS DE ENTRENAMIENTO PREDEFINIDAS
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS workout_routines (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    cover_image_url TEXT,
    goal VARCHAR(100) NOT NULL,                                   -- hypertrophy, strength, fat_loss, conditioning
    difficulty_level VARCHAR(30) NOT NULL DEFAULT 'intermediate', -- beginner, intermediate, advanced
    category VARCHAR(50) NOT NULL DEFAULT 'full_body',            -- full_body, upper_lower, push_pull_legs, split
    estimated_duration_minutes INT NOT NULL DEFAULT 45,
    publication_status VARCHAR(30) NOT NULL DEFAULT 'draft',      -- draft, published, archived
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TRIGGER trg_workout_routines_updated_at
BEFORE UPDATE ON workout_routines
FOR EACH ROW EXECUTE FUNCTION set_updated_at_timestamp();

CREATE INDEX idx_routines_name ON workout_routines(name);
CREATE INDEX idx_routines_status ON workout_routines(publication_status);
CREATE INDEX idx_routines_is_active ON workout_routines(is_active);

-- Detalle de ejercicios configurados en la rutina
CREATE TABLE IF NOT EXISTS routine_exercises (
    id SERIAL PRIMARY KEY,
    routine_id INT NOT NULL REFERENCES workout_routines(id) ON DELETE CASCADE,
    exercise_id INT NOT NULL REFERENCES exercises(id) ON DELETE RESTRICT,
    sequence_order INT NOT NULL DEFAULT 1,
    sets INT NOT NULL DEFAULT 3,
    reps INT NOT NULL DEFAULT 10,
    duration_seconds INT DEFAULT 0,
    rest_seconds INT NOT NULL DEFAULT 60,
    recommended_weight_kg NUMERIC(6,2),
    specific_instructions TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_routine_exercises_routine ON routine_exercises(routine_id, sequence_order);
CREATE INDEX idx_routine_exercises_exercise ON routine_exercises(exercise_id);

-- ----------------------------------------------------------------------------
-- 9. PLANES, PRECIOS Y OFERTA COMERCIAL
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS plans (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    billing_cycle VARCHAR(30) NOT NULL DEFAULT 'monthly', -- monthly, quarterly, biannual, annual, free
    price NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    benefits JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    discount_percent NUMERIC(5,2) DEFAULT 0.00,
    promo_end_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TRIGGER trg_plans_updated_at
BEFORE UPDATE ON plans
FOR EACH ROW EXECUTE FUNCTION set_updated_at_timestamp();

CREATE INDEX idx_plans_is_active ON plans(is_active);

-- Suscripciones de usuarios
CREATE TABLE IF NOT EXISTS user_subscriptions (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    plan_id INT NOT NULL REFERENCES plans(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL DEFAULT 'active', -- active, expired, cancelled, pending
    start_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    auto_renew BOOLEAN DEFAULT FALSE NOT NULL,
    price_paid NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TRIGGER trg_user_subscriptions_updated_at
BEFORE UPDATE ON user_subscriptions
FOR EACH ROW EXECUTE FUNCTION set_updated_at_timestamp();

CREATE INDEX idx_user_subscriptions_user ON user_subscriptions(user_id);
CREATE INDEX idx_user_subscriptions_plan ON user_subscriptions(plan_id);
CREATE INDEX idx_user_subscriptions_status ON user_subscriptions(status);

-- ----------------------------------------------------------------------------
-- 10. GESTIÓN DE PUBLICIDAD Y ANUNCIOS
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ads (
    id SERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    media_url TEXT NOT NULL,
    destination_url TEXT,
    placement VARCHAR(50) NOT NULL DEFAULT 'banner_home', -- banner_home, interstitial_workout, sidebar, footer
    priority INT NOT NULL DEFAULT 1,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    impressions_count INT NOT NULL DEFAULT 0,
    clicks_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TRIGGER trg_ads_updated_at
BEFORE UPDATE ON ads
FOR EACH ROW EXECUTE FUNCTION set_updated_at_timestamp();

CREATE INDEX idx_ads_placement ON ads(placement);
CREATE INDEX idx_ads_is_active ON ads(is_active);
CREATE INDEX idx_ads_dates ON ads(starts_at, ends_at);

-- ----------------------------------------------------------------------------
-- 11. GESTIÓN DE RECURSOS MULTIMEDIA
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS media_assets (
    id SERIAL PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) UNIQUE NOT NULL,
    file_path TEXT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    asset_type VARCHAR(30) NOT NULL DEFAULT 'image', -- image, video, document
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_media_assets_filename ON media_assets(stored_filename);
CREATE INDEX idx_media_assets_asset_type ON media_assets(asset_type);
