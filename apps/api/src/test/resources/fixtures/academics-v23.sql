INSERT INTO users(id,full_name,email,password_hash) VALUES
('00000000-0000-0000-0000-000000000001','Historical learner','academic-history@example.com','x'),
('00000000-0000-0000-0000-000000000002','Manual learner','academic-history-manual@example.com','x');
INSERT INTO course_templates(id,title,level,language) VALUES
('00000000-0000-0000-0000-000000000011','Purchased course','BEGINNER','EN'),
('00000000-0000-0000-0000-000000000012','Later course','BEGINNER','EN');
INSERT INTO course_offerings(id,course_template_id,slug,name,status,created_by) VALUES
('00000000-0000-0000-0000-000000000021','00000000-0000-0000-0000-000000000011',
 'historical-original','Original cohort','PUBLISHED','00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000022','00000000-0000-0000-0000-000000000012',
 'historical-later','Later cohort','DRAFT','00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000023','00000000-0000-0000-0000-000000000011',
 'historical-repeat','Repeated cohort','PUBLISHED','00000000-0000-0000-0000-000000000001');
INSERT INTO collections(id,title,slug,collection_type,status,created_by) VALUES
('00000000-0000-0000-0000-000000000031','Historical diploma','historical-diploma',
 'DIPLOMA','PUBLISHED','00000000-0000-0000-0000-000000000001');
-- Current catalog differs from the historical purchased course snapshot.
INSERT INTO collection_courses(collection_id,course_offering_id,position,is_mandatory) VALUES
('00000000-0000-0000-0000-000000000031','00000000-0000-0000-0000-000000000022',1,true);
INSERT INTO orders(id,user_id,amount_bdt,provider,provider_txn_id,status,paid_at) VALUES
('00000000-0000-0000-0000-000000000041','00000000-0000-0000-0000-000000000001',
 1000,'SSLCOMMERZ','historical-payment','PAID','2025-01-01T00:00:00Z');
INSERT INTO order_items(id,order_id,collection_id,item_type,title_snapshot,price_bdt) VALUES
('00000000-0000-0000-0000-000000000051','00000000-0000-0000-0000-000000000041',
 '00000000-0000-0000-0000-000000000031','COLLECTION','Purchased diploma',1000);
INSERT INTO order_item_courses(order_item_id,course_offering_id,position,is_mandatory) VALUES
('00000000-0000-0000-0000-000000000051','00000000-0000-0000-0000-000000000021',1,true);
INSERT INTO enrollments(id,user_id,course_offering_id,status,enrolled_at,completed_at,expires_at) VALUES
('00000000-0000-0000-0000-000000000061','00000000-0000-0000-0000-000000000001',
 '00000000-0000-0000-0000-000000000021','ACTIVE','2025-01-01T00:00:00Z',
 '2025-06-01T00:00:00Z','2030-01-01T00:00:00Z'),
('00000000-0000-0000-0000-000000000062','00000000-0000-0000-0000-000000000001',
 '00000000-0000-0000-0000-000000000023','REFUNDED','2025-07-01T00:00:00Z',
 '2025-12-01T00:00:00Z',NULL);
INSERT INTO collection_enrollments(id,user_id,collection_id,source_order_item_id,
 enrolled_at,completed_at,expires_at) VALUES
('00000000-0000-0000-0000-000000000071','00000000-0000-0000-0000-000000000001',
 '00000000-0000-0000-0000-000000000031','00000000-0000-0000-0000-000000000051',
 '2025-01-01T00:00:00Z','2025-06-01T00:00:00Z','2030-01-01T00:00:00Z'),
('00000000-0000-0000-0000-000000000072','00000000-0000-0000-0000-000000000002',
 '00000000-0000-0000-0000-000000000031',NULL,'2025-07-01T00:00:00Z',NULL,NULL);
INSERT INTO certificates(id,certificate_code,user_id,target_type,course_offering_id,
 enrollment_id,recipient_name,target_title,target_slug,pdf_object_key) VALUES
('00000000-0000-0000-0000-000000000081','GII-HISTORICAL-COURSE',
 '00000000-0000-0000-0000-000000000001','COURSE','00000000-0000-0000-0000-000000000021',
 '00000000-0000-0000-0000-000000000061','Historical learner','Purchased course',
 'historical-original','certificates/retained-course.pdf');
INSERT INTO certificates(id,certificate_code,user_id,target_type,collection_id,
 collection_enrollment_id,recipient_name,target_title,target_slug,pdf_object_key) VALUES
('00000000-0000-0000-0000-000000000082','GII-HISTORICAL-PROGRAM',
 '00000000-0000-0000-0000-000000000001','COLLECTION','00000000-0000-0000-0000-000000000031',
 '00000000-0000-0000-0000-000000000071','Historical learner','Historical diploma',
 'historical-diploma','certificates/retained-program.pdf');
