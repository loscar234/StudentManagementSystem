-- ============================================================
-- Student Management System - Database Schema
-- Author: Senior Software Engineer
-- Database: MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS student_management_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE student_management_system;

-- ============================================================
-- TABLE: persons (base entity for students and lecturers)
-- ============================================================
CREATE TABLE IF NOT EXISTS persons (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(150) UNIQUE NOT NULL,
    phone       VARCHAR(20),
    gender      ENUM('Male', 'Female', 'Other') NOT NULL,
    date_of_birth DATE,
    address     VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: students
-- ============================================================
CREATE TABLE IF NOT EXISTS students (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    person_id       INT NOT NULL,
    reg_number      VARCHAR(30) UNIQUE NOT NULL,
    programme       VARCHAR(150) NOT NULL,
    year_of_study   INT NOT NULL DEFAULT 1 CHECK (year_of_study BETWEEN 1 AND 6),
    semester        INT NOT NULL DEFAULT 1 CHECK (semester BETWEEN 1 AND 2),
    enrollment_date DATE NOT NULL,
    status          ENUM('Active', 'Suspended', 'Graduated', 'Deferred') DEFAULT 'Active',
    CONSTRAINT fk_student_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: lecturers
-- ============================================================
CREATE TABLE IF NOT EXISTS lecturers (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    person_id       INT NOT NULL,
    staff_id        VARCHAR(30) UNIQUE NOT NULL,
    department      VARCHAR(150) NOT NULL,
    qualification   VARCHAR(100),
    specialization  VARCHAR(150),
    hire_date       DATE NOT NULL,
    status          ENUM('Active', 'On Leave', 'Retired') DEFAULT 'Active',
    CONSTRAINT fk_lecturer_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: courses
-- ============================================================
CREATE TABLE IF NOT EXISTS courses (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    course_code     VARCHAR(20) UNIQUE NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    credit_hours    INT NOT NULL DEFAULT 3 CHECK (credit_hours BETWEEN 1 AND 6),
    department      VARCHAR(150),
    level           INT NOT NULL DEFAULT 100 COMMENT 'e.g. 100, 200, 300, 400',
    semester        INT NOT NULL DEFAULT 1 CHECK (semester BETWEEN 1 AND 2),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: lecturer_courses (many-to-many: lecturers <-> courses)
-- ============================================================
CREATE TABLE IF NOT EXISTS lecturer_courses (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    lecturer_id     INT NOT NULL,
    course_id       INT NOT NULL,
    academic_year   VARCHAR(10) NOT NULL COMMENT 'e.g. 2024/2025',
    semester        INT NOT NULL DEFAULT 1,
    assigned_date   DATE NOT NULL,
    UNIQUE KEY uq_lecturer_course_year (lecturer_id, course_id, academic_year, semester),
    CONSTRAINT fk_lc_lecturer FOREIGN KEY (lecturer_id) REFERENCES lecturers(id) ON DELETE CASCADE,
    CONSTRAINT fk_lc_course   FOREIGN KEY (course_id)   REFERENCES courses(id)   ON DELETE CASCADE
);

-- ============================================================
-- TABLE: student_courses (many-to-many: students <-> courses)
-- A student registers exactly 5 courses per semester
-- ============================================================
CREATE TABLE IF NOT EXISTS student_courses (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT NOT NULL,
    course_id       INT NOT NULL,
    academic_year   VARCHAR(10) NOT NULL,
    semester        INT NOT NULL DEFAULT 1,
    registration_date DATE NOT NULL,
    UNIQUE KEY uq_student_course_year (student_id, course_id, academic_year, semester),
    CONSTRAINT fk_sc_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_sc_course  FOREIGN KEY (course_id)  REFERENCES courses(id)  ON DELETE CASCADE
);

-- ============================================================
-- TABLE: scores
-- CAT max = 30, Exam max = 70, Total max = 100
-- ============================================================
CREATE TABLE IF NOT EXISTS scores (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT NOT NULL,
    course_id       INT NOT NULL,
    academic_year   VARCHAR(10) NOT NULL,
    semester        INT NOT NULL DEFAULT 1,
    cat_score       DECIMAL(5,2) DEFAULT 0.00 CHECK (cat_score >= 0 AND cat_score <= 30),
    exam_score      DECIMAL(5,2) DEFAULT 0.00 CHECK (exam_score >= 0 AND exam_score <= 70),
    total_score     DECIMAL(5,2) GENERATED ALWAYS AS (cat_score + exam_score) STORED,
    grade           VARCHAR(5) GENERATED ALWAYS AS (
                        CASE
                            WHEN (cat_score + exam_score) >= 70 THEN 'A'
                            WHEN (cat_score + exam_score) >= 60 THEN 'B'
                            WHEN (cat_score + exam_score) >= 50 THEN 'C'
                            WHEN (cat_score + exam_score) >= 40 THEN 'D'
                            ELSE 'F'
                        END
                    ) STORED,
    remarks         VARCHAR(200),
    entered_by      INT COMMENT 'lecturer_id who entered the score',
    entered_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_score (student_id, course_id, academic_year, semester),
    CONSTRAINT fk_score_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_score_course  FOREIGN KEY (course_id)  REFERENCES courses(id)  ON DELETE CASCADE,
    CONSTRAINT fk_score_lecturer FOREIGN KEY (entered_by) REFERENCES lecturers(id) ON DELETE SET NULL
);

-- ============================================================
-- TABLE: books
-- ============================================================
CREATE TABLE IF NOT EXISTS books (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    isbn            VARCHAR(20) UNIQUE NOT NULL,
    title           VARCHAR(255) NOT NULL,
    author          VARCHAR(200) NOT NULL,
    edition         VARCHAR(20),
    version         VARCHAR(20),
    year_published  YEAR NOT NULL,
    publisher       VARCHAR(150),
    category        VARCHAR(100),
    total_copies    INT NOT NULL DEFAULT 1 CHECK (total_copies >= 0),
    available_copies INT NOT NULL DEFAULT 1 CHECK (available_copies >= 0),
    shelf_location  VARCHAR(50),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: borrowing_records
-- ============================================================
CREATE TABLE IF NOT EXISTS borrowing_records (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT NOT NULL,
    book_id         INT NOT NULL,
    borrow_date     DATE NOT NULL,
    due_date        DATE NOT NULL,
    return_date     DATE,
    status          ENUM('Borrowed', 'Returned', 'Overdue', 'Reserved') DEFAULT 'Borrowed',
    fine_amount     DECIMAL(8,2) DEFAULT 0.00 COMMENT 'Fine for overdue books',
    notes           VARCHAR(255),
    CONSTRAINT fk_borrow_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_borrow_book    FOREIGN KEY (book_id)    REFERENCES books(id)    ON DELETE CASCADE
);

-- ============================================================
-- TABLE: reservations
-- ============================================================
CREATE TABLE IF NOT EXISTS reservations (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT NOT NULL,
    book_id         INT NOT NULL,
    reservation_date DATE NOT NULL,
    expiry_date     DATE NOT NULL,
    status          ENUM('Active', 'Fulfilled', 'Cancelled', 'Expired') DEFAULT 'Active',
    CONSTRAINT fk_res_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_res_book    FOREIGN KEY (book_id)    REFERENCES books(id)    ON DELETE CASCADE
);

-- ============================================================
-- INDEXES for performance
-- ============================================================
CREATE INDEX idx_student_reg    ON students(reg_number);
CREATE INDEX idx_lecturer_staff ON lecturers(staff_id);
CREATE INDEX idx_course_code    ON courses(course_code);
CREATE INDEX idx_book_title     ON books(title);
CREATE INDEX idx_book_isbn      ON books(isbn);
CREATE INDEX idx_borrow_status  ON borrowing_records(status);
CREATE INDEX idx_borrow_due     ON borrowing_records(due_date);

-- ============================================================
-- VIEWS
-- ============================================================

-- View: Student result slip
CREATE OR REPLACE VIEW v_student_result_slip AS
SELECT
    s.reg_number,
    CONCAT(p.first_name, ' ', p.last_name) AS student_name,
    s.programme,
    c.course_code,
    c.title AS course_title,
    sc2.cat_score,
    sc2.exam_score,
    sc2.total_score,
    sc2.grade,
    sc2.academic_year,
    sc2.semester
FROM students s
JOIN persons p ON s.person_id = p.id
JOIN scores sc2 ON sc2.student_id = s.id
JOIN courses c  ON c.id = sc2.course_id
ORDER BY s.reg_number, c.course_code;

-- View: Library overview
CREATE OR REPLACE VIEW v_library_overview AS
SELECT
    b.id,
    b.isbn,
    b.title,
    b.author,
    b.edition,
    b.year_published,
    b.total_copies,
    b.available_copies,
    (b.total_copies - b.available_copies) AS borrowed_copies,
    COUNT(CASE WHEN br.status = 'Overdue' THEN 1 END) AS overdue_count
FROM books b
LEFT JOIN borrowing_records br ON br.book_id = b.id AND br.status IN ('Borrowed','Overdue')
GROUP BY b.id;

-- View: Overdue books detail
CREATE OR REPLACE VIEW v_overdue_books AS
SELECT
    br.id AS record_id,
    CONCAT(p.first_name, ' ', p.last_name) AS student_name,
    s.reg_number,
    b.title AS book_title,
    b.isbn,
    br.borrow_date,
    br.due_date,
    DATEDIFF(CURDATE(), br.due_date) AS days_overdue,
    (DATEDIFF(CURDATE(), br.due_date) * 5.00) AS fine_amount
FROM borrowing_records br
JOIN students s  ON s.id  = br.student_id
JOIN persons  p  ON p.id  = s.person_id
JOIN books    b  ON b.id  = br.book_id
WHERE br.due_date < CURDATE() AND br.return_date IS NULL;

-- ============================================================
-- SAMPLE DATA
-- ============================================================

-- Persons
INSERT INTO persons (first_name, last_name, email, phone, gender, date_of_birth, address) VALUES
('Alice',   'Mwangi',   'alice.mwangi@university.ac.ke',   '0712345678', 'Female', '2002-03-15', 'Nairobi'),
('Bob',     'Ochieng',  'bob.ochieng@university.ac.ke',    '0723456789', 'Male',   '2001-07-22', 'Kisumu'),
('Carol',   'Wanjiku',  'carol.wanjiku@university.ac.ke',  '0734567890', 'Female', '2003-01-10', 'Nakuru'),
('David',   'Kamau',    'david.kamau@university.ac.ke',    '0745678901', 'Male',   '2000-11-05', 'Mombasa'),
('Eve',     'Njoroge',  'eve.njoroge@university.ac.ke',    '0756789012', 'Female', '2002-06-18', 'Eldoret'),
('Frank',   'Otieno',   'frank.otieno@university.ac.ke',   '0767890123', 'Male',   '1980-04-12', 'Nairobi'),
('Grace',   'Akinyi',   'grace.akinyi@university.ac.ke',   '0778901234', 'Female', '1975-09-30', 'Kisumu'),
('Henry',   'Mutua',    'henry.mutua@university.ac.ke',    '0789012345', 'Male',   '1982-12-01', 'Nairobi');

-- Students
INSERT INTO students (person_id, reg_number, programme, year_of_study, semester, enrollment_date) VALUES
(1, 'CS/001/2023', 'Bachelor of Science in Computer Science', 2, 1, '2023-09-01'),
(2, 'CS/002/2023', 'Bachelor of Science in Computer Science', 2, 1, '2023-09-01'),
(3, 'IT/001/2023', 'Bachelor of Science in Information Technology', 2, 1, '2023-09-01'),
(4, 'SE/001/2022', 'Bachelor of Science in Software Engineering', 3, 1, '2022-09-01'),
(5, 'CS/003/2024', 'Bachelor of Science in Computer Science', 1, 1, '2024-09-01');

-- Lecturers
INSERT INTO lecturers (person_id, staff_id, department, qualification, specialization, hire_date) VALUES
(6, 'STAFF/001', 'Computer Science', 'PhD Computer Science', 'Algorithms & Data Structures', '2010-01-15'),
(7, 'STAFF/002', 'Information Technology', 'MSc Information Systems', 'Database Systems', '2012-03-01'),
(8, 'STAFF/003', 'Software Engineering', 'PhD Software Engineering', 'Software Architecture', '2008-07-20');

-- Courses
INSERT INTO courses (course_code, title, description, credit_hours, department, level, semester) VALUES
('CS101', 'Introduction to Programming',       'Fundamentals of programming using Java',          3, 'Computer Science',       100, 1),
('CS201', 'Data Structures & Algorithms',      'Core data structures and algorithm analysis',     3, 'Computer Science',       200, 1),
('CS301', 'Database Systems',                  'Relational databases, SQL, and normalization',    3, 'Computer Science',       300, 1),
('CS302', 'Operating Systems',                 'Process management, memory, file systems',        3, 'Computer Science',       300, 1),
('CS401', 'Software Engineering',              'SDLC, design patterns, project management',       3, 'Software Engineering',   400, 1),
('IT201', 'Networking Fundamentals',           'OSI model, TCP/IP, network configuration',        3, 'Information Technology', 200, 1),
('IT301', 'Web Development',                   'HTML, CSS, JavaScript, and frameworks',           3, 'Information Technology', 300, 1),
('SE201', 'Object-Oriented Programming',       'OOP concepts with Java',                          3, 'Software Engineering',   200, 1),
('SE301', 'Software Testing & QA',             'Testing methodologies and quality assurance',     3, 'Software Engineering',   300, 1),
('CS202', 'Discrete Mathematics',              'Logic, sets, graphs, and combinatorics',          3, 'Computer Science',       200, 1);

-- Lecturer course allocations
INSERT INTO lecturer_courses (lecturer_id, course_id, academic_year, semester, assigned_date) VALUES
(1, 1, '2024/2025', 1, '2024-09-01'),
(1, 2, '2024/2025', 1, '2024-09-01'),
(1, 10,'2024/2025', 1, '2024-09-01'),
(2, 3, '2024/2025', 1, '2024-09-01'),
(2, 6, '2024/2025', 1, '2024-09-01'),
(2, 7, '2024/2025', 1, '2024-09-01'),
(3, 4, '2024/2025', 1, '2024-09-01'),
(3, 5, '2024/2025', 1, '2024-09-01'),
(3, 8, '2024/2025', 1, '2024-09-01'),
(3, 9, '2024/2025', 1, '2024-09-01');

-- Student course registrations (5 courses each)
INSERT INTO student_courses (student_id, course_id, academic_year, semester, registration_date) VALUES
(1,1,'2024/2025',1,'2024-09-05'),(1,2,'2024/2025',1,'2024-09-05'),(1,3,'2024/2025',1,'2024-09-05'),
(1,4,'2024/2025',1,'2024-09-05'),(1,5,'2024/2025',1,'2024-09-05'),
(2,1,'2024/2025',1,'2024-09-05'),(2,2,'2024/2025',1,'2024-09-05'),(2,6,'2024/2025',1,'2024-09-05'),
(2,7,'2024/2025',1,'2024-09-05'),(2,8,'2024/2025',1,'2024-09-05'),
(3,1,'2024/2025',1,'2024-09-05'),(3,3,'2024/2025',1,'2024-09-05'),(3,6,'2024/2025',1,'2024-09-05'),
(3,7,'2024/2025',1,'2024-09-05'),(3,9,'2024/2025',1,'2024-09-05'),
(4,2,'2024/2025',1,'2024-09-05'),(4,3,'2024/2025',1,'2024-09-05'),(4,4,'2024/2025',1,'2024-09-05'),
(4,5,'2024/2025',1,'2024-09-05'),(4,8,'2024/2025',1,'2024-09-05'),
(5,1,'2024/2025',1,'2024-09-05'),(5,2,'2024/2025',1,'2024-09-05'),(5,6,'2024/2025',1,'2024-09-05'),
(5,9,'2024/2025',1,'2024-09-05'),(5,10,'2024/2025',1,'2024-09-05');

-- Scores
INSERT INTO scores (student_id, course_id, academic_year, semester, cat_score, exam_score, entered_by) VALUES
(1,1,'2024/2025',1,25.00,58.00,1),(1,2,'2024/2025',1,22.00,52.00,1),(1,3,'2024/2025',1,28.00,60.00,2),
(1,4,'2024/2025',1,20.00,45.00,3),(1,5,'2024/2025',1,26.00,55.00,3),
(2,1,'2024/2025',1,18.00,40.00,1),(2,2,'2024/2025',1,15.00,35.00,1),(2,6,'2024/2025',1,24.00,50.00,2),
(2,7,'2024/2025',1,20.00,48.00,2),(2,8,'2024/2025',1,22.00,55.00,3),
(3,1,'2024/2025',1,27.00,62.00,1),(3,3,'2024/2025',1,25.00,58.00,2),(3,6,'2024/2025',1,28.00,65.00,2),
(3,7,'2024/2025',1,23.00,54.00,2),(3,9,'2024/2025',1,26.00,60.00,3);

-- Books
INSERT INTO books (isbn, title, author, edition, version, year_published, publisher, category, total_copies, available_copies, shelf_location) VALUES
('978-0-13-468599-1', 'Effective Java',                         'Joshua Bloch',          '3rd', '3.0', 2018, 'Addison-Wesley', 'Programming',     5, 3, 'A1-01'),
('978-0-596-51774-8', 'Head First Java',                        'Kathy Sierra',          '2nd', '2.0', 2005, 'O\'Reilly',       'Programming',     4, 2, 'A1-02'),
('978-0-13-235088-4', 'The Pragmatic Programmer',               'David Thomas',          '2nd', '2.0', 2019, 'Addison-Wesley', 'Software Eng',    3, 1, 'B2-01'),
('978-0-13-110362-7', 'The C Programming Language',             'Brian Kernighan',       '2nd', '2.0', 1988, 'Prentice Hall',  'Programming',     6, 6, 'A2-01'),
('978-0-201-63361-0', 'Design Patterns',                        'Gang of Four',          '1st', '1.0', 1994, 'Addison-Wesley', 'Software Eng',    4, 2, 'B2-02'),
('978-0-13-597444-5', 'Introduction to Algorithms',             'Cormen et al.',         '4th', '4.0', 2022, 'MIT Press',      'Algorithms',      5, 4, 'C1-01'),
('978-0-07-352332-3', 'Database System Concepts',               'Silberschatz et al.',   '7th', '7.0', 2019, 'McGraw-Hill',    'Databases',       4, 3, 'D1-01'),
('978-0-13-603580-5', 'Computer Networks',                      'Andrew Tanenbaum',      '5th', '5.0', 2010, 'Prentice Hall',  'Networking',      3, 1, 'E1-01'),
('978-1-491-91205-8', 'Learning Python',                        'Mark Lutz',             '5th', '5.0', 2013, 'O\'Reilly',       'Programming',     4, 4, 'A1-03'),
('978-0-13-235088-5', 'Clean Code',                             'Robert C. Martin',      '1st', '1.0', 2008, 'Prentice Hall',  'Software Eng',    5, 3, 'B2-03');

-- Borrowing records
INSERT INTO borrowing_records (student_id, book_id, borrow_date, due_date, return_date, status) VALUES
(1, 1, '2025-01-10', '2025-01-24', NULL,         'Borrowed'),
(1, 2, '2025-01-05', '2025-01-19', '2025-01-18', 'Returned'),
(2, 3, '2025-01-08', '2025-01-22', NULL,         'Overdue'),
(2, 5, '2025-01-12', '2025-01-26', NULL,         'Borrowed'),
(3, 6, '2025-01-15', '2025-01-29', NULL,         'Borrowed'),
(4, 7, '2024-12-20', '2025-01-03', NULL,         'Overdue'),
(5, 8, '2025-01-18', '2025-02-01', NULL,         'Borrowed');

-- ============================================================
-- STORED PROCEDURES
-- ============================================================

DELIMITER $$

-- Procedure: Register student for courses (enforces max 5)
CREATE PROCEDURE sp_register_student_course(
    IN p_student_id INT,
    IN p_course_id  INT,
    IN p_acad_year  VARCHAR(10),
    IN p_semester   INT
)
BEGIN
    DECLARE v_count INT;
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

-- Procedure: Borrow a book
CREATE PROCEDURE sp_borrow_book(
    IN p_student_id INT,
    IN p_book_id    INT
)
BEGIN
    DECLARE v_available INT;
    SELECT available_copies INTO v_available FROM books WHERE id = p_book_id;

    IF v_available <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No available copies. Please reserve the book.';
    ELSE
        INSERT INTO borrowing_records (student_id, book_id, borrow_date, due_date, status)
        VALUES (p_student_id, p_book_id, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'Borrowed');
        UPDATE books SET available_copies = available_copies - 1 WHERE id = p_book_id;
    END IF;
END$$

-- Procedure: Return a book
CREATE PROCEDURE sp_return_book(
    IN p_record_id INT
)
BEGIN
    DECLARE v_book_id INT;
    DECLARE v_due_date DATE;
    DECLARE v_return_date DATE;
    DECLARE v_status VARCHAR(20);

    SELECT book_id, due_date, return_date, status
    INTO v_book_id, v_due_date, v_return_date, v_status
    FROM borrowing_records WHERE id = p_record_id;

    -- Only proceed if record exists and has not already been returned
    IF v_book_id IS NOT NULL AND v_return_date IS NULL AND v_status != 'Returned' THEN
        UPDATE borrowing_records
        SET return_date = CURDATE(),
            status = 'Returned',
            fine_amount = IF(CURDATE() > v_due_date, DATEDIFF(CURDATE(), v_due_date) * 5.00, 0.00)
        WHERE id = p_record_id;

        UPDATE books SET available_copies = available_copies + 1 WHERE id = v_book_id;
    END IF;
END$$

DELIMITER ;

-- ============================================================
-- USEFUL CRUD QUERIES
-- ============================================================

-- [SELECT] Get all students with full details
-- SELECT s.reg_number, CONCAT(p.first_name,' ',p.last_name) AS name,
--        p.email, s.programme, s.year_of_study
-- FROM students s JOIN persons p ON s.person_id = p.id;

-- [SELECT] Get result slip for a student
-- SELECT * FROM v_student_result_slip WHERE reg_number = 'CS/001/2023';

-- [SELECT] Search course by code or title
-- SELECT * FROM courses WHERE course_code LIKE '%CS%' OR title LIKE '%Data%';

-- [SELECT] Get lecturer's assigned courses
-- SELECT l.staff_id, CONCAT(p.first_name,' ',p.last_name) AS lecturer,
--        c.course_code, c.title, lc.academic_year
-- FROM lecturer_courses lc
-- JOIN lecturers l ON l.id = lc.lecturer_id
-- JOIN persons p   ON p.id = l.person_id
-- JOIN courses c   ON c.id = lc.course_id
-- WHERE l.staff_id = 'STAFF/001';

-- [SELECT] Real-time book search by title
-- SELECT * FROM v_library_overview WHERE title LIKE '%Java%';

-- [SELECT] All overdue books
-- SELECT * FROM v_overdue_books;

-- [UPDATE] Update student score
-- UPDATE scores SET cat_score = 25.00, exam_score = 60.00
-- WHERE student_id = 1 AND course_id = 1 AND academic_year = '2024/2025';

-- [DELETE] Remove student course registration
-- DELETE FROM student_courses WHERE student_id = 1 AND course_id = 1;
