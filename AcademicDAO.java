import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AcademicDAO.java
 * -------------------------------------------------------------------------
 * Core business and data access logic for Kongu Engineering College (R2024)
 * Student Academic & Continuous Evaluation System.
 *
 * Covers:
 *  1. Course Enrollment and Attendance Tracking
 *  2. Phased Incremental Assessment Updates (CIA 1, CIA 2, Assignments, ESE)
 *  3. Configurable Internal Continuous Assessment (CIA) formulas
 *  4. Strict R2024 Course-Type Evaluation Rules (THEORY, PRACTICAL, EMBEDDED)
 *  5. Attendance Eligibility and Condonation processing
 *  6. Semester Mark Sheet generation with SGPA and equivalent percentage
 *  7. Attendance Defaulters Report
 *  8. Department Performance Summary
 * -------------------------------------------------------------------------
 */
public class AcademicDAO {

    // =========================================================================
    // Configurable CIA Calculation Formula
    // =========================================================================
    public enum CIAFormula {
        WEIGHTED_40_40_20,        // (CIA1 * 0.40) + (CIA2 * 0.40) + (Assignment * 0.20) [Default KEC assumption]
        AVERAGE_CIA_80_ASSGN_20,  // ((CIA1 + CIA2)/2 * 0.80) + (Assignment * 0.20)
        EQUAL_WEIGHT              // (CIA1 + CIA2 + Assignment) / 3.0
    }

    private static CIAFormula activeFormula = CIAFormula.WEIGHTED_40_40_20;

    public static CIAFormula getActiveFormula() {
        return activeFormula;
    }

    public static void setActiveFormula(CIAFormula formula) {
        if (formula != null) {
            activeFormula = formula;
        }
    }

    // =========================================================================
    // Data Transfer Objects (DTOs) for Clean Reporting
    // =========================================================================
    public static class CourseEvaluationResult {
        public String courseCode;
        public String title;
        public String courseType;
        public int credits;
        public int classesHeld;
        public int classesAttended;
        public double attendancePct;
        public boolean medicalCondonation;
        public boolean attendanceEligible;
        public String attendanceStatusRemark; // ELIGIBLE, CONDONED, DEF_SA

        public Double cia1;
        public Double cia2;
        public Double assignment;
        public Double cia100;           // Internal scaled out of 100
        public Double ciaWeighted;      // Weighted CIA contribution (e.g. 40, 60, or 50)

        public Double eseRaw;           // Raw ESE out of 100
        public Double eseWeighted;      // Weighted ESE contribution (e.g. 60, 40, or 50)
        public boolean isAbsentEse;

        public Double totalMarks;       // Sum of weighted CIA + weighted ESE (0-100)
        public String grade;            // O, A+, A, B+, B, RA, SA, AB, PENDING
        public Integer gradePoint;      // 10, 9, 8, 7, 6, 0 (or null if pending)
        public String resultStatus;     // PASS, RE-APPEAR (RA), SHORTAGE (SA), ABSENT (AB), PENDING
        public String failureReason;    // Null if pass; details if failed
    }

    public static class SemesterMarkSheet {
        public String rollNo;
        public String studentName;
        public String department;
        public int semester;
        public List<CourseEvaluationResult> courses = new ArrayList<>();
        public int totalRegisteredCredits;
        public int earnedCredits;
        public Double sgpa;             // Null if any course is pending
        public Double equivalentPercentage; // Null if any course is pending
        public boolean isPending;
        public int passedCoursesCount;
        public int failedCoursesCount;
        public int pendingCoursesCount;
    }

    public static class AttendanceDefaulterItem {
        public String rollNo;
        public String studentName;
        public String department;
        public int semester;
        public String courseCode;
        public String courseTitle;
        public int classesHeld;
        public int classesAttended;
        public double attendancePct;
        public boolean condonationApproved;
        public String statusRemark;
    }

    public static class DepartmentSummary {
        public String department;
        public int totalStudents;
        public int totalEnrollments;
        public int passedCount;
        public int failedRACount;
        public int absentABCount;
        public int shortageSACount;
        public int pendingCount;
        public double classAverageSGPA;
        public int studentsWithFinalSGPA;
    }

    // =========================================================================
    // 1. Enrollment & Attendance Operations
    // =========================================================================

    /**
     * Enrolls a student in a course for a semester and initializes attendance.
     */
    public boolean enrollStudent(String rollNo, String courseCode, int semester,
                                 int classesHeld, int classesAttended, boolean condonation) {
        if (rollNo == null || courseCode == null) return false;
        if (classesHeld < 0 || classesAttended < 0 || classesAttended > classesHeld) {
            System.err.println("[VALIDATION ERROR] Classes attended must be >= 0 and <= classes held.");
            return false;
        }

        String sql = "INSERT INTO enrollments (student_roll_no, course_code, semester, " +
                     "classes_held, classes_attended, medical_condonation) " +
                     "VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE classes_held = VALUES(classes_held), " +
                     "classes_attended = VALUES(classes_attended), " +
                     "medical_condonation = VALUES(medical_condonation)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, rollNo.trim().toUpperCase());
            pstmt.setString(2, courseCode.trim().toUpperCase());
            pstmt.setInt(3, semester);
            pstmt.setInt(4, classesHeld);
            pstmt.setInt(5, classesAttended);
            pstmt.setBoolean(6, condonation);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to enroll student: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates attendance details for an enrollment identified by roll number, course code, and semester.
     */
    public boolean updateAttendance(String rollNo, String courseCode, int semester,
                                    int classesHeld, int classesAttended, boolean condonation) {
        if (classesHeld < 0 || classesAttended < 0 || classesAttended > classesHeld) {
            System.err.println("[VALIDATION ERROR] Classes attended must be between 0 and classes held (" + classesHeld + ").");
            return false;
        }

        String sql = "UPDATE enrollments SET classes_held = ?, classes_attended = ?, medical_condonation = ? " +
                     "WHERE student_roll_no = ? AND course_code = ? AND semester = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, classesHeld);
            pstmt.setInt(2, classesAttended);
            pstmt.setBoolean(3, condonation);
            pstmt.setString(4, rollNo.trim().toUpperCase());
            pstmt.setString(5, courseCode.trim().toUpperCase());
            pstmt.setInt(6, semester);

            int updated = pstmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to update attendance: " + e.getMessage());
            return false;
        }
    }

    /**
     * Resolves an enrollment ID for a student, course, and semester.
     */
    public Integer getEnrollmentId(String rollNo, String courseCode, int semester) {
        String sql = "SELECT enrollment_id FROM enrollments WHERE student_roll_no = ? AND course_code = ? AND semester = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, rollNo.trim().toUpperCase());
            pstmt.setString(2, courseCode.trim().toUpperCase());
            pstmt.setInt(3, semester);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("enrollment_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to find enrollment: " + e.getMessage());
        }
        return null;
    }

    // =========================================================================
    // 2. Incremental Assessment Marks Entry (Preserving Existing Marks)
    // =========================================================================

    /**
     * Helper to ensure an assessment_marks row exists for an enrollment.
     */
    private void ensureAssessmentRecordExists(Connection conn, int enrollmentId) throws SQLException {
        String checkSql = "SELECT 1 FROM assessment_marks WHERE enrollment_id = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, enrollmentId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {
                    String insertSql = "INSERT INTO assessment_marks (enrollment_id) VALUES (?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, enrollmentId);
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }

    /**
     * Phase 1: Enter / Update CIA 1 (scale 0 - 100).
     */
    public boolean updateCia1(int enrollmentId, double marks) {
        if (marks < 0.0 || marks > 100.0) {
            System.err.println("[VALIDATION ERROR] CIA 1 marks must be between 0 and 100.");
            return false;
        }

        try (Connection conn = DBConnection.getConnection()) {
            ensureAssessmentRecordExists(conn, enrollmentId);
            String sql = "UPDATE assessment_marks SET cia1 = ? WHERE enrollment_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, marks);
                pstmt.setInt(2, enrollmentId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to update CIA 1: " + e.getMessage());
            return false;
        }
    }

    /**
     * Phase 1: Enter / Update CIA 2 (scale 0 - 100).
     */
    public boolean updateCia2(int enrollmentId, double marks) {
        if (marks < 0.0 || marks > 100.0) {
            System.err.println("[VALIDATION ERROR] CIA 2 marks must be between 0 and 100.");
            return false;
        }

        try (Connection conn = DBConnection.getConnection()) {
            ensureAssessmentRecordExists(conn, enrollmentId);
            String sql = "UPDATE assessment_marks SET cia2 = ? WHERE enrollment_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, marks);
                pstmt.setInt(2, enrollmentId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to update CIA 2: " + e.getMessage());
            return false;
        }
    }

    /**
     * Phase 2: Enter / Update Assignments / Quizzes / Lab Evaluations (scale 0 - 100).
     */
    public boolean updateAssignmentQuizLab(int enrollmentId, double marks) {
        if (marks < 0.0 || marks > 100.0) {
            System.err.println("[VALIDATION ERROR] Assignment/Quiz/Lab marks must be between 0 and 100.");
            return false;
        }

        try (Connection conn = DBConnection.getConnection()) {
            ensureAssessmentRecordExists(conn, enrollmentId);
            String sql = "UPDATE assessment_marks SET assignment_quiz_lab = ? WHERE enrollment_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, marks);
                pstmt.setInt(2, enrollmentId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to update Assignment/Quiz/Lab: " + e.getMessage());
            return false;
        }
    }

    /**
     * Phase 3: Enter End Semester Examination (ESE) mark or mark Absent (AB).
     * If isAbsent is true, mark is treated as absent (ese column set to null, is_absent_ese = true).
     * Otherwise, validate mark (0 - 100), set ese, and set is_absent_ese = false.
     */
    public boolean updateEse(int enrollmentId, Double marks, boolean isAbsent) {
        if (!isAbsent) {
            if (marks == null || marks < 0.0 || marks > 100.0) {
                System.err.println("[VALIDATION ERROR] ESE marks must be between 0 and 100.");
                return false;
            }
        }

        try (Connection conn = DBConnection.getConnection()) {
            ensureAssessmentRecordExists(conn, enrollmentId);
            String sql = "UPDATE assessment_marks SET ese = ?, is_absent_ese = ? WHERE enrollment_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                if (isAbsent) {
                    pstmt.setNull(1, Types.DECIMAL);
                    pstmt.setBoolean(2, true);
                } else {
                    pstmt.setDouble(1, marks);
                    pstmt.setBoolean(2, false);
                }
                pstmt.setInt(3, enrollmentId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to update ESE: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // 3. Evaluation Rules & Internal Assessment Conversion
    // =========================================================================

    /**
     * Computes the combined Continuous Internal Assessment (out of 100) using the
     * active formula. Returns null if any required component is missing.
     */
    public static Double computeCiaOutOf100(Double cia1, Double cia2, Double assignment) {
        if (cia1 == null || cia2 == null || assignment == null) {
            return null; // Incomplete assessment
        }

        switch (activeFormula) {
            case AVERAGE_CIA_80_ASSGN_20:
                double ciaAvg = (cia1 + cia2) / 2.0;
                return (ciaAvg * 0.80) + (assignment * 0.20);
            case EQUAL_WEIGHT:
                return (cia1 + cia2 + assignment) / 3.0;
            case WEIGHTED_40_40_20:
            default:
                // Official KEC R2024 Working Assumption:
                // CIA 1: 40%, CIA 2: 40%, Assignments/Tutorials/Quizzes: 20%
                return (cia1 * 0.40) + (cia2 * 0.40) + (assignment * 0.20);
        }
    }

    /**
     * Evaluates a single course enrollment under KEC R2024 regulations.
     */
    public static CourseEvaluationResult evaluateCourse(
            String courseCode, String title, String courseType, int credits,
            int classesHeld, int classesAttended, boolean medicalCondonation,
            Double cia1, Double cia2, Double assignment, Double ese, boolean isAbsentEse) {

        CourseEvaluationResult res = new CourseEvaluationResult();
        res.courseCode = courseCode;
        res.title = title;
        res.courseType = courseType.toUpperCase();
        res.credits = credits;
        res.classesHeld = classesHeld;
        res.classesAttended = classesAttended;
        res.medicalCondonation = medicalCondonation;
        res.cia1 = cia1;
        res.cia2 = cia2;
        res.assignment = assignment;
        res.eseRaw = ese;
        res.isAbsentEse = isAbsentEse;

        // 1. Calculate Attendance Percentage
        if (classesHeld > 0) {
            res.attendancePct = ((double) classesAttended / classesHeld) * 100.0;
        } else {
            res.attendancePct = 100.0; // Default when no classes recorded yet
        }

        // 2. Attendance Eligibility Evaluation
        // Rules:
        //  >= 75%               : Eligible
        //  65% <= Att < 75%     : Conditionally eligible; needs medical condonation
        //  < 65%                : SA (Shortage of Attendance, Barred from ESE, GP 0)
        if (res.attendancePct >= 75.0) {
            res.attendanceEligible = true;
            res.attendanceStatusRemark = "ELIGIBLE";
        } else if (res.attendancePct >= 65.0) {
            if (medicalCondonation) {
                res.attendanceEligible = true;
                res.attendanceStatusRemark = "CONDONED (ELIGIBLE)";
            } else {
                res.attendanceEligible = false;
                res.attendanceStatusRemark = "SA (NO CONDONATION)";
            }
        } else {
            res.attendanceEligible = false;
            res.attendanceStatusRemark = "SA (DEF_BELOW_65)";
        }

        // If attendance shortage: immediate SA status
        if (!res.attendanceEligible) {
            res.grade = "SA";
            res.gradePoint = 0;
            res.resultStatus = "FAIL (SA)";
            res.failureReason = String.format("Attendance Shortage (%.1f%%) - Barred from ESE", res.attendancePct);
            return res;
        }

        // 3. ESE Absence Check
        if (isAbsentEse) {
            res.grade = "AB";
            res.gradePoint = 0;
            res.resultStatus = "ABSENT (AB)";
            res.failureReason = "Absent in End Semester Examination";
            return res;
        }

        // 4. Check for Pending Assessments
        res.cia100 = computeCiaOutOf100(cia1, cia2, assignment);
        if (res.cia100 == null || ese == null) {
            res.grade = "PENDING";
            res.gradePoint = null;
            res.resultStatus = "PENDING";
            res.failureReason = "Incomplete Marks (Awaiting CIA components or ESE)";
            return res;
        }

        // 5. Weightage Calculation by Course Type
        double ciaWeightPct;
        double eseWeightPct;
        double minEseContrib;

        switch (res.courseType) {
            case "PRACTICAL":
                ciaWeightPct = 0.60;
                eseWeightPct = 0.40;
                minEseContrib = 20.0; // 20 / 40
                break;
            case "EMBEDDED":
                ciaWeightPct = 0.50;
                eseWeightPct = 0.50;
                minEseContrib = 22.5; // 22.5 / 50
                break;
            case "THEORY":
            default:
                ciaWeightPct = 0.40;
                eseWeightPct = 0.60;
                minEseContrib = 27.0; // 27 / 60
                break;
        }

        res.ciaWeighted = res.cia100 * ciaWeightPct;
        res.eseWeighted = (ese / 100.0) * (eseWeightPct * 100.0);
        res.totalMarks = res.ciaWeighted + res.eseWeighted;

        // 6. Pass / Fail Verification
        // Condition: ESE contribution >= minimum required AND total marks >= 50
        boolean passedEseMin = res.eseWeighted >= (minEseContrib - 1e-6);
        boolean passedTotal = res.totalMarks >= 50.0;

        if (passedEseMin && passedTotal) {
            res.resultStatus = "PASS";
            // KEC R2024 Grade Thresholds
            double total = Math.round(res.totalMarks * 100.0) / 100.0;
            if (total >= 91.0) {
                res.grade = "O";
                res.gradePoint = 10;
            } else if (total >= 81.0) {
                res.grade = "A+";
                res.gradePoint = 9;
            } else if (total >= 71.0) {
                res.grade = "A";
                res.gradePoint = 8;
            } else if (total >= 61.0) {
                res.grade = "B+";
                res.gradePoint = 7;
            } else { // 50 to 60.99
                res.grade = "B";
                res.gradePoint = 6;
            }
        } else {
            res.grade = "RA";
            res.gradePoint = 0;
            res.resultStatus = "RE-APPEAR (RA)";
            if (!passedEseMin && !passedTotal) {
                res.failureReason = String.format("Failed ESE minimum (%.2f < %.1f) & Total < 50 (%.2f)",
                        res.eseWeighted, minEseContrib, res.totalMarks);
            } else if (!passedEseMin) {
                res.failureReason = String.format("Failed ESE minimum requirement (%.2f < %.1f)",
                        res.eseWeighted, minEseContrib);
            } else {
                res.failureReason = String.format("Overall total below pass mark (%.2f < 50.0)", res.totalMarks);
            }
        }

        return res;
    }

    // =========================================================================
    // 4. Semester Mark Sheet & SGPA Generation
    // =========================================================================

    private static Double getNullableDouble(ResultSet rs, String colName) throws SQLException {
        BigDecimal bd = rs.getBigDecimal(colName);
        return (bd != null) ? bd.doubleValue() : null;
    }

    /**
     * Generates a comprehensive semester mark sheet for a student and semester.
     */
    public SemesterMarkSheet generateSemesterMarkSheet(String rollNo, int semester) {
        if (rollNo == null) return null;

        String studentSql = "SELECT roll_no, name, department FROM students WHERE roll_no = ?";
        SemesterMarkSheet sheet = new SemesterMarkSheet();
        sheet.rollNo = rollNo.trim().toUpperCase();
        sheet.semester = semester;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement studentStmt = conn.prepareStatement(studentSql)) {

            studentStmt.setString(1, sheet.rollNo);
            try (ResultSet rs = studentStmt.executeQuery()) {
                if (rs.next()) {
                    sheet.studentName = rs.getString("name");
                    sheet.department = rs.getString("department");
                } else {
                    return null; // Student not found
                }
            }

            String evalSql =
                "SELECT e.enrollment_id, e.course_code, c.title, c.credits, c.course_type, " +
                "       e.classes_held, e.classes_attended, e.medical_condonation, " +
                "       m.cia1, m.cia2, m.assignment_quiz_lab, m.ese, m.is_absent_ese " +
                "FROM enrollments e " +
                "JOIN courses c ON e.course_code = c.course_code " +
                "LEFT JOIN assessment_marks m ON e.enrollment_id = m.enrollment_id " +
                "WHERE e.student_roll_no = ? AND e.semester = ? " +
                "ORDER BY e.course_code";

            try (PreparedStatement evalStmt = conn.prepareStatement(evalSql)) {
                evalStmt.setString(1, sheet.rollNo);
                evalStmt.setInt(2, semester);

                try (ResultSet rs = evalStmt.executeQuery()) {
                    int totalCreds = 0;
                    int earnedCreds = 0;
                    double weightedGradePointSum = 0.0;
                    boolean anyPending = false;

                    while (rs.next()) {
                        String code = rs.getString("course_code");
                        String title = rs.getString("title");
                        String type = rs.getString("course_type");
                        int creds = rs.getInt("credits");
                        int held = rs.getInt("classes_held");
                        int attended = rs.getInt("classes_attended");
                        boolean condonation = rs.getBoolean("medical_condonation");

                        Double cia1 = getNullableDouble(rs, "cia1");
                        Double cia2 = getNullableDouble(rs, "cia2");
                        Double assgn = getNullableDouble(rs, "assignment_quiz_lab");
                        Double ese = getNullableDouble(rs, "ese");
                        boolean isAbsent = rs.getBoolean("is_absent_ese");

                        CourseEvaluationResult eval = evaluateCourse(
                                code, title, type, creds, held, attended, condonation,
                                cia1, cia2, assgn, ese, isAbsent
                        );

                        sheet.courses.add(eval);
                        totalCreds += creds;

                        if ("PENDING".equals(eval.grade)) {
                            anyPending = true;
                            sheet.pendingCoursesCount++;
                        } else if ("PASS".equals(eval.resultStatus)) {
                            earnedCreds += creds;
                            sheet.passedCoursesCount++;
                            weightedGradePointSum += (creds * eval.gradePoint);
                        } else {
                            sheet.failedCoursesCount++;
                            // Grade point is 0 for RA, SA, AB
                            weightedGradePointSum += (creds * 0);
                        }
                    }

                    sheet.totalRegisteredCredits = totalCreds;
                    sheet.earnedCredits = earnedCreds;
                    sheet.isPending = anyPending;

                    // SGPA calculation:
                    // If any course is PENDING, final SGPA cannot be definitively finalized.
                    if (!anyPending && totalCreds > 0) {
                        sheet.sgpa = Math.round((weightedGradePointSum / totalCreds) * 100.0) / 100.0;
                        sheet.equivalentPercentage = Math.round((sheet.sgpa * 10.0) * 100.0) / 100.0;
                    } else {
                        sheet.sgpa = null;
                        sheet.equivalentPercentage = null;
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to generate semester mark sheet: " + e.getMessage());
        }

        return sheet;
    }

    // =========================================================================
    // 5. Attendance Defaulters Report
    // =========================================================================

    /**
     * Finds all students with attendance below 65% OR between 65%-74.9% without medical condonation.
     */
    public List<AttendanceDefaulterItem> getAttendanceDefaulters() {
        List<AttendanceDefaulterItem> list = new ArrayList<>();

        String sql =
            "SELECT s.roll_no, s.name, s.department, e.semester, " +
            "       e.course_code, c.title, e.classes_held, e.classes_attended, e.medical_condonation " +
            "FROM enrollments e " +
            "JOIN students s ON e.student_roll_no = s.roll_no " +
            "JOIN courses c ON e.course_code = c.course_code " +
            "WHERE e.classes_held > 0 " +
            "ORDER BY s.department, s.roll_no, e.course_code";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int held = rs.getInt("classes_held");
                int attended = rs.getInt("classes_attended");
                boolean condonation = rs.getBoolean("medical_condonation");
                double pct = ((double) attended / held) * 100.0;

                boolean isDefaulter = false;
                String remark = "";

                if (pct < 65.0) {
                    isDefaulter = true;
                    remark = "Below 65% (Barred - SA)";
                } else if (pct < 75.0 && !condonation) {
                    isDefaulter = true;
                    remark = "65%-74% Lacks Condonation Approval (Barred - SA)";
                }

                if (isDefaulter) {
                    AttendanceDefaulterItem item = new AttendanceDefaulterItem();
                    item.rollNo = rs.getString("roll_no");
                    item.studentName = rs.getString("name");
                    item.department = rs.getString("department");
                    item.semester = rs.getInt("semester");
                    item.courseCode = rs.getString("course_code");
                    item.courseTitle = rs.getString("title");
                    item.classesHeld = held;
                    item.classesAttended = attended;
                    item.attendancePct = pct;
                    item.condonationApproved = condonation;
                    item.statusRemark = remark;
                    list.add(item);
                }
            }

        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to fetch attendance defaulters: " + e.getMessage());
        }

        return list;
    }

    // =========================================================================
    // 6. Department Performance Summary
    // =========================================================================

    /**
     * Generates department-wise pass, fail (RA/SA/AB), pending metrics and average class SGPA.
     */
    public List<DepartmentSummary> getDepartmentPerformanceSummary() {
        Map<String, DepartmentSummary> deptMap = new LinkedHashMap<>();
        Map<String, List<Double>> deptSgpas = new HashMap<>();

        // 1. Fetch all distinct departments from students
        String deptSql = "SELECT DISTINCT department FROM students ORDER BY department";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deptSql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String dept = rs.getString("department");
                DepartmentSummary sum = new DepartmentSummary();
                sum.department = dept;
                deptMap.put(dept, sum);
                deptSgpas.put(dept, new ArrayList<>());
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to fetch departments: " + e.getMessage());
            return new ArrayList<>();
        }

        // 2. Count total students per department
        String studentCountSql = "SELECT department, COUNT(*) as count FROM students GROUP BY department";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(studentCountSql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String dept = rs.getString("department");
                if (deptMap.containsKey(dept)) {
                    deptMap.get(dept).totalStudents = rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to count students by department: " + e.getMessage());
        }

        // 3. Evaluate all students to collect per-department course results and final SGPAs
        String allStudentsSql = "SELECT roll_no, current_semester, department FROM students";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(allStudentsSql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String rollNo = rs.getString("roll_no");
                int sem = rs.getInt("current_semester");
                String dept = rs.getString("department");

                SemesterMarkSheet sheet = generateSemesterMarkSheet(rollNo, sem);
                if (sheet != null && deptMap.containsKey(dept)) {
                    DepartmentSummary sum = deptMap.get(dept);
                    sum.totalEnrollments += sheet.courses.size();

                    for (CourseEvaluationResult eval : sheet.courses) {
                        if ("PASS".equals(eval.resultStatus)) {
                            sum.passedCount++;
                        } else if ("RE-APPEAR (RA)".equals(eval.resultStatus)) {
                            sum.failedRACount++;
                        } else if ("FAIL (SA)".equals(eval.resultStatus)) {
                            sum.shortageSACount++;
                        } else if ("ABSENT (AB)".equals(eval.resultStatus)) {
                            sum.absentABCount++;
                        } else {
                            sum.pendingCount++;
                        }
                    }

                    if (sheet.sgpa != null) {
                        deptSgpas.get(dept).add(sheet.sgpa);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[SQL ERROR] Failed to compute department performance: " + e.getMessage());
        }

        // 4. Calculate Department Average SGPAs
        for (Map.Entry<String, DepartmentSummary> entry : deptMap.entrySet()) {
            DepartmentSummary sum = entry.getValue();
            List<Double> sgpaList = deptSgpas.get(entry.getKey());
            sum.studentsWithFinalSGPA = sgpaList.size();
            if (!sgpaList.isEmpty()) {
                double total = 0.0;
                for (double d : sgpaList) total += d;
                sum.classAverageSGPA = Math.round((total / sgpaList.size()) * 100.0) / 100.0;
            } else {
                sum.classAverageSGPA = 0.0;
            }
        }

        return new ArrayList<>(deptMap.values());
    }
}
