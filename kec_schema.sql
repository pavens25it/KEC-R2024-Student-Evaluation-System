-- ============================================================================
-- Kongu Engineering College (Autonomous), Perundurai, Erode - 638 060
-- Regulation 2024 (R2024) Student Academic & Continuous Evaluation System
-- Database Schema Definition: kec_schema.sql
-- ============================================================================

CREATE DATABASE IF NOT EXISTS kec_eval_db;
USE kec_eval_db;

-- ----------------------------------------------------------------------------
-- Drop existing tables in reverse dependency order for clean re-initialization
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS assessment_marks;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS students;

-- ----------------------------------------------------------------------------
-- Table 1: students
-- Stores fundamental student demographic and academic profile details.
-- ----------------------------------------------------------------------------
CREATE TABLE students (
    roll_no VARCHAR(15) NOT NULL,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(60) NOT NULL,
    current_semester INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_students PRIMARY KEY (roll_no),
    CONSTRAINT chk_student_semester CHECK (current_semester BETWEEN 1 AND 8)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- Table 2: courses
-- Stores curriculum course catalog details under KEC R2024 regulation.
-- Course Types:
--   - THEORY    : 40% CIA, 60% ESE (Min ESE Contribution: 27/60)
--   - PRACTICAL : 60% CIA, 40% ESE (Min ESE Contribution: 20/40)
--   - EMBEDDED  : 50% CIA, 50% ESE (Min ESE Contribution: 22.5/50)
-- ----------------------------------------------------------------------------
CREATE TABLE courses (
    course_code VARCHAR(15) NOT NULL,
    title VARCHAR(150) NOT NULL,
    credits INT NOT NULL,
    course_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_courses PRIMARY KEY (course_code),
    CONSTRAINT chk_course_credits CHECK (credits > 0 AND credits <= 10),
    CONSTRAINT chk_course_type CHECK (course_type IN ('THEORY', 'PRACTICAL', 'EMBEDDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- Table 3: enrollments
-- Maps students to enrolled courses for a given semester, tracking attendance
-- and medical condonation approval eligibility.
-- Attendance Eligibility Rules (R2024):
--   - Attendance >= 75%               : Eligible for End Semester Exam (ESE)
--   - 65% <= Attendance < 75%         : Conditionally eligible (Requires Medical Condonation approval)
--   - Attendance < 65%                : SA (Shortage of Attendance) -> Barred from ESE
-- ----------------------------------------------------------------------------
CREATE TABLE enrollments (
    enrollment_id INT AUTO_INCREMENT NOT NULL,
    student_roll_no VARCHAR(15) NOT NULL,
    course_code VARCHAR(15) NOT NULL,
    semester INT NOT NULL,
    classes_held INT NOT NULL DEFAULT 0,
    classes_attended INT NOT NULL DEFAULT 0,
    medical_condonation BOOLEAN NOT NULL DEFAULT FALSE,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_enrollments PRIMARY KEY (enrollment_id),
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_roll_no)
        REFERENCES students(roll_no) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_code)
        REFERENCES courses(course_code) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_student_course_sem UNIQUE (student_roll_no, course_code, semester),
    CONSTRAINT chk_enrollment_sem CHECK (semester BETWEEN 1 AND 8),
    CONSTRAINT chk_classes_held CHECK (classes_held >= 0),
    CONSTRAINT chk_classes_attended CHECK (classes_attended >= 0),
    CONSTRAINT chk_attendance_validity CHECK (classes_attended <= classes_held)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- Table 4: assessment_marks
-- Supports phased, incremental assessment entries:
--   Phase 1: CIA 1 and CIA 2 (scale 0-100)
--   Phase 2: Assignment / Quiz / Lab Evaluation (scale 0-100)
--   Phase 3: ESE Marks (scale 0-100) OR ESE Absence (is_absent_ese = TRUE)
-- Missing marks remain NULL (PENDING status) rather than defaulting to zero.
-- ----------------------------------------------------------------------------
CREATE TABLE assessment_marks (
    mark_id INT AUTO_INCREMENT NOT NULL,
    enrollment_id INT NOT NULL,
    cia1 DECIMAL(5,2) DEFAULT NULL,
    cia2 DECIMAL(5,2) DEFAULT NULL,
    assignment_quiz_lab DECIMAL(5,2) DEFAULT NULL,
    ese DECIMAL(5,2) DEFAULT NULL,
    is_absent_ese BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_assessment_marks PRIMARY KEY (mark_id),
    CONSTRAINT uq_assessment_enrollment UNIQUE (enrollment_id),
    CONSTRAINT fk_assessment_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES enrollments(enrollment_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_cia1 CHECK (cia1 IS NULL OR (cia1 >= 0.00 AND cia1 <= 100.00)),
    CONSTRAINT chk_cia2 CHECK (cia2 IS NULL OR (cia2 >= 0.00 AND cia2 <= 100.00)),
    CONSTRAINT chk_assignment CHECK (assignment_quiz_lab IS NULL OR (assignment_quiz_lab >= 0.00 AND assignment_quiz_lab <= 100.00)),
    CONSTRAINT chk_ese CHECK (ese IS NULL OR (ese >= 0.00 AND ese <= 100.00))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- Indexes for query optimization
-- ----------------------------------------------------------------------------
CREATE INDEX idx_student_dept ON students(department);
CREATE INDEX idx_enrollment_student ON enrollments(student_roll_no);
CREATE INDEX idx_enrollment_course ON enrollments(course_code);
CREATE INDEX idx_enrollment_sem ON enrollments(semester);

-- ============================================================================
-- Seed Sample Data for Testing & Practical Viva Demonstrations
-- ============================================================================

-- 1. Insert Sample Students (Information Technology & Computer Science)
INSERT INTO students (roll_no, name, department, current_semester) VALUES
('24ITR001', 'Aarav Kumar S', 'Information Technology', 3),
('24ITR015', 'Bavithra M', 'Information Technology', 3),
('24ITR045', 'Dinesh Karthik K', 'Information Technology', 3),
('24ITR088', 'Elango V', 'Information Technology', 3),
('24CSR010', 'Harini P', 'Computer Science and Engineering', 3),
('24CSR025', 'Jeeva Anand R', 'Computer Science and Engineering', 3);

-- 2. Insert Sample Courses (Covering THEORY, PRACTICAL, EMBEDDED)
INSERT INTO courses (course_code, title, credits, course_type) VALUES
('24ITT31', 'Data Structures and Algorithms', 3, 'THEORY'),
('24ITC31', 'Object Oriented Programming with Java', 4, 'EMBEDDED'),
('24ITP31', 'Data Structures and Java Laboratory', 2, 'PRACTICAL'),
('24ITT32', 'Database Management Systems', 3, 'THEORY'),
('24CST31', 'Operating Systems and Systems Programming', 3, 'THEORY');

-- 3. Insert Enrollments with diverse Attendance Profiles
-- (a) 24ITR001: Excellent attendance (>= 75%)
INSERT INTO enrollments (enrollment_id, student_roll_no, course_code, semester, classes_held, classes_attended, medical_condonation) VALUES
(1, '24ITR001', '24ITT31', 3, 50, 46, FALSE), -- 92% (Eligible)
(2, '24ITR001', '24ITC31', 3, 60, 54, FALSE), -- 90% (Eligible)
(3, '24ITR001', '24ITP31', 3, 30, 28, FALSE); -- 93.3% (Eligible)

-- (b) 24ITR015: Attendance in 65%-74% range with Medical Condonation Approved
INSERT INTO enrollments (enrollment_id, student_roll_no, course_code, semester, classes_held, classes_attended, medical_condonation) VALUES
(4, '24ITR015', '24ITT31', 3, 50, 35, TRUE),  -- 70% (Eligible via Condonation)
(5, '24ITR015', '24ITC31', 3, 60, 48, FALSE); -- 80% (Eligible)

-- (c) 24ITR045: Attendance in 65%-74% range WITHOUT Condonation -> Barred (SA)
INSERT INTO enrollments (enrollment_id, student_roll_no, course_code, semester, classes_held, classes_attended, medical_condonation) VALUES
(6, '24ITR045', '24ITT31', 3, 50, 34, FALSE), -- 68% (No Condonation -> SA)
(7, '24ITR045', '24ITC31', 3, 60, 50, FALSE); -- 83.3% (Eligible)

-- (d) 24ITR088: Defaulter with < 65% attendance -> Barred (SA)
INSERT INTO enrollments (enrollment_id, student_roll_no, course_code, semester, classes_held, classes_attended, medical_condonation) VALUES
(8, '24ITR088', '24ITT31', 3, 50, 28, FALSE); -- 56% (Defaulter -> SA)

-- (e) 24CSR010 & 24CSR025: CSE department students
INSERT INTO enrollments (enrollment_id, student_roll_no, course_code, semester, classes_held, classes_attended, medical_condonation) VALUES
(9,  '24CSR010', '24CST31', 3, 45, 42, FALSE), -- 93.3% (Eligible)
(10, '24CSR025', '24CST31', 3, 45, 40, FALSE); -- 88.9% (Eligible)

-- 4. Insert Phased Assessment Marks showcasing all evaluation outcomes
-- Enrollment 1: 24ITR001 - 24ITT31 (THEORY: CIA 40%, ESE 60%, Min ESE 27)
-- CIA: 92, 90, Assignment: 95 => CIA out of 100 = 91.8 => CIA contribution (40%) = 36.72
-- ESE: 88 => ESE contribution (60%) = 52.80 (>= 27)
-- Total = 89.52 => Grade A+
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_quiz_lab, ese, is_absent_ese) VALUES
(1, 92.00, 90.00, 95.00, 88.00, FALSE);

-- Enrollment 2: 24ITR001 - 24ITC31 (EMBEDDED: CIA 50%, ESE 50%, Min ESE 22.5)
-- CIA: 95, 94, Assignment: 96 => CIA out of 100 = 94.8 => CIA contribution (50%) = 47.4
-- ESE: 92 => ESE contribution (50%) = 46.0 (>= 22.5)
-- Total = 93.4 => Grade O
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_quiz_lab, ese, is_absent_ese) VALUES
(2, 95.00, 94.00, 96.00, 92.00, FALSE);

-- Enrollment 3: 24ITR001 - 24ITP31 (PRACTICAL: CIA 60%, ESE 40%, Min ESE 20)
-- CIA: 85, 88, Lab: 90 => CIA out of 100 = 87.2 => CIA contribution (60%) = 52.32
-- ESE: 85 => ESE contribution (40%) = 34.0 (>= 20)
-- Total = 86.32 => Grade A+
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_quiz_lab, ese, is_absent_ese) VALUES
(3, 85.00, 88.00, 90.00, 85.00, FALSE);

-- Enrollment 4: 24ITR015 - 24ITT31 (THEORY, Condonation approved)
-- CIA: 75, 70, Assignment: 72 => CIA out of 100 = 72.4 => CIA contribution (40%) = 28.96
-- ESE: 65 => ESE contribution (60%) = 39.0 (>= 27)
-- Total = 67.96 => Grade B+
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_quiz_lab, ese, is_absent_ese) VALUES
(4, 75.00, 70.00, 72.00, 65.00, FALSE);

-- Enrollment 5: 24ITR015 - 24ITC31 (EMBEDDED, ESE PENDING)
-- Phase 1 & 2 entered, but ESE is NULL (Pending)
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_quiz_lab, ese, is_absent_ese) VALUES
(5, 80.00, 78.00, 85.00, NULL, FALSE);

-- Enrollment 6: 24ITR045 - 24ITT31 (Attendance 68%, no condonation -> SA)
-- Even with marks, attendance shortage results in SA
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_quiz_lab, ese, is_absent_ese) VALUES
(6, 60.00, 65.00, 70.00, NULL, FALSE);

-- Enrollment 7: 24ITR045 - 24ITC31 (Eligible, but Absent in ESE -> AB)
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_quiz_lab, ese, is_absent_ese) VALUES
(7, 72.00, 68.00, 75.00, NULL, TRUE);

-- Enrollment 8: 24ITR088 - 24ITT31 (Attendance 56% < 65% -> Defaulter SA)
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_quiz_lab, ese, is_absent_ese) VALUES
(8, 45.00, 50.00, 55.00, NULL, FALSE);

-- Enrollment 9: 24CSR010 - 24CST31 (THEORY: Passed CIA, Failed ESE minimum < 27 -> RA)
-- CIA: 80, 82, Assignment: 80 => CIA (40%) = 32.32
-- ESE: 40 => ESE contribution (60%) = 24.0 (< 27 minimum required!)
-- Total = 56.32, BUT ESE minimum failed -> Grade RA
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_quiz_lab, ese, is_absent_ese) VALUES
(9, 80.00, 82.00, 80.00, 40.00, FALSE);

-- Enrollment 10: 24CSR025 - 24CST31 (THEORY: Passed with B Grade)
-- CIA: 60, 62, Assignment: 65 => CIA (40%) = 24.72
-- ESE: 50 => ESE contribution (60%) = 30.0 (>= 27)
-- Total = 54.72 => Grade B
INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_quiz_lab, ese, is_absent_ese) VALUES
(10, 60.00, 62.00, 65.00, 50.00, FALSE);
