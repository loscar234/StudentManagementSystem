-- ============================================================
-- PHASE 2 SCHEMA MIGRATION: Academic Period, Governance & Auditing
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    person_id       INT NULL UNIQUE,
    username        VARCHAR(50) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            ENUM('ADMIN', 'REGISTRAR', 'LECTURER', 'LIBRARIAN', 'APPROVER') NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until    DATETIME NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login      DATETIME NULL,
    CONSTRAINT fk_user_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE SET NULL
);

-- TABLE: academic_terms
CREATE TABLE IF NOT EXISTS academic_terms (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    academic_year         VARCHAR(10) NOT NULL,
    semester              INT NOT NULL CHECK (semester BETWEEN 1 AND 2),
    start_date            DATE NOT NULL,
    end_date              DATE NOT NULL,
    registration_start    DATE NOT NULL,
    registration_deadline DATE NOT NULL,
    status                ENUM('UPCOMING', 'ACTIVE', 'CLOSED', 'ARCHIVED') NOT NULL DEFAULT 'UPCOMING',
    is_current            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_term_year_sem (academic_year, semester)
);

-- Seed initial terms
INSERT INTO academic_terms (academic_year, semester, start_date, end_date, registration_start, registration_deadline, status, is_current)
VALUES 
('2023/2024', 2, '2024-01-15', '2024-05-30', '2024-01-05', '2024-02-15', 'CLOSED', FALSE),
('2024/2025', 1, '2024-09-01', '2025-01-31', '2024-09-01', '2024-10-15', 'ACTIVE', TRUE),
('2024/2025', 2, '2025-02-01', '2025-06-30', '2025-01-15', '2025-02-28', 'UPCOMING', FALSE)
ON DUPLICATE KEY UPDATE status = VALUES(status), is_current = VALUES(is_current);

-- TABLE: course_prerequisites
CREATE TABLE IF NOT EXISTS course_prerequisites (
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    course_id              INT NOT NULL,
    prerequisite_course_id INT NOT NULL,
    min_grade              VARCHAR(5) DEFAULT 'D',
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_course_prereq (course_id, prerequisite_course_id),
    CONSTRAINT fk_cp_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_prereq FOREIGN KEY (prerequisite_course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- Seed course prerequisites
INSERT IGNORE INTO course_prerequisites (course_id, prerequisite_course_id, min_grade)
SELECT c1.id, c2.id, 'D'
FROM courses c1, courses c2
WHERE c1.course_code = 'CS201' AND c2.course_code = 'CS101';

INSERT IGNORE INTO course_prerequisites (course_id, prerequisite_course_id, min_grade)
SELECT c1.id, c2.id, 'D'
FROM courses c1, courses c2
WHERE c1.course_code = 'CS301' AND c2.course_code = 'CS101';

INSERT IGNORE INTO course_prerequisites (course_id, prerequisite_course_id, min_grade)
SELECT c1.id, c2.id, 'D'
FROM courses c1, courses c2
WHERE c1.course_code = 'CS401' AND c2.course_code = 'SE201';

-- TABLE: academic_audit_log (Audit trail for authorized exception actions)
CREATE TABLE IF NOT EXISTS academic_audit_log (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    entity_type      ENUM('ENROLLMENT', 'SCORE', 'TERM') NOT NULL,
    entity_id        INT NULL,
    student_id       INT NULL,
    course_id        INT NULL,
    academic_year    VARCHAR(10) NOT NULL,
    semester         INT NOT NULL,
    action           VARCHAR(50) NOT NULL,
    previous_value   TEXT NULL,
    new_value        TEXT NULL,
    reason           TEXT NOT NULL,
    authorized_by    INT NULL COMMENT 'user_id who authorized the change',
    authorized_user  VARCHAR(50) NOT NULL COMMENT 'username captured at authorization time',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_course  FOREIGN KEY (course_id)  REFERENCES courses(id)  ON DELETE SET NULL
);

-- Update scores table to add status and finalization tracking if not present
SET @col_status := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scores' AND COLUMN_NAME = 'status');
SET @sql := IF(@col_status = 0, 'ALTER TABLE scores ADD COLUMN status ENUM(\'DRAFT\', \'SUBMITTED\', \'FINALIZED\') NOT NULL DEFAULT \'DRAFT\'', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_fin_at := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scores' AND COLUMN_NAME = 'finalized_at');
SET @sql := IF(@col_fin_at = 0, 'ALTER TABLE scores ADD COLUMN finalized_at DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_fin_by := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scores' AND COLUMN_NAME = 'finalized_by');
SET @sql := IF(@col_fin_by = 0, 'ALTER TABLE scores ADD COLUMN finalized_by INT NULL', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP PROCEDURE IF EXISTS sp_register_student_course;

DELIMITER $$
CREATE PROCEDURE sp_register_student_course(
    IN p_student_id INT,
    IN p_course_id  INT,
    IN p_acad_year  VARCHAR(10),
    IN p_semester   INT
)
BEGIN
    DECLARE v_count INT;
    DECLARE v_already_registered INT;
    DECLARE v_term_status VARCHAR(20);
    DECLARE v_reg_deadline DATE;

    -- 1. Check duplicate enrollment rule in database
    SELECT COUNT(*) INTO v_already_registered
    FROM student_courses
    WHERE student_id = p_student_id
      AND course_id = p_course_id
      AND academic_year = p_acad_year
      AND semester = p_semester;

    IF v_already_registered > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Duplicate enrollment: Student is already registered for this course in this academic period.';
    END IF;

    -- 2. Check academic term lifecycle and registration deadline
    SELECT status, registration_deadline 
    INTO v_term_status, v_reg_deadline
    FROM academic_terms
    WHERE academic_year = p_acad_year AND semester = p_semester;

    IF v_term_status IS NOT NULL THEN
        IF v_term_status = 'CLOSED' OR v_term_status = 'ARCHIVED' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Academic term is closed. Standard enrollment is not permitted. Authorized override required.';
        END IF;

        IF CURDATE() > v_reg_deadline THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Registration deadline has passed. Late registration requires administrative authorization.';
        END IF;
    END IF;

    -- 3. Check 5 courses maximum limit
    SELECT COUNT(*) INTO v_count
    FROM student_courses
    WHERE student_id = p_student_id
      AND academic_year = p_acad_year
      AND semester = p_semester;

    IF v_count >= 5 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Student cannot register more than 5 courses per semester.';
    ELSE
        INSERT INTO student_courses (student_id, course_id, academic_year, semester, registration_date)
        VALUES (p_student_id, p_course_id, p_acad_year, p_semester, CURDATE());
    END IF;
END$$

DROP PROCEDURE IF EXISTS sp_register_student_course_exception$$

CREATE PROCEDURE sp_register_student_course_exception(
    IN p_student_id     INT,
    IN p_course_id      INT,
    IN p_acad_year      VARCHAR(10),
    IN p_semester       INT,
    IN p_authorized_by  INT,
    IN p_auth_username  VARCHAR(50),
    IN p_reason         TEXT
)
BEGIN
    DECLARE v_count INT;
    DECLARE v_already_registered INT;
    DECLARE v_new_sc_id INT;

    -- 1. Check duplicate enrollment rule
    SELECT COUNT(*) INTO v_already_registered
    FROM student_courses
    WHERE student_id = p_student_id
      AND course_id = p_course_id
      AND academic_year = p_acad_year
      AND semester = p_semester;

    IF v_already_registered > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Duplicate enrollment: Student is already registered for this course in this academic period.';
    END IF;

    -- 2. Check 5-course limit
    SELECT COUNT(*) INTO v_count
    FROM student_courses
    WHERE student_id = p_student_id
      AND academic_year = p_acad_year
      AND semester = p_semester;

    IF v_count >= 5 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Student cannot register more than 5 courses per semester.';
    END IF;

    -- 3. Insert exception registration
    INSERT INTO student_courses (student_id, course_id, academic_year, semester, registration_date)
    VALUES (p_student_id, p_course_id, p_acad_year, p_semester, CURDATE());
    SET v_new_sc_id = LAST_INSERT_ID();

    -- 4. Audit trail logging (Preserve reason, user, time)
    INSERT INTO academic_audit_log (
        entity_type, entity_id, student_id, course_id, academic_year, semester,
        action, previous_value, new_value, reason, authorized_by, authorized_user
    ) VALUES (
        'ENROLLMENT', v_new_sc_id, p_student_id, p_course_id, p_acad_year, p_semester,
        'AUTHORIZED_LATE_REGISTRATION', NULL, CONCAT('Enrolled course_id=', p_course_id),
        p_reason, p_authorized_by, p_auth_username
    );
END$$
DELIMITER ;
