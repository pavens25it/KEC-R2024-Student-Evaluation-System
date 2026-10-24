import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * StudentDAO.java
 * -------------------------------------------------------------------------
 * Data Access Object (DAO) for managing Student and Course entities in the
 * Kongu Engineering College (R2024) Evaluation System.
 *
 * Implements:
 *  - Full CRUD operations using PreparedStatements to prevent SQL injection.
 *  - Proper connection and statement lifecycle via try-with-resources.
 *  - Input validation aligned with KEC R2024 schema constraints.
 * -------------------------------------------------------------------------
 */
public class StudentDAO {

    // =========================================================================
    // Student Entity Model
    // =========================================================================
    public static class Student {
        private String rollNo;
        private String name;
        private String department;
        private int currentSemester;

        public Student(String rollNo, String name, String department, int currentSemester) {
            this.rollNo = rollNo;
            this.name = name;
            this.department = department;
            this.currentSemester = currentSemester;
        }

        public String getRollNo() { return rollNo; }
        public String getName() { return name; }
        public String getDepartment() { return department; }
        public int getCurrentSemester() { return currentSemester; }

        public void setName(String name) { this.name = name; }
        public void setDepartment(String department) { this.department = department; }
        public void setCurrentSemester(int currentSemester) { this.currentSemester = currentSemester; }

        @Override
        public String toString() {
            return String.format("[Roll: %-10s | Name: %-25s | Dept: %-30s | Sem: %d]",
                    rollNo, name, department, currentSemester);
        }
    }

    // =========================================================================
    // Course Entity Model
    // =========================================================================
    public static class Course {
        private String courseCode;
        private String title;
        private int credits;
        private String courseType; // THEORY, PRACTICAL, EMBEDDED

        public Course(String courseCode, String title, int credits, String courseType) {
            this.courseCode = courseCode;
            this.title = title;
            this.credits = credits;
            this.courseType = courseType;
        }

        public String getCourseCode() { return courseCode; }
        public String getTitle() { return title; }
        public int getCredits() { return credits; }
        public String getCourseType() { return courseType; }

        public void setTitle(String title) { this.title = title; }
        public void setCredits(int credits) { this.credits = credits; }
        public void setCourseType(String courseType) { this.courseType = courseType; }

        @Override
        public String toString() {
            return String.format("[Code: %-10s | Title: %-35s | Credits: %d | Type: %-9s]",
                    courseCode, title, credits, courseType);
        }
    }

    // =========================================================================
    // Student CRUD Operations
    // =========================================================================

    /**
     * Adds a new student record to the database.
     *
     * @param rollNo          Unique student roll number (e.g. 24ITR001)
     * @param name            Full name of student
     * @param department      Department (e.g. Information Technology)
     * @param currentSemester Semester number (1 to 8)
     * @return true if added successfully, false otherwise
     */
    public boolean addStudent(String rollNo, String name, String department, int currentSemester) {
        if (rollNo == null || rollNo.trim().isEmpty() ||
            name == null || name.trim().isEmpty() ||
            department == null || department.trim().isEmpty()) {
            System.err.println("[VALIDATION ERROR] Student fields cannot be empty.");
            return false;
        }

        if (currentSemester < 1 || currentSemester > 8) {
            System.err.println("[VALIDATION ERROR] Current semester must be between 1 and 8.");
            return false;
        }

        String sql = "INSERT INTO students (roll_no, name, department, current_semester) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, rollNo.trim().toUpperCase());
            pstmt.setString(2, name.trim());
            pstmt.setString(3, department.trim());
            pstmt.setInt(4, currentSemester);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to add student: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a student with the given roll number exists.
     */
    public boolean studentExists(String rollNo) {
        if (rollNo == null || rollNo.trim().isEmpty()) return false;

        String sql = "SELECT 1 FROM students WHERE roll_no = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, rollNo.trim().toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to check student existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all student records ordered by department and roll number.
     */
    public List<Student> getAllStudents() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT roll_no, name, department, current_semester FROM students ORDER BY department, roll_no";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(new Student(
                        rs.getString("roll_no"),
                        rs.getString("name"),
                        rs.getString("department"),
                        rs.getInt("current_semester")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to fetch students: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves a student by their unique roll number.
     */
    public Student getStudentByRollNo(String rollNo) {
        if (rollNo == null || rollNo.trim().isEmpty()) return null;

        String sql = "SELECT roll_no, name, department, current_semester FROM students WHERE roll_no = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, rollNo.trim().toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            rs.getString("roll_no"),
                            rs.getString("name"),
                            rs.getString("department"),
                            rs.getInt("current_semester")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to fetch student by roll number: " + e.getMessage());
        }
        return null;
    }

    // =========================================================================
    // Course CRUD Operations
    // =========================================================================

    /**
     * Adds a new course to the curriculum catalog.
     *
     * @param courseCode Unique course code (e.g. 24ITT31)
     * @param title      Course title
     * @param credits    Credits (1 to 10)
     * @param courseType THEORY, PRACTICAL, or EMBEDDED
     * @return true if added successfully, false otherwise
     */
    public boolean addCourse(String courseCode, String title, int credits, String courseType) {
        if (courseCode == null || courseCode.trim().isEmpty() ||
            title == null || title.trim().isEmpty() ||
            courseType == null || courseType.trim().isEmpty()) {
            System.err.println("[VALIDATION ERROR] Course fields cannot be empty.");
            return false;
        }

        String typeUpper = courseType.trim().toUpperCase();
        if (!typeUpper.equals("THEORY") && !typeUpper.equals("PRACTICAL") && !typeUpper.equals("EMBEDDED")) {
            System.err.println("[VALIDATION ERROR] Course type must be THEORY, PRACTICAL, or EMBEDDED.");
            return false;
        }

        if (credits <= 0 || credits > 10) {
            System.err.println("[VALIDATION ERROR] Credits must be between 1 and 10.");
            return false;
        }

        String sql = "INSERT INTO courses (course_code, title, credits, course_type) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseCode.trim().toUpperCase());
            pstmt.setString(2, title.trim());
            pstmt.setInt(3, credits);
            pstmt.setString(4, typeUpper);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to add course: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a course with the specified code exists.
     */
    public boolean courseExists(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) return false;

        String sql = "SELECT 1 FROM courses WHERE course_code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseCode.trim().toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to check course existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all courses ordered by course code.
     */
    public List<Course> getAllCourses() {
        List<Course> list = new ArrayList<>();
        String sql = "SELECT course_code, title, credits, course_type FROM courses ORDER BY course_code";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(new Course(
                        rs.getString("course_code"),
                        rs.getString("title"),
                        rs.getInt("credits"),
                        rs.getString("course_type")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to fetch courses: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves a course by its course code.
     */
    public Course getCourseByCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) return null;

        String sql = "SELECT course_code, title, credits, course_type FROM courses WHERE course_code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseCode.trim().toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Course(
                            rs.getString("course_code"),
                            rs.getString("title"),
                            rs.getInt("credits"),
                            rs.getString("course_type")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to fetch course by code: " + e.getMessage());
        }
        return null;
    }
}
