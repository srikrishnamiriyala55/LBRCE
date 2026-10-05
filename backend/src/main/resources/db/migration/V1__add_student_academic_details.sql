CREATE TABLE IF NOT EXISTS student_academic_details (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    branch VARCHAR(20) NOT NULL,
    study_year INT NOT NULL,
    semester INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_student_academic_year UNIQUE (student_id, academic_year_id),
    CONSTRAINT fk_student_academic_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_student_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(id),
    INDEX ix_student_academic_year (academic_year_id, branch, study_year, semester)
);

-- Only seed the active year from legacy current-profile fields; never invent
-- historical programme information.
INSERT INTO student_academic_details (student_id, academic_year_id, branch, study_year, semester, status, created_at, updated_at)
SELECT s.id, ay.id, s.branch, s.year, s.semester, 'ACTIVE', NOW(), NOW()
FROM student s JOIN academic_year ay ON ay.active = TRUE
WHERE s.branch IS NOT NULL AND s.year IS NOT NULL AND s.semester IS NOT NULL
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);
