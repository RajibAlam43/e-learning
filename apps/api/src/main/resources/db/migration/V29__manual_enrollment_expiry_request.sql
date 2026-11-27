-- Preserve all existing grants and audit records; only future requests use this field.
ALTER TABLE manual_enrollment_audit ADD COLUMN requested_expires_at timestamptz;
