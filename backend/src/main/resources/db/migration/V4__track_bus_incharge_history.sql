CREATE TABLE bus_incharge_assignment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bus_id BIGINT NOT NULL,
    incharge_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    assigned_at DATETIME(6) NOT NULL,
    ended_at DATETIME(6) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    assigned_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bus_incharge_year UNIQUE (bus_id, incharge_id, academic_year_id),
    CONSTRAINT fk_bia_bus FOREIGN KEY (bus_id) REFERENCES bus(id),
    CONSTRAINT fk_bia_incharge FOREIGN KEY (incharge_id) REFERENCES incharge(id),
    CONSTRAINT fk_bia_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(id),
    INDEX idx_bia_incharge_year (incharge_id, academic_year_id),
    INDEX idx_bia_bus_year (bus_id, academic_year_id)
);

INSERT INTO bus_incharge_assignment
    (bus_id,incharge_id,academic_year_id,assigned_at,ended_at,status,assigned_by)
SELECT b.id,b.incharge_id,a.academic_year_id,MIN(COALESCE(a.allocated_at,a.created_at,b.created_at,NOW())),
       CASE WHEN ay.active=TRUE THEN NULL ELSE MAX(COALESCE(a.deactivated_at,ay.end_date,NOW())) END,
       CASE WHEN ay.active=TRUE THEN 'ACTIVE' ELSE 'ENDED' END,'MIGRATION'
FROM transport_allocation a
JOIN bus b ON b.id=a.bus_id
JOIN academic_year ay ON ay.id=a.academic_year_id
WHERE b.incharge_id IS NOT NULL
GROUP BY b.id,b.incharge_id,a.academic_year_id,ay.active;

-- Also cover an active bus that does not have an allocation yet.
INSERT IGNORE INTO bus_incharge_assignment
    (bus_id,incharge_id,academic_year_id,assigned_at,status,assigned_by)
SELECT b.id,b.incharge_id,ay.id,COALESCE(b.updated_at,b.created_at,NOW()),'ACTIVE','MIGRATION'
FROM bus b JOIN academic_year ay ON ay.active=TRUE
WHERE b.incharge_id IS NOT NULL;
