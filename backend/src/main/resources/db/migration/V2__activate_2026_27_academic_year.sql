-- Roll the installation forward while preserving every 2025-26 record as history.
INSERT INTO academic_year (year_name, start_date, end_date, active, created_at)
SELECT '2026-27', '2026-06-01', '2027-05-31', FALSE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM academic_year WHERE year_name = '2026-27');

SET @new_academic_year_id = (SELECT id FROM academic_year WHERE year_name = '2026-27' LIMIT 1);

UPDATE transport_allocation
SET status = 'INACTIVE', deactivated_at = COALESCE(deactivated_at, NOW()), updated_at = NOW()
WHERE academic_year_id <> @new_academic_year_id AND status = 'ACTIVE';

UPDATE bus_pass
SET status = 'EXPIRED', revoked_at = COALESCE(revoked_at, NOW()), updated_at = NOW()
WHERE academic_year_id <> @new_academic_year_id AND status = 'ACTIVE';

UPDATE transfer_request
SET status = 'CANCELLED', processed_at = COALESCE(processed_at, NOW()),
    remarks = COALESCE(remarks, 'Closed during academic-year rollover'), updated_at = NOW()
WHERE academic_year_id <> @new_academic_year_id
  AND status IN ('TRANSFER_REQUESTED', 'OLD_INCHARGE_APPROVED', 'NEW_INCHARGE_APPROVED');

UPDATE bus_application
SET status = 'CANCELLED', remarks = COALESCE(remarks, 'Closed during academic-year rollover'),
    reviewed_at = COALESCE(reviewed_at, NOW()), reviewed_by = COALESCE(reviewed_by, 'SYSTEM'), updated_at = NOW()
WHERE academic_year_id <> @new_academic_year_id
  AND status IN ('PENDING', 'UNDER_REVIEW');

UPDATE academic_year SET active = FALSE WHERE id <> @new_academic_year_id;
UPDATE academic_year SET active = TRUE WHERE id = @new_academic_year_id;

INSERT INTO student_academic_details
    (student_id, academic_year_id, branch, study_year, semester, status, created_at, updated_at)
SELECT s.id, @new_academic_year_id, s.branch, s.year, s.semester, 'ACTIVE', NOW(), NOW()
FROM student s
WHERE s.branch IS NOT NULL AND s.year IS NOT NULL AND s.semester IS NOT NULL
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

UPDATE bus SET available_seats = total_seats WHERE status = 'ACTIVE';
