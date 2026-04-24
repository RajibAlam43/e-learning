CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =========================
-- USERS / AUTH
-- =========================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_code VARCHAR(50) UNIQUE,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    phone_country_code VARCHAR(5),
    full_name VARCHAR(255) NOT NULL,
    password_hash TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'active',
    email_verified_at TIMESTAMPTZ,
    phone_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_users_status
        CHECK (status IN ('active', 'suspended', 'deleted')),

    CONSTRAINT chk_users_email_or_phone
        CHECK (email IS NOT NULL OR phone IS NOT NULL)
);

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    session_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_id UUID REFERENCES refresh_tokens(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at TIMESTAMPTZ
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO roles (name)
VALUES ('student'), ('instructor'), ('admin');

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    avatar_url TEXT,
    locale VARCHAR(20) NOT NULL DEFAULT 'bn-BD',
    timezone VARCHAR(100),
    bio TEXT,
    extra_json JSONB
);

-- =========================
-- INSTRUCTORS
-- =========================

CREATE TABLE instructor_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(255) NOT NULL,
    headline VARCHAR(255),
    institution VARCHAR(255),
    expertise_area VARCHAR(255),
    about TEXT,
    photo_url TEXT,
    is_public BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =========================
-- COURSE CATALOG
-- =========================

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    parent_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    short_description TEXT,
    description TEXT,
    language VARCHAR(20) NOT NULL DEFAULT 'bn',
    level VARCHAR(30) NOT NULL DEFAULT 'beginner',
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    thumbnail_url TEXT,
    price_bdt INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'draft',
    submitted_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    created_by UUID NOT NULL REFERENCES users(id),
    approved_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_course_language
        CHECK (language IN ('bn', 'en')),

    CONSTRAINT chk_course_level
        CHECK (level IN ('beginner', 'intermediate', 'advanced')),

    CONSTRAINT chk_course_price
        CHECK (price_bdt >= 0),

    CONSTRAINT chk_course_status
        CHECK (status IN ('draft', 'published', 'archived'))
);

CREATE TABLE course_instructors (
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    instructor_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'primary',
    PRIMARY KEY (course_id, instructor_user_id)
);

CREATE TABLE course_sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (course_id, position)
);

-- =========================
-- MEDIA / LESSONS / NOTES
-- =========================

CREATE TABLE media_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(30) NOT NULL,
    asset_type VARCHAR(30) NOT NULL DEFAULT 'video',

    provider_asset_id TEXT,
    playback_id TEXT,
    playback_policy VARCHAR(30),

    file_url TEXT,
    duration_sec INTEGER,
    status VARCHAR(30) NOT NULL DEFAULT 'ready',

    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_media_provider
        CHECK (provider IN ('mux', 'youtube', 'r2', 'external')),

    CONSTRAINT chk_media_asset_type
        CHECK (asset_type IN ('video', 'pdf', 'image', 'audio', 'subtitle', 'other')),

    CONSTRAINT chk_media_playback_policy
        CHECK (playback_policy IS NULL OR playback_policy IN ('public', 'signed')),

    CONSTRAINT chk_media_status
        CHECK (status IN ('uploading', 'processing', 'ready', 'failed', 'deleted'))
);

CREATE TABLE lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    section_id UUID REFERENCES course_sections(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL,
    lesson_type VARCHAR(30) NOT NULL DEFAULT 'video',

    media_asset_id UUID REFERENCES media_assets(id) ON DELETE SET NULL,

    is_preview_free BOOLEAN NOT NULL DEFAULT false,
    status VARCHAR(30) NOT NULL DEFAULT 'draft',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (course_id, slug),
    UNIQUE (course_id, position),

    CONSTRAINT chk_lesson_type
        CHECK (lesson_type IN ('video', 'quiz', 'live', 'assignment', 'pdf')),

    CONSTRAINT chk_lesson_status
        CHECK (status IN ('draft', 'published'))
);

CREATE TABLE lesson_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id UUID NOT NULL UNIQUE REFERENCES lessons(id) ON DELETE CASCADE,
    content_md TEXT,
    content_html TEXT,
    attachments_json JSONB,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =========================
-- ENROLLMENT / PROGRESS
-- =========================

CREATE TABLE enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'active',
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,

    UNIQUE (user_id, course_id),

    CONSTRAINT chk_enrollment_status
        CHECK (status IN ('active', 'refunded', 'revoked'))
);

CREATE TABLE lesson_progress (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    completed BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMPTZ,
    last_position_sec INTEGER,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, lesson_id)
);

-- =========================
-- QUIZZES
-- =========================

CREATE TABLE quizzes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    lesson_id UUID REFERENCES lessons(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    passing_score_pct INTEGER NOT NULL DEFAULT 60,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    time_limit_sec INTEGER,
    is_published BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_quiz_score
        CHECK (passing_score_pct BETWEEN 0 AND 100),

    CONSTRAINT chk_quiz_attempts
        CHECK (max_attempts > 0)
);

CREATE TABLE quiz_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(30) NOT NULL DEFAULT 'mcq',
    points INTEGER NOT NULL DEFAULT 1,

    UNIQUE (quiz_id, position),

    CONSTRAINT chk_question_type
        CHECK (question_type IN ('mcq')),

    CONSTRAINT chk_question_points
        CHECK (points > 0)
);

CREATE TABLE quiz_choices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    choice_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE quiz_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    attempt_no INTEGER NOT NULL,
    score_pct INTEGER,
    passed BOOLEAN,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at TIMESTAMPTZ,

    UNIQUE (quiz_id, user_id, attempt_no),

    CONSTRAINT chk_attempt_no
        CHECK (attempt_no > 0),

    CONSTRAINT chk_attempt_score
        CHECK (score_pct IS NULL OR score_pct BETWEEN 0 AND 100)
);

CREATE TABLE quiz_attempt_answers (
    attempt_id UUID NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    choice_id UUID NOT NULL REFERENCES quiz_choices(id) ON DELETE CASCADE,

    PRIMARY KEY (attempt_id, question_id)
);

-- =========================
-- ORDERS / PAYMENTS
-- =========================

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount_bdt INTEGER NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
    provider VARCHAR(50) NOT NULL,
    provider_txn_id TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,

    CONSTRAINT chk_order_amount
        CHECK (amount_bdt >= 0),

    CONSTRAINT chk_order_provider
        CHECK (provider IN ('bkash', 'nagad', 'card', 'manual', 'sslcommerz')),

    CONSTRAINT chk_order_status
        CHECK (status IN ('pending', 'paid', 'failed', 'refunded', 'cancelled'))
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id),
    price_bdt INTEGER NOT NULL,
    discount_bdt INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT chk_order_item_price
        CHECK (price_bdt >= 0),

    CONSTRAINT chk_order_item_discount
        CHECK (discount_bdt >= 0)
);

CREATE TABLE payment_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    provider VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    provider_event_id TEXT,
    raw_payload_json JSONB NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'received',
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_payment_event_status
        CHECK (status IN ('received', 'processed', 'failed', 'ignored'))
);

-- =========================
-- CERTIFICATES
-- =========================

CREATE TABLE certificate_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    template_json JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    certificate_code VARCHAR(100) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    template_id UUID REFERENCES certificate_templates(id) ON DELETE SET NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    pdf_url TEXT,
    issued_by UUID REFERENCES users(id),

    UNIQUE (user_id, course_id)
);

-- =========================
-- SUPPORT / FAQ / AUDIT
-- =========================

CREATE TABLE support_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(30),
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'open',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ,

    CONSTRAINT chk_support_status
        CHECK (status IN ('open', 'closed'))
);

CREATE TABLE faqs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id TEXT,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =========================
-- INDEXES
-- =========================

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_session_id ON refresh_tokens(session_id);

CREATE INDEX idx_courses_status_published_at ON courses(status, published_at);
CREATE INDEX idx_courses_category_id ON courses(category_id);
CREATE INDEX idx_courses_created_by ON courses(created_by);

CREATE INDEX idx_course_sections_course_position ON course_sections(course_id, position);

CREATE INDEX idx_media_assets_provider_asset_id ON media_assets(provider, provider_asset_id);
CREATE INDEX idx_media_assets_created_by ON media_assets(created_by);

CREATE INDEX idx_lessons_course_position ON lessons(course_id, position);
CREATE INDEX idx_lessons_section_id ON lessons(section_id);
CREATE INDEX idx_lessons_media_asset_id ON lessons(media_asset_id);

CREATE INDEX idx_enrollments_user_id ON enrollments(user_id);
CREATE INDEX idx_enrollments_course_id ON enrollments(course_id);
CREATE INDEX idx_enrollments_status ON enrollments(status);

CREATE INDEX idx_lesson_progress_user_id ON lesson_progress(user_id);
CREATE INDEX idx_lesson_progress_lesson_id ON lesson_progress(lesson_id);

CREATE INDEX idx_quizzes_course_id ON quizzes(course_id);
CREATE INDEX idx_quiz_attempts_user_quiz ON quiz_attempts(user_id, quiz_id);

CREATE INDEX idx_orders_user_status ON orders(user_id, status);
CREATE INDEX idx_orders_provider_txn_id ON orders(provider_txn_id);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_course_id ON order_items(course_id);

CREATE INDEX idx_payment_events_order_id ON payment_events(order_id);
CREATE INDEX idx_payment_events_provider_event_id ON payment_events(provider_event_id);

CREATE INDEX idx_certificates_code ON certificates(certificate_code);
CREATE INDEX idx_certificates_user_id ON certificates(user_id);

CREATE INDEX idx_faqs_published_position ON faqs(is_published, position);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

CREATE INDEX idx_email_verification_tokens_user_id ON email_verification_tokens(user_id);