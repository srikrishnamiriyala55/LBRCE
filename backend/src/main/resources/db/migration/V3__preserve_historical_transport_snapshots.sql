-- Immutable allocation snapshots keep historical passes/reports correct even
-- after student, bus, boarding-point, fee or in-charge master data changes.
ALTER TABLE transport_allocation
    ADD COLUMN student_name_snapshot VARCHAR(255) NULL,
    ADD COLUMN roll_number_snapshot VARCHAR(100) NULL,
    ADD COLUMN branch_snapshot VARCHAR(50) NULL,
    ADD COLUMN study_year_snapshot INT NULL,
    ADD COLUMN semester_snapshot INT NULL,
    ADD COLUMN phone_number_snapshot VARCHAR(30) NULL,
    ADD COLUMN bus_number_snapshot VARCHAR(100) NULL,
    ADD COLUMN starting_point_snapshot VARCHAR(255) NULL,
    ADD COLUMN ending_point_snapshot VARCHAR(255) NULL,
    ADD COLUMN boarding_point_snapshot VARCHAR(255) NULL,
    ADD COLUMN annual_fee_snapshot BIGINT NULL,
    ADD COLUMN incharge_name_snapshot VARCHAR(255) NULL,
    ADD COLUMN incharge_teacher_id_snapshot VARCHAR(100) NULL;

ALTER TABLE bus_application
    ADD COLUMN student_name_snapshot VARCHAR(255) NULL,
    ADD COLUMN roll_number_snapshot VARCHAR(100) NULL,
    ADD COLUMN bus_number_snapshot VARCHAR(100) NULL,
    ADD COLUMN starting_point_snapshot VARCHAR(255) NULL,
    ADD COLUMN ending_point_snapshot VARCHAR(255) NULL,
    ADD COLUMN boarding_point_snapshot VARCHAR(255) NULL,
    ADD COLUMN annual_fee_snapshot BIGINT NULL;

UPDATE bus_application a
JOIN student s ON s.id=a.student_id
JOIN bus b ON b.id=a.bus_id
JOIN boarding_points bp ON bp.id=a.boarding_point_id
SET a.student_name_snapshot=COALESCE(a.student_name_snapshot,s.name),
    a.roll_number_snapshot=COALESCE(a.roll_number_snapshot,s.student_id),
    a.bus_number_snapshot=COALESCE(a.bus_number_snapshot,b.bus_number),
    a.starting_point_snapshot=COALESCE(a.starting_point_snapshot,b.starting_point),
    a.ending_point_snapshot=COALESCE(a.ending_point_snapshot,b.ending_point),
    a.boarding_point_snapshot=COALESCE(a.boarding_point_snapshot,bp.station_name),
    a.annual_fee_snapshot=COALESCE(a.annual_fee_snapshot,bp.fee_amount);

UPDATE transport_allocation a
JOIN student s ON s.id = a.student_id
JOIN bus b ON b.id = a.bus_id
JOIN boarding_points bp ON bp.id = a.boarding_point_id
LEFT JOIN incharge i ON i.id = b.incharge_id
LEFT JOIN student_academic_details sad
       ON sad.student_id = a.student_id
      AND sad.academic_year_id = a.academic_year_id
SET a.student_name_snapshot = COALESCE(a.student_name_snapshot, s.name),
    a.roll_number_snapshot = COALESCE(a.roll_number_snapshot, s.student_id),
    a.branch_snapshot = COALESCE(a.branch_snapshot, sad.branch, s.branch),
    a.study_year_snapshot = COALESCE(a.study_year_snapshot, sad.study_year, s.year),
    a.semester_snapshot = COALESCE(a.semester_snapshot, sad.semester, s.semester),
    a.phone_number_snapshot = COALESCE(a.phone_number_snapshot, s.phone_number),
    a.bus_number_snapshot = COALESCE(a.bus_number_snapshot, b.bus_number),
    a.starting_point_snapshot = COALESCE(a.starting_point_snapshot, b.starting_point),
    a.ending_point_snapshot = COALESCE(a.ending_point_snapshot, b.ending_point),
    a.boarding_point_snapshot = COALESCE(a.boarding_point_snapshot, bp.station_name),
    a.annual_fee_snapshot = COALESCE(a.annual_fee_snapshot, bp.fee_amount),
    a.incharge_name_snapshot = COALESCE(a.incharge_name_snapshot, i.name),
    a.incharge_teacher_id_snapshot = COALESCE(a.incharge_teacher_id_snapshot, i.teacher_id);

CREATE INDEX idx_allocation_bus_year_status
    ON transport_allocation (bus_id, academic_year_id, status);
CREATE INDEX idx_application_bus_year_status
    ON bus_application (bus_id, academic_year_id, status);
CREATE INDEX idx_fee_student_year
    ON fee (student_id, academic_year_id);
CREATE INDEX idx_transfer_student_year_status
    ON transfer_request (student_id, academic_year_id, status);
