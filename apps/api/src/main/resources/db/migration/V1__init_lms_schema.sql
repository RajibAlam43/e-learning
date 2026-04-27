CREATE
EXTENSION IF NOT EXISTS "pgcrypto";-- =========================
-- USERS / AUTH
-- =========================

CREATE TABLE users (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                student_code VARCHAR(50) UNIQUE,
                                                                         email TEXT UNIQUE,
                                                                                    phone VARCHAR(20) UNIQUE,
                                                                                                      phone_country_code VARCHAR(5),
                                                                                                                         full_name TEXT NOT NULL,
                                                                                                                                        password_hash TEXT NOT NULL,
                                                                                                                                                           status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                                                                                                                                                                                               email_verified_at TIMESTAMPTZ,
                                                                                                                                                                                               phone_verified_at TIMESTAMPTZ,
                                                                                                                                                                                               created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                                                               CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE',
                                                                                                                                                                                                                                                                                                                             'SUSPENDED',
                                                                                                                                                                                                                                                                                                                             'DELETED')), CONSTRAINT chk_users_email_or_phone CHECK (email IS NOT NULL
                                                                                                                                                                                                                                                                                                                                                                                     OR phone IS NOT NULL));
CREATE TABLE email_verification_tokens
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL,
                                            token_hash TEXT NOT NULL UNIQUE,
                                                                     expires_at TIMESTAMPTZ NOT NULL,
                                                                                            used_at TIMESTAMPTZ,
                                                                                            revoked_at TIMESTAMPTZ,
                                                                                            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                    CONSTRAINT fk_email_verification_tokens_user
   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE);
CREATE TABLE password_reset_tokens
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL,
                                            token_hash TEXT NOT NULL UNIQUE,
                                                                     expires_at TIMESTAMPTZ NOT NULL,
                                                                                            used_at TIMESTAMPTZ,
                                                                                            revoked_at TIMESTAMPTZ,
                                                                                            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                    CONSTRAINT fk_password_reset_tokens_user
   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE);
CREATE TABLE refresh_tokens
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL,
                                            token_hash TEXT NOT NULL UNIQUE,
                                                                     session_id UUID NOT NULL,
                                                                                     expires_at TIMESTAMPTZ NOT NULL,
                                                                                                            used_at TIMESTAMPTZ,
                                                                                                            revoked_at TIMESTAMPTZ,
                                                                                                            replaced_by_token_id UUID,
                                                                                                                                 last_used_at TIMESTAMPTZ,
                                                                                                                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                         CONSTRAINT fk_refresh_tokens_user
   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                                         CONSTRAINT fk_refresh_tokens_replaced_by
   FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens (id) ON DELETE
   SET NULL);
CREATE TABLE roles (id BIGSERIAL PRIMARY KEY,
                                 name VARCHAR(50) NOT NULL UNIQUE);
CREATE TABLE user_roles
  (user_id UUID NOT NULL,
                role_id BIGINT NOT NULL,
                               PRIMARY KEY (user_id,
                                            role_id), CONSTRAINT fk_user_roles_user
   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                                         CONSTRAINT fk_user_roles_role
   FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE);
CREATE TABLE user_profiles
  (user_id UUID PRIMARY KEY,
                avatar_url TEXT, locale VARCHAR(20) NOT NULL DEFAULT 'bn-BD',
                                                                     timezone VARCHAR(100),
                                                                              bio TEXT, extra_json JSONB,
                                                                                                   CONSTRAINT fk_user_profiles_user
   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE);
CREATE TABLE instructor_profiles
  (user_id UUID PRIMARY KEY,
                display_name TEXT NOT NULL,
                                  headline TEXT, institution TEXT, expertise_area TEXT, about TEXT, photo_url TEXT, is_public BOOLEAN NOT NULL DEFAULT true,
                                                                                                                                                       credentials_text TEXT, specialties_json JSONB,
                                                                                                                                                                                               years_experience INTEGER, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                                                                                         CONSTRAINT fk_instructor_profiles_user
   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                                         CONSTRAINT chk_instructor_profiles_years_experience_non_negative CHECK (years_experience IS NULL
                                                                                                                                 OR years_experience >= 0));-- =========================================================
-- COURSE CATALOG
-- =========================================================

CREATE TABLE categories
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               name VARCHAR(150) NOT NULL,
                                                 slug VARCHAR(180) NOT NULL UNIQUE,
                                                                            parent_id UUID,
                                                                                      created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                              CONSTRAINT fk_categories_parent
   FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE
   SET NULL);
CREATE TABLE courses (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                  title TEXT NOT NULL,
                                                             slug TEXT NOT NULL UNIQUE,
                                                                                thumbnail_url TEXT, short_description TEXT, description TEXT, highlights JSONB,
                                                                                                                                                         price_bdt NUMERIC(12, 2) NOT NULL DEFAULT 0,
                                                                                                                                                                                                   course_outcomes JSONB,
                                                                                                                                                                                                                   requirements JSONB,
                                                                                                                                                                                                                                prerequisites TEXT, level VARCHAR(30) NOT NULL DEFAULT 'BEGINNER',
                                                                                                                                                                                                                                                                                       language VARCHAR(20) NOT NULL DEFAULT 'BN',
                                                                                                                                                                                                                                                                                                                             study_mode VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
                                                                                                                                                                                                                                                                                                                                                                     status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                                                                                                                                                                                                                                                                                                                                                                                                         published_at TIMESTAMPTZ,
                                                                                                                                                                                                                                                                                                                                                                                                         is_free BOOLEAN NOT NULL DEFAULT false,
                                                                                                                                                                                                                                                                                                                                                                                                                                          live_session_count INTEGER NOT NULL,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                     quiz_count INTEGER NOT NULL,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        recorded_hours_count INTEGER NOT NULL,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     created_by UUID NOT NULL,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     preview_lesson_id UUID,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       estimated_duration_minutes INTEGER, target_audience TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 CONSTRAINT fk_courses_created_by
                      FOREIGN KEY (created_by) REFERENCES users (id),
                                                          CONSTRAINT chk_course_language CHECK (language IN ('BN',
                                                                                                             'EN')) , CONSTRAINT chk_course_level CHECK (level IN ('BEGINNER',
                                                                                                                                                                   'INTERMEDIATE',
                                                                                                                                                                   'ADVANCED')), CONSTRAINT chk_course_study_mode CHECK (study_mode IN ('SCHEDULED',
                                                                                                                                                                                                                                        'SELF_PACED')), CONSTRAINT chk_course_status CHECK (status IN ('DRAFT',
                                                                                                                                                                                                                                                                                                       'PUBLISHED',
                                                                                                                                                                                                                                                                                                       'ARCHIVED')), CONSTRAINT chk_course_price CHECK (price_bdt >= 0), CONSTRAINT chk_courses_estimated_duration_non_negative CHECK (estimated_duration_minutes IS NULL
                                                                                                                                                                                                                                                                                                                                                                                                                                       OR estimated_duration_minutes >= 0), CONSTRAINT chk_courses_counts_non_negative CHECK (live_session_count >= 0
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              AND quiz_count >= 0
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              AND recorded_hours_count >= 0));
CREATE TABLE course_categories
  (course_id UUID NOT NULL,
                  category_id UUID NOT NULL,
                                   PRIMARY KEY (course_id,
                                                category_id), CONSTRAINT fk_course_categories_course
   FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
                                                             CONSTRAINT fk_course_categories_category
   FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE);
CREATE TABLE course_instructors
  (course_id UUID NOT NULL,
                  instructor_user_id UUID NOT NULL,
                                          role VARCHAR(50) NOT NULL DEFAULT 'primary',
                                                                            PRIMARY KEY (course_id,
                                                                                         instructor_user_id), CONSTRAINT fk_course_instructors_course
   FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
                                                             CONSTRAINT fk_course_instructors_instructor
   FOREIGN KEY (instructor_user_id) REFERENCES users (id) ON DELETE CASCADE);
CREATE TABLE media_assets
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               provider VARCHAR(30) NOT NULL,
                                                    asset_type VARCHAR(30) NOT NULL DEFAULT 'VIDEO',
                                                                                            provider_asset_id TEXT, playback_id TEXT, playback_policy VARCHAR(30),
                                                                                                                                                      file_url TEXT, duration_sec INTEGER, status VARCHAR(30) NOT NULL DEFAULT 'READY',
                                                                                                                                                                                                                               created_by UUID,
                                                                                                                                                                                                                                          created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                                                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                                                                                                          CONSTRAINT fk_media_assets_created_by
   FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE
   SET NULL,
       CONSTRAINT chk_media_provider CHECK (provider IN ('MUX',
                                                         'YOUTUBE',
                                                         'R2',
                                                         'BUNNY')), CONSTRAINT chk_media_asset_type CHECK (asset_type IN ('VIDEO',
                                                                                                                          'PDF',
                                                                                                                          'IMAGE')), CONSTRAINT chk_media_playback_policy CHECK (playback_policy IS NULL
                                                                                                                                                                                 OR playback_policy IN ('PUBLIC',
                                                                                                                                                                                                        'SIGNED')), CONSTRAINT chk_media_status CHECK (status IN ('UPLOADING',
                                                                                                                                                                                                                                                                  'PROCESSING',
                                                                                                                                                                                                                                                                  'READY',
                                                                                                                                                                                                                                                                  'FAILED',
                                                                                                                                                                                                                                                                  'DELETED')), CONSTRAINT chk_media_duration_non_negative CHECK (duration_sec IS NULL
                                                                                                                                                                                                                                                                                                                                 OR duration_sec >= 0));
CREATE TABLE course_sections
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               course_id UUID NOT NULL,
                                              title TEXT NOT NULL,
                                                         slug TEXT NOT NULL,
                                                                   position INTEGER NOT NULL,
                                                                                    description TEXT, is_mandatory BOOLEAN NOT NULL DEFAULT false,
                                                                                                                                            is_free BOOLEAN NOT NULL DEFAULT false,
                                                                                                                                                                             status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                                                                                                                                                                                                                 published_at TIMESTAMPTZ,
                                                                                                                                                                                                                 release_type VARCHAR(30) NOT NULL DEFAULT 'IMMEDIATE',
                                                                                                                                                                                                                                                           release_at TIMESTAMPTZ,
                                                                                                                                                                                                                                                           unlock_after_days INTEGER, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                                                                                                              updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                                                                                                                                                      CONSTRAINT fk_course_sections_course
   FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
                                                             CONSTRAINT uk_course_sections_course_position UNIQUE (course_id,
                                                                                                                   position), CONSTRAINT uk_course_sections_course_slug UNIQUE (course_id,
                                                                                                                                                                                slug), CONSTRAINT chk_course_sections_status CHECK (status IN ('DRAFT',
                                                                                                                                                                                                                                               'PUBLISHED',
                                                                                                                                                                                                                                               'ARCHIVED')), CONSTRAINT chk_course_sections_release_type CHECK (release_type IN ('IMMEDIATE',
                                                                                                                                                                                                                                                                                                                                 'FIXED_DATE',
                                                                                                                                                                                                                                                                                                                                 'RELATIVE_DAYS')), CONSTRAINT chk_course_sections_unlock_after_days_non_negative CHECK (unlock_after_days IS NULL
                                                                                                                                                                                                                                                                                                                                                                                                                         OR unlock_after_days >= 0));
CREATE TABLE lessons
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               course_id UUID NOT NULL,
                                              section_id UUID NOT NULL,
                                                              title TEXT NOT NULL,
                                                                         slug TEXT NOT NULL,
                                                                                   position INTEGER NOT NULL,
                                                                                                    is_mandatory BOOLEAN NOT NULL DEFAULT false,
                                                                                                                                          lesson_type VARCHAR(30) NOT NULL DEFAULT 'VIDEO',
                                                                                                                                                                                   primary_media_asset_id UUID,
                                                                                                                                                                                                          is_free BOOLEAN NOT NULL DEFAULT false,
                                                                                                                                                                                                                                           status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                                                                                                                                                                                                                                                                               duration_seconds INTEGER, thumbnail_url TEXT, transcript_url TEXT, release_type VARCHAR(30),
                                                                                                                                                                                                                                                                                                                                                               release_at TIMESTAMPTZ,
                                                                                                                                                                                                                                                                                                                                                               unlock_after_days INTEGER, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                                                                                                                                                                                                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                                                                                                                                                                                                                                                          CONSTRAINT fk_lessons_course
   FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
                                                             CONSTRAINT fk_lessons_section
   FOREIGN KEY (section_id) REFERENCES course_sections (id) ON DELETE CASCADE,
                                                                      CONSTRAINT fk_lessons_primary_media_asset
   FOREIGN KEY (primary_media_asset_id) REFERENCES media_assets (id) ON DELETE
   SET NULL,
       CONSTRAINT uk_lessons_primary_media_asset UNIQUE (primary_media_asset_id), CONSTRAINT uk_lessons_course_slug UNIQUE (course_id,
                                                                                                                            slug), CONSTRAINT uk_lessons_section_position UNIQUE (section_id,
                                                                                                                                                                                  position), CONSTRAINT chk_lesson_type CHECK (lesson_type IN ('VIDEO',
                                                                                                                                                                                                                                               'QUIZ',
                                                                                                                                                                                                                                               'LIVE',
                                                                                                                                                                                                                                               'ASSIGNMENT',
                                                                                                                                                                                                                                               'PDF')), CONSTRAINT chk_lesson_status CHECK (status IN ('DRAFT',
                                                                                                                                                                                                                                                                                                       'PUBLISHED',
                                                                                                                                                                                                                                                                                                       'ARCHIVED')), CONSTRAINT chk_lessons_release_type CHECK (release_type IS NULL
                                                                                                                                                                                                                                                                                                                                                                OR release_type IN ('IMMEDIATE',
                                                                                                                                                                                                                                                                                                                                                                                    'FIXED_DATE',
                                                                                                                                                                                                                                                                                                                                                                                    'RELATIVE_DAYS')), CONSTRAINT chk_lessons_duration_seconds_non_negative CHECK (duration_seconds IS NULL
                                                                                                                                                                                                                                                                                                                                                                                                                                                                   OR duration_seconds >= 0), CONSTRAINT chk_lessons_unlock_after_days_non_negative CHECK (unlock_after_days IS NULL
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           OR unlock_after_days >= 0));
ALTER TABLE courses ADD
CONSTRAINT fk_courses_preview_lesson

FOREIGN KEY (preview_lesson_id) REFERENCES lessons (id)
    ON
DELETE

   SET NULL;
CREATE TABLE lesson_resources
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               lesson_id UUID NOT NULL,
                                              resource_type VARCHAR(20) NOT NULL,
                                                                        title TEXT, file_url TEXT NOT NULL,
                                                                                                  mime_type TEXT, position INTEGER NOT NULL,
                                                                                                                                   created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                           updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                   CONSTRAINT fk_lesson_resources_lesson
   FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE,
                                                             CONSTRAINT uk_lesson_resources_lesson_position UNIQUE (lesson_id,
                                                                                                                    position), CONSTRAINT chk_lesson_resources_resource_type CHECK (resource_type IN ('PDF',
                                                                                                                                                                                                      'IMAGE')));-- =========================================================
-- ENROLLMENT / PROGRESS
-- =========================================================

CREATE TABLE enrollments
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL,
                                            course_id UUID NOT NULL,
                                                           status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                                                                                               enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                        revoked_at TIMESTAMPTZ,
                                                                                                                                        completed_at TIMESTAMPTZ,
                                                                                                                                        expires_at TIMESTAMPTZ,
                                                                                                                                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                CONSTRAINT fk_enrollments_user
   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                                         CONSTRAINT fk_enrollments_course
   FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
                                                             CONSTRAINT uk_enrollments_user_course UNIQUE (user_id,
                                                                                                           course_id), CONSTRAINT chk_enrollment_status CHECK (status IN ('ACTIVE',
                                                                                                                                                                          'REFUNDED',
                                                                                                                                                                          'REVOKED')));
CREATE TABLE lesson_progress
  (user_id UUID NOT NULL,
                lesson_id UUID NOT NULL,
                               completed BOOLEAN NOT NULL DEFAULT false,
                                                                  completed_at TIMESTAMPTZ,
                                                                  last_position_sec INTEGER, updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                     PRIMARY KEY (user_id,
                                                                                                                                                  lesson_id), CONSTRAINT fk_lesson_progress_user
   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                                         CONSTRAINT fk_lesson_progress_lesson
   FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE,
                                                             CONSTRAINT chk_lesson_progress_last_position_non_negative CHECK (last_position_sec IS NULL
                                                                                                                              OR last_position_sec >= 0));-- =========================================================
-- QUIZZES
-- =========================================================

CREATE TABLE quizzes
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               course_id UUID NOT NULL,
                                              lesson_id UUID,
                                                        title TEXT NOT NULL,
                                                                   passing_score_pct INTEGER NOT NULL DEFAULT 60,
                                                                                                              max_attempts INTEGER NOT NULL DEFAULT 3,
                                                                                                                                                    time_limit_sec INTEGER, is_published BOOLEAN NOT NULL DEFAULT false,
                                                                                                                                                                                                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                                          updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                                                                                  CONSTRAINT fk_quizzes_course
   FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
                                                             CONSTRAINT fk_quizzes_lesson
   FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE,
                                                             CONSTRAINT chk_quiz_score CHECK (passing_score_pct BETWEEN 0 AND 100), CONSTRAINT chk_quiz_attempts CHECK (max_attempts > 0), CONSTRAINT chk_quiz_time_limit_non_negative CHECK (time_limit_sec IS NULL
                                                                                                                                                                                                                                              OR time_limit_sec >= 0));
CREATE TABLE quiz_questions
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               quiz_id UUID NOT NULL,
                                            position INTEGER NOT NULL,
                                                             question_text TEXT NOT NULL,
                                                                                question_type VARCHAR(30) NOT NULL DEFAULT 'MCQ',
                                                                                                                           points INTEGER NOT NULL DEFAULT 1,
                                                                                                                                                           explanation_text TEXT, CONSTRAINT fk_quiz_questions_quiz
   FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE,
                                                           CONSTRAINT uk_quiz_questions_quiz_position UNIQUE (quiz_id,
                                                                                                              position), CONSTRAINT chk_question_type CHECK (question_type IN ('MCQ')), CONSTRAINT chk_question_points CHECK (points > 0));
CREATE TABLE quiz_choices
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               question_id UUID NOT NULL,
                                                choice_text TEXT NOT NULL,
                                                                 is_correct BOOLEAN NOT NULL DEFAULT false,
                                                                                                     CONSTRAINT fk_quiz_choices_question
   FOREIGN KEY (question_id) REFERENCES quiz_questions (id) ON DELETE CASCADE,
                                                                      CONSTRAINT uq_quiz_choices_question_id_id UNIQUE (question_id,
                                                                                                                        id));
CREATE TABLE quiz_attempts
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               quiz_id UUID NOT NULL,
                                            user_id UUID NOT NULL,
                                                         attempt_no INTEGER NOT NULL,
                                                                            score_pct INTEGER, passed BOOLEAN, started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                       submitted_at TIMESTAMPTZ,
                                                                                                                                                       CONSTRAINT fk_quiz_attempts_quiz
   FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE,
                                                           CONSTRAINT fk_quiz_attempts_user
   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                                         CONSTRAINT uk_quiz_attempts_quiz_user_attempt UNIQUE (quiz_id,
                                                                                                               user_id,
                                                                                                               attempt_no), CONSTRAINT chk_attempt_no CHECK (attempt_no > 0), CONSTRAINT chk_attempt_score CHECK (score_pct IS NULL
                                                                                                                                                                                                                  OR score_pct BETWEEN 0 AND 100));
CREATE TABLE quiz_attempt_answers
  (attempt_id UUID NOT NULL,
                   question_id UUID NOT NULL,
                                    choice_id UUID NOT NULL,
                                                   PRIMARY KEY (attempt_id,
                                                                question_id), CONSTRAINT fk_quiz_attempt_answers_attempt
   FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (id) ON DELETE CASCADE,
                                                                    CONSTRAINT fk_quiz_attempt_answers_question
   FOREIGN KEY (question_id) REFERENCES quiz_questions (id) ON DELETE CASCADE,
                                                                      CONSTRAINT fk_quiz_attempt_answers_choice_for_question
   FOREIGN KEY (question_id,
                choice_id) REFERENCES quiz_choices (question_id, id) ON DELETE CASCADE);-- =========================================================
-- ORDERS / PAYMENTS
-- =========================================================

CREATE TABLE orders
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL,
                                            amount_bdt NUMERIC(12, 2) NOT NULL,
                                                                      currency VARCHAR(10) NOT NULL DEFAULT 'BDT',
                                                                                                            provider VARCHAR(50) NOT NULL,
                                                                                                                                 provider_txn_id TEXT, status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                                                                                                                                                                                           paid_at TIMESTAMPTZ,
                                                                                                                                                                                           refunded_at TIMESTAMPTZ,
                                                                                                                                                                                           created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                                   CONSTRAINT fk_orders_user
   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                                         CONSTRAINT chk_order_amount CHECK (amount_bdt >= 0), CONSTRAINT chk_order_provider CHECK (provider IN ('SSLCOMMERZ',
                                                                                                                                                                'BKASH')), CONSTRAINT chk_order_status CHECK (status IN ('PENDING',
                                                                                                                                                                                                                         'PAID',
                                                                                                                                                                                                                         'FAILED',
                                                                                                                                                                                                                         'REFUNDED',
                                                                                                                                                                                                                         'CANCELLED')));
CREATE TABLE order_items
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               order_id UUID NOT NULL,
                                             course_id UUID NOT NULL,
                                                            price_bdt NUMERIC(12, 2) NOT NULL,
                                                                                     discount_bdt NUMERIC(12, 2) NOT NULL DEFAULT 0,
                                                                                                                                  CONSTRAINT fk_order_items_order
   FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
                                                           CONSTRAINT fk_order_items_course
   FOREIGN KEY (course_id) REFERENCES courses (id),
                                      CONSTRAINT chk_order_item_price CHECK (price_bdt >= 0), CONSTRAINT chk_order_item_discount CHECK (discount_bdt >= 0));
CREATE TABLE payment_events
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               order_id UUID,
                                        provider VARCHAR(50) NOT NULL,
                                                             event_type VARCHAR(100) NOT NULL,
                                                                                     provider_event_id TEXT, raw_payload_json JSONB NOT NULL,
                                                                                                                                    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
                                                                                                                                                                        processed_at TIMESTAMPTZ,
                                                                                                                                                                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                                                CONSTRAINT fk_payment_events_order
   FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE
   SET NULL,
       CONSTRAINT chk_payment_event_provider CHECK (provider IN ('SSLCOMMERZ',
                                                                 'BKASH')), CONSTRAINT chk_payment_event_status CHECK (status IN ('RECEIVED',
                                                                                                                                  'PROCESSED',
                                                                                                                                  'FAILED',
                                                                                                                                  'IGNORED')));-- =========================================================
-- CERTIFICATES
-- =========================================================

CREATE TABLE certificate_templates (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                                name TEXT NOT NULL,
                                                                          template_json JSONB NOT NULL,
                                                                                              is_active BOOLEAN NOT NULL DEFAULT true,
                                                                                                                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                         updated_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE certificates
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               certificate_code VARCHAR(100) NOT NULL UNIQUE,
                                                                      user_id UUID NOT NULL,
                                                                                   course_id UUID NOT NULL,
                                                                                                  template_id UUID,
                                                                                                              issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                     revoked_at TIMESTAMPTZ,
                                                                                                                                                     pdf_url TEXT, issued_by UUID,
                                                                                                                                                                             recipient_name TEXT NOT NULL,
                                                                                                                                                                                                 course_title TEXT NOT NULL,
                                                                                                                                                                                                                   CONSTRAINT fk_certificates_user
   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                                         CONSTRAINT fk_certificates_course
   FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
                                                             CONSTRAINT fk_certificates_template
   FOREIGN KEY (template_id) REFERENCES certificate_templates (id) ON DELETE
   SET NULL,
       CONSTRAINT fk_certificates_issued_by
   FOREIGN KEY (issued_by) REFERENCES users (id) ON DELETE
   SET NULL,
       CONSTRAINT uk_certificates_user_course UNIQUE (user_id,
                                                      course_id));-- =========================================================
-- SUPPORT / FAQ / AUDIT
-- =========================================================

CREATE TABLE support_tickets
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID,
                                       name TEXT, email TEXT, phone VARCHAR(30),
                                                                    subject TEXT NOT NULL,
                                                                                 message TEXT NOT NULL,
                                                                                              status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
                                                                                                                                  closed_at TIMESTAMPTZ,
                                                                                                                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                          CONSTRAINT fk_support_tickets_user
   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE
   SET NULL,
       CONSTRAINT chk_support_status CHECK (status IN ('OPEN',
                                                       'CLOSED')));
CREATE TABLE faqs (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                               question TEXT NOT NULL,
                                                             answer TEXT NOT NULL,
                                                                         position INTEGER NOT NULL DEFAULT 0,
                                                                                                           is_published BOOLEAN NOT NULL DEFAULT true,
                                                                                                                                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                                         updated_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE audit_logs
  (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               actor_user_id UUID,
                                             action VARCHAR(100) NOT NULL,
                                                                 entity_type VARCHAR(100) NOT NULL,
                                                                                          entity_id TEXT, metadata_json JSONB,
                                                                                                                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                                                                                                                                CONSTRAINT fk_audit_logs_actor
   FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE
   SET NULL);-- =========================================================
-- SEED DATA
-- =========================================================

INSERT INTO roles (name)

VALUES ('student'),
       ('instructor'),
       ('admin');-- =========================================================
-- INDEXES
-- =========================================================

CREATE INDEX idx_users_email
    ON users(email);
CREATE INDEX idx_users_phone
    ON users(phone);
CREATE INDEX idx_users_student_code
    ON users (student_code);
CREATE INDEX idx_user_roles_role_id
    ON user_roles (role_id);
CREATE INDEX idx_email_verification_tokens_user_id
    ON email_verification_tokens (user_id);
CREATE INDEX idx_password_reset_tokens_user_id
    ON password_reset_tokens (user_id);
CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_session_id
    ON refresh_tokens(session_id);
CREATE INDEX idx_refresh_tokens_replaced_by_token_id
    ON refresh_tokens (replaced_by_token_id);
CREATE INDEX idx_categories_parent_id
    ON categories (parent_id);
CREATE INDEX idx_courses_status_published_at
    ON courses(status, published_at);
CREATE INDEX idx_courses_created_by
    ON courses(created_by);
CREATE INDEX idx_courses_preview_lesson_id
    ON courses (preview_lesson_id);
CREATE INDEX idx_course_categories_category_id
    ON course_categories (category_id);
CREATE INDEX idx_course_instructors_instructor_user_id
    ON course_instructors (instructor_user_id);
CREATE INDEX idx_media_assets_created_by
    ON media_assets (created_by);
CREATE INDEX idx_media_assets_provider_asset_id
    ON media_assets(provider, provider_asset_id);
CREATE INDEX idx_course_sections_course_id
    ON course_sections (course_id);
CREATE INDEX idx_lessons_course_id
    ON lessons (course_id);
CREATE INDEX idx_lessons_section_id
    ON lessons(section_id);
CREATE INDEX idx_lessons_primary_media_asset_id
    ON lessons (primary_media_asset_id);
CREATE INDEX idx_lesson_resources_lesson_id
    ON lesson_resources (lesson_id);
CREATE INDEX idx_enrollments_user_id
    ON enrollments(user_id);
CREATE INDEX idx_enrollments_course_id
    ON enrollments(course_id);
CREATE INDEX idx_enrollments_status
    ON enrollments(status);
CREATE INDEX idx_enrollments_expires_at
    ON enrollments (expires_at);
CREATE INDEX idx_lesson_progress_lesson_id
    ON lesson_progress(lesson_id);
CREATE INDEX idx_quizzes_course_id
    ON quizzes(course_id);
CREATE INDEX idx_quizzes_lesson_id
    ON quizzes (lesson_id);
CREATE UNIQUE INDEX uk_quizzes_lesson_id_not_null
    ON quizzes (lesson_id)

 WHERE lesson_id IS NOT NULL;
CREATE UNIQUE INDEX uk_quizzes_course_id_when_lesson_null
    ON quizzes (course_id)

 WHERE lesson_id IS NULL;
CREATE INDEX idx_quiz_questions_quiz_id
    ON quiz_questions (quiz_id);
CREATE INDEX idx_quiz_choices_question_id
    ON quiz_choices (question_id);
CREATE INDEX idx_quiz_attempts_user_quiz
    ON quiz_attempts(user_id, quiz_id);
CREATE INDEX idx_orders_user_status
    ON orders(user_id, status);
CREATE INDEX idx_orders_provider_txn_id
    ON orders(provider_txn_id);
CREATE UNIQUE INDEX uk_orders_provider_provider_txn_id_not_null
    ON orders (provider, provider_txn_id)

 WHERE provider_txn_id IS NOT NULL;
CREATE INDEX idx_order_items_order_id
    ON order_items(order_id);
CREATE INDEX idx_order_items_course_id
    ON order_items(course_id);
CREATE INDEX idx_payment_events_order_id
    ON payment_events(order_id);
CREATE INDEX idx_payment_events_provider_event_id
    ON payment_events(provider_event_id);
CREATE UNIQUE INDEX uk_payment_events_provider_provider_event_id_not_null
    ON payment_events (provider, provider_event_id)

 WHERE provider_event_id IS NOT NULL;
CREATE INDEX idx_certificates_code
    ON certificates(certificate_code);
CREATE INDEX idx_certificates_user_id
    ON certificates(user_id);
CREATE INDEX idx_certificates_course_id
    ON certificates (course_id);
CREATE INDEX idx_support_tickets_status
    ON support_tickets (status);
CREATE INDEX idx_faqs_published_position
    ON faqs(is_published, position);
CREATE INDEX idx_audit_logs_actor
    ON audit_logs(actor_user_id);
CREATE INDEX idx_audit_logs_entity
    ON audit_logs(entity_type, entity_id);
CREATE UNIQUE INDEX uk_media_assets_provider_playback_id_not_null
    ON media_assets (provider, playback_id)

 WHERE playback_id IS NOT NULL;

--# Option A: pgFormatter (best for PostgreSQL)
--brew install pgformatter
--pg_format -i 4 -w 120 -L -o apps/api/src/main/resources/db/migration/V1__init_lms_schema.sql apps/api/src/main/resources/db/migration/V1__init_lms_schema.sql
