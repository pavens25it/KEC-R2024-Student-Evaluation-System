import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ViewDatabase.java
 * -------------------------------------------------------------------------
 * Standalone viewer utility to inspect all 4 database tables:
 *  1. students
 *  2. courses
 *  3. enrollments (with attendance & condonation)
 *  4. assessment_marks (phased CIA, ESE, and AB status)
 * -------------------------------------------------------------------------
 */
public class ViewDatabase {

    public static void main(String[] args) {
        System.out.println("==========================================================================================");
        System.out.println("            KONGU ENGINEERING COLLEGE (R2024) - DATABASE INSPECTION VIEWER                ");
        System.out.println("==========================================================================================");

        try (Connection conn = DBConnection.getConnection()) {
            showStudents(conn);
            showCourses(conn);
            showEnrollments(conn);
            showAssessmentMarks(conn);
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to read database: " + e.getMessage());
        }
    }

    private static void showStudents(Connection conn) throws SQLException {
        System.out.println("\n>>> [TABLE 1: STUDENTS] <<<");
        String sql = "SELECT roll_no, name, department, current_semester FROM students ORDER BY department, roll_no LIMIT 25";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("+---------------+----------------------+--------------------------------+----------+");
            System.out.printf("| %-13s | %-20s | %-30s | %-8s |\n", "ROLL NUMBER", "NAME", "DEPARTMENT", "SEMESTER");
            System.out.println("+---------------+----------------------+--------------------------------+----------+");
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.printf("| %-13s | %-20s | %-30s | Sem %-4d |\n",
                        rs.getString("roll_no"),
                        truncate(rs.getString("name"), 20),
                        truncate(rs.getString("department"), 30),
                        rs.getInt("current_semester"));
            }
            System.out.println("+---------------+----------------------+--------------------------------+----------+");

            int total = getTotalCount(conn, "students");
            System.out.println("Displayed: " + count + " of " + total + " total students in database.");
        }
    }

    private static void showCourses(Connection conn) throws SQLException {
        System.out.println("\n>>> [TABLE 2: COURSES CATALOG] <<<");
        String sql = "SELECT course_code, title, credits, course_type FROM courses ORDER BY course_code";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("+---------------+------------------------------------------+---------+------------+");
            System.out.printf("| %-13s | %-40s | %-7s | %-10s |\n", "COURSE CODE", "TITLE", "CREDITS", "TYPE");
            System.out.println("+---------------+------------------------------------------+---------+------------+");
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.printf("| %-13s | %-40s | %-7d | %-10s |\n",
                        rs.getString("course_code"),
                        truncate(rs.getString("title"), 40),
                        rs.getInt("credits"),
                        rs.getString("course_type"));
            }
            System.out.println("+---------------+------------------------------------------+---------+------------+");
            System.out.println("Total Courses in Curriculum: " + count);
        }
    }

    private static void showEnrollments(Connection conn) throws SQLException {
        System.out.println("\n>>> [TABLE 3: COURSE ENROLLMENTS & ATTENDANCE] <<<");
        String sql =
            "SELECT e.enrollment_id, e.student_roll_no, s.name, e.course_code, e.semester, " +
            "       e.classes_held, e.classes_attended, e.medical_condonation " +
            "FROM enrollments e " +
            "JOIN students s ON e.student_roll_no = s.roll_no " +
            "ORDER BY e.semester, e.student_roll_no, e.course_code LIMIT 25";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("+-------+---------------+----------------------+-------------+-----+------+------+-------+-------------+");
            System.out.printf("| %-5s | %-13s | %-20s | %-11s | %-3s | %-4s | %-4s | %-5s | %-11s |\n",
                    "E_ID", "ROLL NO", "STUDENT NAME", "COURSE CODE", "SEM", "HELD", "ATTD", "ATTD%", "CONDONATION");
            System.out.println("+-------+---------------+----------------------+-------------+-----+------+------+-------+-------------+");
            int count = 0;
            while (rs.next()) {
                count++;
                int held = rs.getInt("classes_held");
                int attd = rs.getInt("classes_attended");
                double pct = (held > 0) ? ((double) attd / held) * 100.0 : 0.0;
                boolean cond = rs.getBoolean("medical_condonation");

                System.out.printf("| %-5d | %-13s | %-20s | %-11s | %-3d | %-4d | %-4d | %4.1f%% | %-11s |\n",
                        rs.getInt("enrollment_id"),
                        rs.getString("student_roll_no"),
                        truncate(rs.getString("name"), 20),
                        rs.getString("course_code"),
                        rs.getInt("semester"),
                        held, attd, pct,
                        cond ? "APPROVED" : "NO");
            }
            System.out.println("+-------+---------------+----------------------+-------------+-----+------+------+-------+-------------+");

            int total = getTotalCount(conn, "enrollments");
            System.out.println("Displayed: " + count + " of " + total + " total course enrollments in database.");
        }
    }

    private static void showAssessmentMarks(Connection conn) throws SQLException {
        System.out.println("\n>>> [TABLE 4: ASSESSMENT MARKS (PHASED EVALUATION)] <<<");
        String sql =
            "SELECT m.mark_id, m.enrollment_id, e.student_roll_no, e.course_code, " +
            "       m.cia1, m.cia2, m.assignment_quiz_lab, m.ese, m.is_absent_ese " +
            "FROM assessment_marks m " +
            "JOIN enrollments e ON m.enrollment_id = e.enrollment_id " +
            "ORDER BY m.enrollment_id LIMIT 25";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("+-------+-------+---------------+-------------+-------+-------+-------+-------+--------+");
            System.out.printf("| %-5s | %-5s | %-13s | %-11s | %-5s | %-5s | %-5s | %-5s | %-6s |\n",
                    "M_ID", "E_ID", "ROLL NO", "COURSE CODE", "CIA 1", "CIA 2", "ASSGN", "ESE", "STATUS");
            System.out.println("+-------+-------+---------------+-------------+-------+-------+-------+-------+--------+");
            int count = 0;
            while (rs.next()) {
                count++;
                java.math.BigDecimal cia1 = rs.getBigDecimal("cia1");
                java.math.BigDecimal cia2 = rs.getBigDecimal("cia2");
                java.math.BigDecimal assgn = rs.getBigDecimal("assignment_quiz_lab");
                java.math.BigDecimal ese = rs.getBigDecimal("ese");
                boolean isAbsent = rs.getBoolean("is_absent_ese");

                String status = isAbsent ? "ABSENT" : (ese == null ? "PEND" : "FINAL");

                System.out.printf("| %-5d | %-5d | %-13s | %-11s | %-5s | %-5s | %-5s | %-5s | %-6s |\n",
                        rs.getInt("mark_id"),
                        rs.getInt("enrollment_id"),
                        rs.getString("student_roll_no"),
                        rs.getString("course_code"),
                        cia1 != null ? String.format("%.0f", cia1) : "--",
                        cia2 != null ? String.format("%.0f", cia2) : "--",
                        assgn != null ? String.format("%.0f", assgn) : "--",
                        ese != null ? String.format("%.0f", ese) : (isAbsent ? "AB" : "--"),
                        status);
            }
            System.out.println("+-------+-------+---------------+-------------+-------+-------+-------+-------+--------+");

            int total = getTotalCount(conn, "assessment_marks");
            System.out.println("Displayed: " + count + " of " + total + " total assessment records in database.");
        }
    }

    private static int getTotalCount(Connection conn, String table) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table;
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
