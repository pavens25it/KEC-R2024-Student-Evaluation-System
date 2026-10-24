import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

/**
 * Add50Students.java
 * -------------------------------------------------------------------------
 * Generates and populates 50 realistic Kongu Engineering College student records
 * across Information Technology, Computer Science, AI & DS, and ECE departments,
 * along with course enrollments, attendance records, and phased assessments.
 *
 * Preserves existing records (such as Paven S and Nishal K) and enriches the
 * system for robust professor demonstrations and departmental analytics.
 * -------------------------------------------------------------------------
 */
public class Add50Students {

    private static final String[] FIRST_NAMES = {
        "Aadhavan", "Abinaya", "Ajith Kumar", "Ananya", "Arun", "Balaji",
        "Bhuvanesh", "Charumathi", "Deepak", "Dhanush", "Dharani", "Divya",
        "Gokulnath", "Hariharan", "Hemalatha", "Iniyan", "Ishwarya", "Janani",
        "Kabilan", "Karthika", "Kaviyarasu", "Kiruthika", "Logesh", "Madhumitha",
        "Manikandan", "Meenakshi", "Mithun", "Mohamed", "Monisha", "Mukesh",
        "Nandhini", "Naveen", "Nithya", "Pradeep", "Praveen", "Priyanka",
        "Ragul", "Ramkumar", "Revathi", "Sakthi", "Sandhiya", "Santhosh",
        "Saravanan", "Sneha", "Subash", "Suresh", "Swetha", "Vignesh",
        "Vijay", "Yuvaraj"
    };

    private static final String[] INITIALS = {
        "A", "B", "C", "D", "G", "K", "M", "N", "P", "R", "S", "T", "V"
    };

    public static void main(String[] args) {
        System.out.println("============================================================================");
        System.out.println(" Generating and Enrolling 50 Students into Kongu Engineering College System");
        System.out.println("============================================================================");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Core 3rd semester courses to enroll students into
            String[] itCourses = {"24ITT31", "24ITT32", "24ITC31", "24ITP31"};
            String[] cseCourses = {"24CST31", "24ITT31", "24ITC31", "24ITP31"};
            String[] aidsCourses = {"24AIT31", "24ITT31", "24ITC31", "24ITP31"};

            Random rand = new Random(2024); // Deterministic seed for reproducible testing
            int studentCount = 0;

            for (int i = 0; i < 50; i++) {
                String rollNo;
                String dept;
                String[] courses;

                if (i < 25) { // 25 Information Technology students
                    rollNo = String.format("24ITR%03d", i + 2); // 24ITR002 to 24ITR026
                    dept = "Information Technology";
                    courses = itCourses;
                } else if (i < 40) { // 15 Computer Science students
                    rollNo = String.format("24CSR%03d", (i - 25) + 30); // 24CSR030 to 24CSR044
                    dept = "Computer Science and Engineering";
                    courses = cseCourses;
                } else { // 10 AI & Data Science students
                    rollNo = String.format("24ADR%03d", (i - 40) + 20); // 24ADR020 to 24ADR029
                    dept = "Artificial Intelligence & Data Science";
                    courses = aidsCourses;
                }

                String fullName = FIRST_NAMES[i] + " " + INITIALS[i % INITIALS.length];
                int currentSem = 3;

                // 1. Insert Student
                insertStudent(conn, rollNo, fullName, dept, currentSem);
                studentCount++;

                // 2. Determine Academic Scenario for student
                // 0: Top Performer (Grade O / A+, Attendance > 90%)
                // 1: Above Average (Grade A / B+, Attendance 80-90%)
                // 2: Average Pass (Grade B, Attendance 75-80%)
                // 3: Medical Condonation Case (Attendance 68-74%, Approved)
                // 4: Defaulter Case (Attendance 50-64% -> SA Barred)
                // 5: Absent Case (Attendance > 80%, Absent in ESE)
                // 6: Arrear Case (Failed ESE minimum -> RA)
                int scenario = i % 7;

                for (String cCode : courses) {
                    int classesHeld = 50;
                    int classesAttended;
                    boolean condonation = false;
                    Double cia1 = null;
                    Double cia2 = null;
                    Double assignment = null;
                    Double ese = null;
                    boolean isAbsent = false;

                    switch (scenario) {
                        case 0: // Top performer
                            classesAttended = 46 + rand.nextInt(5); // 46-50 (92-100%)
                            cia1 = 90.0 + rand.nextInt(10);
                            cia2 = 90.0 + rand.nextInt(10);
                            assignment = 92.0 + rand.nextInt(8);
                            ese = 88.0 + rand.nextInt(12);
                            break;

                        case 1: // Above average
                            classesAttended = 41 + rand.nextInt(5); // 41-45 (82-90%)
                            cia1 = 78.0 + rand.nextInt(10);
                            cia2 = 80.0 + rand.nextInt(10);
                            assignment = 82.0 + rand.nextInt(10);
                            ese = 75.0 + rand.nextInt(12);
                            break;

                        case 2: // Average pass
                            classesAttended = 38 + rand.nextInt(4); // 38-41 (76-82%)
                            cia1 = 65.0 + rand.nextInt(10);
                            cia2 = 68.0 + rand.nextInt(10);
                            assignment = 70.0 + rand.nextInt(10);
                            ese = 60.0 + rand.nextInt(10);
                            break;

                        case 3: // Medical Condonation (68-74% attendance, approved)
                            classesAttended = 34 + rand.nextInt(3); // 34-36 (68-72%)
                            condonation = true;
                            cia1 = 72.0 + rand.nextInt(8);
                            cia2 = 74.0 + rand.nextInt(8);
                            assignment = 75.0 + rand.nextInt(8);
                            ese = 68.0 + rand.nextInt(10);
                            break;

                        case 4: // Attendance Defaulter (<65% attendance -> SA)
                            classesAttended = 25 + rand.nextInt(6); // 25-30 (50-60%)
                            condonation = false;
                            cia1 = 60.0 + rand.nextInt(10);
                            cia2 = 62.0 + rand.nextInt(10);
                            assignment = 65.0 + rand.nextInt(10);
                            ese = null; // Barred from ESE
                            break;

                        case 5: // Absent in ESE (AB)
                            classesAttended = 42 + rand.nextInt(5); // 84-92%
                            cia1 = 80.0 + rand.nextInt(10);
                            cia2 = 82.0 + rand.nextInt(10);
                            assignment = 85.0 + rand.nextInt(10);
                            ese = null;
                            isAbsent = true; // Absent in ESE
                            break;

                        case 6: // Failed ESE minimum (< 27/60 in Theory -> RA)
                        default:
                            classesAttended = 40 + rand.nextInt(5); // 80-88%
                            cia1 = 75.0 + rand.nextInt(8);
                            cia2 = 78.0 + rand.nextInt(8);
                            assignment = 80.0 + rand.nextInt(8);
                            ese = 35.0 + rand.nextInt(5); // ESE contribution = 21-24 < 27
                            break;
                    }

                    enrollAndGrade(conn, rollNo, cCode, currentSem, classesHeld, classesAttended,
                            condonation, cia1, cia2, assignment, ese, isAbsent);
                }
            }

            conn.commit();
            System.out.println("[SUCCESS] Successfully inserted " + studentCount + " students and their semester enrollments!");
            System.out.println("============================================================================");

            // Fetch and print count from database
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) AS total FROM students");
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println(" Total Registered Students in Database: " + rs.getInt("total"));
                }
            }
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) AS total FROM enrollments");
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println(" Total Course Enrollments in Database: " + rs.getInt("total"));
                }
            }
            System.out.println("============================================================================");

        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to add students: " + e.getMessage());
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

    private static void enrollAndGrade(Connection conn, String rollNo, String courseCode, int sem,
                                       int held, int attended, boolean condonation,
                                       Double cia1, Double cia2, Double assgn, Double ese, boolean isAbsent) throws SQLException {
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

        int enrollmentId = -1;
        String idSql = "SELECT enrollment_id FROM enrollments WHERE student_roll_no = ? AND course_code = ? AND semester = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(idSql)) {
            pstmt.setString(1, rollNo);
            pstmt.setString(2, courseCode);
            pstmt.setInt(3, sem);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    enrollmentId = rs.getInt("enrollment_id");
                }
            }
        }

        if (enrollmentId == -1) return;

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
