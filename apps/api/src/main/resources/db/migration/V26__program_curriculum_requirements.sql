ALTER TABLE collections
    ADD COLUMN required_credits integer,
    ADD COLUMN elective_credit_limit integer NOT NULL DEFAULT 0,
    ADD COLUMN expected_duration_months integer,
    ADD COLUMN access_duration_months integer;
ALTER TABLE collections ADD CONSTRAINT chk_program_requirements CHECK (
    (required_credits IS NULL AND expected_duration_months IS NULL AND access_duration_months IS NULL)
    OR (required_credits IS NOT NULL AND expected_duration_months IS NOT NULL
        AND required_credits > 0 AND expected_duration_months > 0
        AND (access_duration_months IS NULL OR access_duration_months > 0))
);
ALTER TABLE collections ADD CONSTRAINT chk_elective_credit_limit CHECK (elective_credit_limit >= 0);
ALTER TABLE order_items ADD COLUMN program_snapshot jsonb;
ALTER TABLE collection_enrollments ADD COLUMN program_snapshot jsonb;

-- Freeze the existing course/mandatory contract; do not invent historical credit
-- graduation thresholds or expiry dates. Prefer the purchased course snapshot.
UPDATE order_items i SET program_snapshot = jsonb_build_object(
    'collectionType', c.collection_type, 'requiredCredits', NULL,
    'electiveCreditLimit', 0, 'expectedDurationMonths', NULL, 'accessDurationMonths', NULL,
    'courses', COALESCE((
        SELECT jsonb_agg(jsonb_build_object(
            'courseId',o.id,'courseTemplateId',t.id,
            'title',COALESCE(NULLIF(BTRIM(t.title_en),''),t.title),
            'credits',t.credits,'mandatory',s.is_mandatory,'position',s.position)
            ORDER BY s.position)
        FROM order_item_courses s JOIN course_offerings o ON o.id=s.course_offering_id
        JOIN course_templates t ON t.id=o.course_template_id WHERE s.order_item_id=i.id
    ), (
        SELECT jsonb_agg(jsonb_build_object(
            'courseId',o.id,'courseTemplateId',t.id,
            'title',COALESCE(NULLIF(BTRIM(t.title_en),''),t.title),
            'credits',t.credits,'mandatory',s.is_mandatory,'position',s.position)
            ORDER BY s.position)
        FROM collection_courses s JOIN course_offerings o ON o.id=s.course_offering_id
        JOIN course_templates t ON t.id=o.course_template_id WHERE s.collection_id=i.collection_id
    ), '[]'::jsonb)
) FROM collections c WHERE i.collection_id=c.id;
UPDATE collection_enrollments e SET program_snapshot=i.program_snapshot
FROM order_items i WHERE e.source_order_item_id=i.id;
UPDATE collection_enrollments e SET program_snapshot=jsonb_build_object(
    'collectionType',c.collection_type,'requiredCredits',NULL,
    'electiveCreditLimit',0,'expectedDurationMonths',NULL,'accessDurationMonths',NULL,
    'courses',COALESCE((SELECT jsonb_agg(jsonb_build_object(
        'courseId',o.id,'courseTemplateId',t.id,
        'title',COALESCE(NULLIF(BTRIM(t.title_en),''),t.title),
        'credits',t.credits,'mandatory',s.is_mandatory,'position',s.position)
        ORDER BY s.position)
        FROM collection_courses s JOIN course_offerings o ON o.id=s.course_offering_id
        JOIN course_templates t ON t.id=o.course_template_id WHERE s.collection_id=e.collection_id
    ),'[]'::jsonb)
) FROM collections c WHERE e.collection_id=c.id AND e.program_snapshot IS NULL;

CREATE FUNCTION preserve_program_snapshot() RETURNS trigger AS $$
BEGIN
    IF OLD.program_snapshot IS NOT NULL AND NEW.program_snapshot IS DISTINCT FROM OLD.program_snapshot THEN
        RAISE EXCEPTION 'Purchased program requirements are immutable';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_order_program_snapshot BEFORE UPDATE ON order_items
    FOR EACH ROW EXECUTE FUNCTION preserve_program_snapshot();
CREATE TRIGGER trg_enrollment_program_snapshot BEFORE UPDATE ON collection_enrollments
    FOR EACH ROW EXECUTE FUNCTION preserve_program_snapshot();
