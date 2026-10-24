import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * SeedData.java
 * -------------------------------------------------------------------------
 * Populates realistic Kongu Engineering College (R2024) sample data for
 * academic demonstrations, professor reviews, and laboratory examinations.
 *
 * Populates:
 *  - 10+ Students across IT, CSE, and AI&DS departments
 *  - Comprehensive course catalog (Theory, Practical, Embedded)
 *  - Full enrollments for Semester 3
 *  - Complete marks showcasing:
 *      * Top performers (Grade O, SGPA > 9.5) including Paven S & Nishal K
 *      * Good performers (Grade A+, A)
 *      * Condonation approval cases
 *      * Attendance defaulters (SA - Shortage of attendance)
 *      * ESE Absence (AB)
 *      * Failed ESE minimum (RA - Re-Appear)
 *      * Work in progress / PENDING assessments
 * -------------------------------------------------------------------------
 */
public class SeedData {

    public static void main(String[] args) {
        System.out.println("============================================================================");
        System.out.println(" Populating KEC (R2024) Demonstration Records for Professor Review...");
        System.out.println("============================================================================");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Clean up temporary test course 24ITT01 if present
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM courses WHERE course_code = '24ITT01'")) {
                del.executeUpdate();
            }

            // 1. Insert/Update Courses
            insertCourse(conn, "24ITT31", "Data Structures and Algorithms", 3, "THEORY");
            insertCourse(conn, "24ITT32", "Database Management Systems", 3, "THEORY");
            insertCourse(conn, "24ITC31", "Object Oriented Programming with Java", 4, "EMBEDDED");
            insertCourse(conn, "24ITP31", "Data Structures and Java Laboratory", 2, "PRACTICAL");
            insertCourse(conn, "24MAT31", "Discrete Mathematics and Graph Theory", 4, "THEORY");
            insertCourse(conn, "24ITT41", "Computer Networks & Protocols", 3, "THEORY");
            insertCourse(conn, "24ITC41", "Full Stack Web Development", 4, "EMBEDDED");
            insertCourse(conn, "24ITP41", "Web & Database Development Lab", 2, "PRACTICAL");
            insertCourse(conn, "24AIT31", "Foundations of AI & Machine Learning", 3, "THEORY");
            insertCourse(conn, "24CST31", "Operating Systems & Systems Programming", 3, "THEORY");
            System.out.println("[SUCCESS] Courses catalog populated (10 courses).");

            // 2. Insert/Update Students
            insertStudent(conn, "25ITR104", "Paven S", "Information Technology", 3);
            insertStudent(conn, "25ITR101", "Nishal K", "Information Technology", 3);
            insertStudent(conn, "24ITR001", "Aarav Kumar S", "Information Technology", 3);
            insertStudent(conn, "24ITR015", "Bavithra M", "Information Technology", 3);
            insertStudent(conn, "24ITR045", "Dinesh Karthik K", "Information Technology", 3);
            insertStudent(conn, "24ITR060", "Gokul Prasath R", "Information Technology", 3);
            insertStudent(conn, "24ITR088", "Elango V", "Information Technology", 3);
            insertStudent(conn, "24CSR010", "Harini P", "Computer Science and Engineering", 3);
            insertStudent(conn, "24CSR025", "Jeeva Anand R", "Computer Science and Engineering", 3);
            insertStudent(conn, "24ADR005", "Keerthana S", "Artificial Intelligence & Data Science", 3);
            insertStudent(conn, "24ADR018", "Mithun Chakravarthy V", "Artificial Intelligence & Data Science", 3);
            System.out.println("[SUCCESS] Student demographics populated (11 students across IT, CSE, AI&DS).");

            // 3. Enrollments & Marks for Top Performer: 25ITR104 (Paven S) - Grade O Performer
            enrollAndGrade(conn, "25ITR104", "24ITC31", 3, 60, 58, false, 96.0, 95.0, 98.0, 94.0, false); // Grade O
            enrollAndGrade(conn, "25ITR104", "24ITT31", 3, 50, 48, false, 94.0, 95.0, 96.0, 92.0, false); // Grade O
            enrollAndGrade(conn, "25ITR104", "24ITT32", 3, 50, 49, false, 95.0, 94.0, 96.0, 91.0, false); // Grade O
            enrollAndGrade(conn, "25ITR104", "24ITP31", 3, 30, 29, false, 96.0, 96.0, 98.0, 95.0, false); // Grade O

            // 4. Enrollments & Marks for 25ITR101 (Nishal K) - Grade A+ Performer
            enrollAndGrade(conn, "25ITR101", "24ITC31", 3, 60, 55, false, 86.0, 88.0, 90.0, 84.0, false); // Grade A+
            enrollAndGrade(conn, "25ITR101", "24ITT31", 3, 50, 46, false, 84.0, 85.0, 88.0, 82.0, false); // Grade A+
            enrollAndGrade(conn, "25ITR101", "24ITT32", 3, 50, 45, false, 80.0, 82.0, 85.0, 78.0, false); // Grade A
            enrollAndGrade(conn, "25ITR101", "24ITP31", 3, 30, 28, false, 88.0, 90.0, 92.0, 88.0, false); // Grade A+

            // 5. Enrollments & Marks for 24ITR001 (Aarav Kumar S) - Excellent
            enrollAndGrade(conn, "24ITR001", "24ITC31", 3, 60, 54, false, 95.0, 94.0, 96.0, 92.0, false); // Grade O
            enrollAndGrade(conn, "24ITR001", "24ITT31", 3, 50, 46, false, 92.0, 90.0, 95.0, 88.0, false); // Grade A+
            enrollAndGrade(conn, "24ITR001", "24ITP31", 3, 30, 28, false, 85.0, 88.0, 90.0, 85.0, false); // Grade A+

            // 6. Enrollments & Marks for 24ITR015 (Bavithra M) - Condonation Approved
            enrollAndGrade(conn, "24ITR015", "24ITT31", 3, 50, 35, true,  75.0, 70.0, 72.0, 65.0, false); // 70% condoned -> B+
            enrollAndGrade(conn, "24ITR015", "24ITC31", 3, 60, 48, false, 80.0, 78.0, 85.0, null, false); // ESE Pending

            // 7. Enrollments & Marks for 24ITR045 (Dinesh Karthik K) - Absent in ESE & SA case
            enrollAndGrade(conn, "24ITR045", "24ITT31", 3, 50, 34, false, 60.0, 65.0, 70.0, null, false); // 68% no condonation -> SA
            enrollAndGrade(conn, "24ITR045", "24ITC31", 3, 60, 50, false, 72.0, 68.0, 75.0, null, true);  // Eligible -> Absent (AB)

            // 8. Enrollments & Marks for 24ITR088 (Elango V) - Defaulter below 65%
            enrollAndGrade(conn, "24ITR088", "24ITT31", 3, 50, 28, false, 45.0, 50.0, 55.0, null, false); // 56% -> SA

            // 9. Enrollments & Marks for 24CSR010 (Harini P) - Failed ESE minimum
            enrollAndGrade(conn, "24CSR010", "24CST31", 3, 45, 42, false, 80.0, 82.0, 80.0, 40.0, false); // ESE contrib 24 < 27 -> RA

            // 10. Enrollments & Marks for 24CSR025 (Jeeva Anand R) - Regular Pass
            enrollAndGrade(conn, "24CSR025", "24CST31", 3, 45, 40, false, 60.0, 62.0, 65.0, 50.0, false); // Grade B

            // 11. Enrollments for AI&DS students
            enrollAndGrade(conn, "24ADR005", "24AIT31", 3, 50, 48, false, 94.0, 96.0, 95.0, 92.0, false); // Grade O
            enrollAndGrade(conn, "24ADR005", "24ITT31", 3, 50, 46, false, 88.0, 90.0, 92.0, 86.0, false); // Grade A+
            enrollAndGrade(conn, "24ADR018", "24AIT31", 3, 50, 44, false, 75.0, 78.0, 80.0, 72.0, false); // Grade A

            conn.commit();
            System.out.println("[SUCCESS] All demonstration enrollments and academic evaluations recorded!");
            System.out.println("============================================================================");
            System.out.println(" You can now show your professor:");
            System.out.println("  1. Option 2  - Full list of 11 students across IT, CSE, and AI&DS");
            System.out.println("  2. Option 4  - Curriculum Course Catalog (Theory, Practical, Embedded)");
            System.out.println("  3. Option 11 - Grade sheets for Paven S (25ITR104) and Nishal K (25ITR101)");
            System.out.println("  4. Option 12 - Defaulter Report (students with SA status)");
            System.out.println("  5. Option 13 - Department Performance Summary (pass/fail/averages)");
            System.out.println("============================================================================");

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to seed records: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void insertStudent(Connection conn, String rollNo, String name, String dept, int sem) throws SQLException {
        String sql = "INSERT INTO students (roll_no, name, department, current_semester) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE name = VALUES(name), department = VALUES(department), current_semester = VALUES(current_semester)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, rollNo);
            pstmt.setString(2, name);
            pstmt.setString(3, dept);
            pstmt.setInt(4, sem);
            pstmt.executeUpdate();
        }
    }

    private static void insertCourse(Connection conn, String code, String title, int credits, String type) throws SQLException {
        String sql = "INSERT INTO courses (course_code, title, credits, course_type) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE title = VALUES(title), credits = VALUES(credits), course_type = VALUES(course_type)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.setString(2, title);
            pstmt.setInt(3, credits);
            pstmt.setString(4, type);
            pstmt.executeUpdate();
        }
    }

    private static void enrollAndGrade(Connection conn, String rollNo, String courseCode, int sem,
                                       int held, int attended, boolean condonation,
                                       Double cia1, Double cia2, Double assgn, Double ese, boolean isAbsent) throws SQLException {
        // 1. Upsert enrollment
        String enrollSql = "INSERT INTO enrollments (student_roll_no, course_code, semester, classes_held, classes_attended, medical_condonation) " +
                           "VALUES (?, ?, ?, ?, ?, ?) " +
                           "ON DUPLICATE KEY UPDATE classes_held = VALUES(classes_held), classes_attended = VALUES(classes_attended), medical_condonation = VALUES(medical_condonation)";
        try (PreparedStatement pstmt = conn.prepareStatement(enrollSql)) {
            pstmt.setString(1, rollNo);
            pstmt.setString(2, courseCode);
            pstmt.setInt(3, sem);
            pstmt.setInt(4, held);
            pstmt.setInt(5, attended);
            pstmt.setBoolean(6, condonation);
            pstmt.executeUpdate();
        }

        // 2. Fetch enrollment ID
        int enrollmentId = -1;
        String idSql = "SELECT enrollment_id FROM enrollments WHERE student_roll_no = ? AND course_code = ? AND semester = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(idSql)) {
            pstmt.setString(1, rollNo);
            pstmt.setString(2, courseCode);
            pstmt.setInt(3, sem);
            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    enrollmentId = rs.getInt("enrollment_id");
                }
            }
        }

        if (enrollmentId == -1) return;

        // 3. Upsert assessment marks
        String marksSql = "INSERT INTO assessment_marks (enrollment_id, cia1, cia2, assignment_quiz_lab, ese, is_absent_ese) " +
                          "VALUES (?, ?, ?, ?, ?, ?) " +
                          "ON DUPLICATE KEY UPDATE cia1 = VALUES(cia1), cia2 = VALUES(cia2), " +
                          "assignment_quiz_lab = VALUES(assignment_quiz_lab), ese = VALUES(ese), is_absent_ese = VALUES(is_absent_ese)";
        try (PreparedStatement pstmt = conn.prepareStatement(marksSql)) {
            pstmt.setInt(1, enrollmentId);

            if (cia1 != null) pstmt.setDouble(2, cia1); else pstmt.setNull(2, java.sql.Types.DECIMAL);
            if (cia2 != null) pstmt.setDouble(3, cia2); else pstmt.setNull(3, java.sql.Types.DECIMAL);
            if (assgn != null) pstmt.setDouble(4, assgn); else pstmt.setNull(4, java.sql.Types.DECIMAL);
            if (ese != null) pstmt.setDouble(5, ese); else pstmt.setNull(5, java.sql.Types.DECIMAL);
            pstmt.setBoolean(6, isAbsent);

            pstmt.executeUpdate();
        }
    }
}
