import java.util.List;
import java.util.Scanner;

/**
 * MainApp.java
 * -------------------------------------------------------------------------
 * Interactive Menu-Driven Console Application for:
 * "Development of Kongu Engineering College (R2024) Student Academic &
 *  Continuous Evaluation System using Java JDBC"
 *
 * Implements:
 *  - 13 structured administrative and evaluation menu actions
 *  - Phased incremental assessment mark recording
 *  - Strict attendance gating & medical condonation rules
 *  - R2024 mark sheet formatting with SGPA and equivalent percentage
 *  - Defaulter reporting and departmental performance summaries
 * -------------------------------------------------------------------------
 */
public class MainApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static final StudentDAO studentDAO = new StudentDAO();
    private static final AcademicDAO academicDAO = new AcademicDAO();

    public static void main(String[] args) {
        printWelcomeBanner();

        // Check database connection on startup
        if (!DBConnection.testConnection()) {
            System.out.println("\n[WARNING] Could not immediately connect to the MySQL database!");
            System.out.println("          Please verify credentials in 'db.properties' and ensure");
            System.out.println("          MySQL service is running. You can update credentials or");
            System.out.println("          run 'kec_schema.sql' to initialize the database.\n");
        } else {
            System.out.println("[INFO] Connected to MySQL database successfully! (URL: " + DBConnection.getDbUrl() + ")\n");
        }

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readIntChoice("Enter your choice (0-13): ", 0, 13);
            System.out.println();

            switch (choice) {
                case 1:
                    handleAddStudent();
                    break;
                case 2:
                    handleViewStudents();
                    break;
                case 3:
                    handleAddCourse();
                    break;
                case 4:
                    handleViewCourses();
                    break;
                case 5:
                    handleEnrollAndAttendance();
                    break;
                case 6:
                    handleUpdateAttendance();
                    break;
                case 7:
                    handleEnterCia1();
                    break;
                case 8:
                    handleEnterCia2();
                    break;
                case 9:
                    handleEnterAssignmentQuizLab();
                    break;
                case 10:
                    handleEnterEse();
                    break;
                case 11:
                    handleGenerateMarkSheet();
                    break;
                case 12:
                    handleAttendanceDefaultersReport();
                    break;
                case 13:
                    handleDepartmentPerformanceSummary();
                    break;
                case 0:
                    running = false;
                    System.out.println("============================================================================");
                    System.out.println(" Exiting Kongu Engineering College (R2024) Academic System. Goodbye!");
                    System.out.println("============================================================================");
                    break;
                default:
                    System.out.println("[ERROR] Invalid choice. Please select an option between 0 and 13.");
            }

            if (running) {
                System.out.println("\nPress Enter to return to the main menu...");
                scanner.nextLine();
            }
        }
    }

    // =========================================================================
    // UI Banners & Menu Display
    // =========================================================================

    private static void printWelcomeBanner() {
        System.out.println("============================================================================");
        System.out.println("       KONGU ENGINEERING COLLEGE (AUTONOMOUS) - PERUNDURAI, ERODE           ");
        System.out.println("    Regulation 2024 (R2024) Student Academic & Continuous Evaluation System ");
        System.out.println("============================================================================");
        System.out.println(" Active CIA Formula: " + AcademicDAO.getActiveFormula() + " (Configurable)");
        System.out.println("============================================================================");
    }

    private static void printMainMenu() {
        System.out.println("----------------------------------------------------------------------------");
        System.out.println("                             MAIN MENU SELECTIONS                           ");
        System.out.println("----------------------------------------------------------------------------");
        System.out.println(" [1]  Add student");
        System.out.println(" [2]  View students");
        System.out.println(" [3]  Add course");
        System.out.println(" [4]  View courses");
        System.out.println(" [5]  Enroll student and record attendance");
        System.out.println(" [6]  Update attendance");
        System.out.println(" [7]  Enter / update CIA 1 (Phase 1)");
        System.out.println(" [8]  Enter / update CIA 2 (Phase 1)");
        System.out.println(" [9]  Enter / update Assignment / Quiz / Lab marks (Phase 2)");
        System.out.println(" [10] Enter ESE marks or mark Absent (AB) (Phase 3)");
        System.out.println(" [11] Generate semester mark sheet (SGPA & Equivalent %)");
        System.out.println(" [12] Attendance defaulter report (< 65% or unapproved condonation)");
        System.out.println(" [13] Department performance summary");
        System.out.println(" [0]  Exit application");
        System.out.println("----------------------------------------------------------------------------");
    }

    // =========================================================================
    // Menu Handler Implementations
    // =========================================================================

    // 1. Add Student
    private static void handleAddStudent() {
        System.out.println("--- [1] Add New Student ---");
        String rollNo = readString("Enter Student Roll Number (e.g., 24ITR045): ").toUpperCase();
        if (studentDAO.studentExists(rollNo)) {
            System.out.println("[ERROR] A student with Roll Number '" + rollNo + "' already exists!");
            return;
        }

        String name = readString("Enter Student Name: ");
        String dept = readString("Enter Department (e.g., Information Technology): ");
        int sem = readIntChoice("Enter Current Semester (1-8): ", 1, 8);

        boolean success = studentDAO.addStudent(rollNo, name, dept, sem);
        if (success) {
            System.out.println("[SUCCESS] Student '" + name + "' (" + rollNo + ") added successfully!");
        } else {
            System.out.println("[FAILED] Could not add student. Check database connection and constraints.");
        }
    }

    // 2. View Students
    private static void handleViewStudents() {
        System.out.println("--- [2] Registered Students List ---");
        List<StudentDAO.Student> students = studentDAO.getAllStudents();
        if (students.isEmpty()) {
            System.out.println("[INFO] No students found in the database.");
            return;
        }

        System.out.println("+---------------+--------------------------------+--------------------------------+----------+");
        System.out.printf("| %-13s | %-30s | %-30s | %-8s |\n", "ROLL NUMBER", "STUDENT NAME", "DEPARTMENT", "SEMESTER");
        System.out.println("+---------------+--------------------------------+--------------------------------+----------+");
        for (StudentDAO.Student s : students) {
            System.out.printf("| %-13s | %-30s | %-30s | Sem %-4d |\n",
                    s.getRollNo(), s.getName(), s.getDepartment(), s.getCurrentSemester());
        }
        System.out.println("+---------------+--------------------------------+--------------------------------+----------+");
        System.out.println("Total Students: " + students.size());
    }

    // 3. Add Course
    private static void handleAddCourse() {
        System.out.println("--- [3] Add New Course ---");
        String code = readString("Enter Course Code (e.g., 24ITC31): ").toUpperCase();
        if (studentDAO.courseExists(code)) {
            System.out.println("[ERROR] Course with code '" + code + "' already exists!");
            return;
        }

        String title = readString("Enter Course Title: ");
        int credits = readIntChoice("Enter Course Credits (1-10): ", 1, 10);

        System.out.println("Select Course Type:");
        System.out.println("  1. THEORY    (40% CIA, 60% ESE, Min ESE: 27/60)");
        System.out.println("  2. PRACTICAL (60% CIA, 40% ESE, Min ESE: 20/40)");
        System.out.println("  3. EMBEDDED  (50% CIA, 50% ESE, Min ESE: 22.5/50)");
        int typeChoice = readIntChoice("Enter choice (1-3): ", 1, 3);
        String courseType = (typeChoice == 1) ? "THEORY" : (typeChoice == 2) ? "PRACTICAL" : "EMBEDDED";

        boolean success = studentDAO.addCourse(code, title, credits, courseType);
        if (success) {
            System.out.println("[SUCCESS] Course '" + title + "' (" + code + ") added successfully!");
        } else {
            System.out.println("[FAILED] Could not add course. Check database connection and constraints.");
        }
    }

    // 4. View Courses
    private static void handleViewCourses() {
        System.out.println("--- [4] Curriculum Courses Catalog (R2024) ---");
        List<StudentDAO.Course> courses = studentDAO.getAllCourses();
        if (courses.isEmpty()) {
            System.out.println("[INFO] No courses found in the database.");
            return;
        }

        System.out.println("+---------------+------------------------------------------+---------+------------+-----------------------+");
        System.out.printf("| %-13s | %-40s | %-7s | %-10s | %-21s |\n",
                "COURSE CODE", "COURSE TITLE", "CREDITS", "TYPE", "EVALUATION SCHEME");
        System.out.println("+---------------+------------------------------------------+---------+------------+-----------------------+");
        for (StudentDAO.Course c : courses) {
            String scheme;
            if ("THEORY".equalsIgnoreCase(c.getCourseType())) {
                scheme = "CIA 40% | ESE 60% (>=27)";
            } else if ("PRACTICAL".equalsIgnoreCase(c.getCourseType())) {
                scheme = "CIA 60% | ESE 40% (>=20)";
            } else {
                scheme = "CIA 50% | ESE 50% (>=22.5)";
            }
            System.out.printf("| %-13s | %-40s | %-7d | %-10s | %-21s |\n",
                    c.getCourseCode(), c.getTitle(), c.getCredits(), c.getCourseType(), scheme);
        }
        System.out.println("+---------------+------------------------------------------+---------+------------+-----------------------+");
        System.out.println("Total Courses: " + courses.size());
    }

    // 5. Enroll Student and Record Attendance
    private static void handleEnrollAndAttendance() {
        System.out.println("--- [5] Enroll Student & Record Attendance ---");
        String rollNo = readString("Enter Student Roll Number: ").toUpperCase();
        if (!studentDAO.studentExists(rollNo)) {
            System.out.println("[ERROR] Student '" + rollNo + "' not found! Please register the student first.");
            return;
        }

        String courseCode = readString("Enter Course Code: ").toUpperCase();
        if (!studentDAO.courseExists(courseCode)) {
            System.out.println("[ERROR] Course '" + courseCode + "' not found! Please register the course first.");
            return;
        }

        int sem = readIntChoice("Enter Semester (1-8): ", 1, 8);
        int held = readIntNonNegative("Enter Total Classes Held: ");
        int attended = readIntWithMax("Enter Classes Attended: ", held);

        double pct = (held > 0) ? ((double) attended / held) * 100.0 : 100.0;
        System.out.printf("[INFO] Calculated Attendance: %.2f%%\n", pct);

        boolean condonation = false;
        if (pct >= 65.0 && pct < 75.0) {
            System.out.println("[NOTE] Attendance is between 65% and 74.9%. Medical condonation is required for ESE eligibility.");
            condonation = readBoolean("Is medical condonation approved by Principal/Dean? (Y/N): ");
        } else if (pct < 65.0) {
            System.out.println("[NOTE] Attendance is below 65%. Student is strictly barred from ESE (SA - Shortage of Attendance).");
        }

        boolean success = academicDAO.enrollStudent(rollNo, courseCode, sem, held, attended, condonation);
        if (success) {
            System.out.println("[SUCCESS] Student " + rollNo + " enrolled in " + courseCode + " for Semester " + sem + "!");
        } else {
            System.out.println("[FAILED] Enrollment failed. Check database constraints.");
        }
    }

    // 6. Update Attendance
    private static void handleUpdateAttendance() {
        System.out.println("--- [6] Update Attendance ---");
        String rollNo = readString("Enter Student Roll Number: ").toUpperCase();
        String courseCode = readString("Enter Course Code: ").toUpperCase();
        int sem = readIntChoice("Enter Semester (1-8): ", 1, 8);

        Integer enrollmentId = academicDAO.getEnrollmentId(rollNo, courseCode, sem);
        if (enrollmentId == null) {
            System.out.println("[ERROR] No enrollment record found for " + rollNo + " in " + courseCode + " (Sem " + sem + ").");
            return;
        }

        int held = readIntNonNegative("Enter Updated Total Classes Held: ");
        int attended = readIntWithMax("Enter Updated Classes Attended: ", held);

        double pct = (held > 0) ? ((double) attended / held) * 100.0 : 100.0;
        System.out.printf("[INFO] Updated Attendance: %.2f%%\n", pct);

        boolean condonation = false;
        if (pct >= 65.0 && pct < 75.0) {
            System.out.println("[NOTE] Attendance is in 65%-74.9% range.");
            condonation = readBoolean("Is medical condonation approved? (Y/N): ");
        }

        boolean success = academicDAO.updateAttendance(rollNo, courseCode, sem, held, attended, condonation);
        if (success) {
            System.out.println("[SUCCESS] Attendance updated successfully for " + rollNo + " in " + courseCode + "!");
        } else {
            System.out.println("[FAILED] Failed to update attendance.");
        }
    }

    // 7. Enter / Update CIA 1 (Phase 1)
    private static void handleEnterCia1() {
        System.out.println("--- [7] Phase 1: Enter / Update CIA 1 ---");
        Integer enrollmentId = promptForEnrollment();
        if (enrollmentId == null) return;

        double mark = readDoubleInRange("Enter CIA 1 Mark (0 - 100): ", 0.0, 100.0);
        boolean ok = academicDAO.updateCia1(enrollmentId, mark);
        if (ok) {
            System.out.println("[SUCCESS] CIA 1 mark recorded (" + mark + "/100). Existing marks preserved!");
        } else {
            System.out.println("[FAILED] Could not record CIA 1 mark.");
        }
    }

    // 8. Enter / Update CIA 2 (Phase 1)
    private static void handleEnterCia2() {
        System.out.println("--- [8] Phase 1: Enter / Update CIA 2 ---");
        Integer enrollmentId = promptForEnrollment();
        if (enrollmentId == null) return;

        double mark = readDoubleInRange("Enter CIA 2 Mark (0 - 100): ", 0.0, 100.0);
        boolean ok = academicDAO.updateCia2(enrollmentId, mark);
        if (ok) {
            System.out.println("[SUCCESS] CIA 2 mark recorded (" + mark + "/100). Existing marks preserved!");
        } else {
            System.out.println("[FAILED] Could not record CIA 2 mark.");
        }
    }

    // 9. Enter / Update Assignment / Quiz / Lab marks (Phase 2)
    private static void handleEnterAssignmentQuizLab() {
        System.out.println("--- [9] Phase 2: Enter / Update Assignment / Quiz / Lab Marks ---");
        Integer enrollmentId = promptForEnrollment();
        if (enrollmentId == null) return;

        double mark = readDoubleInRange("Enter Assignment / Quiz / Lab Mark (0 - 100): ", 0.0, 100.0);
        boolean ok = academicDAO.updateAssignmentQuizLab(enrollmentId, mark);
        if (ok) {
            System.out.println("[SUCCESS] Assignment / Quiz / Lab mark recorded (" + mark + "/100). Existing marks preserved!");
        } else {
            System.out.println("[FAILED] Could not record mark.");
        }
    }

    // 10. Enter ESE Marks or Mark AB (Phase 3)
    private static void handleEnterEse() {
        System.out.println("--- [10] Phase 3: Enter ESE Marks or Mark Absent (AB) ---");
        Integer enrollmentId = promptForEnrollment();
        if (enrollmentId == null) return;

        boolean isAbsent = readBoolean("Was the student ABSENT for the End Semester Exam? (Y/N): ");
        Double eseMark = null;
        if (!isAbsent) {
            eseMark = readDoubleInRange("Enter ESE Mark (0 - 100): ", 0.0, 100.0);
        }

        boolean ok = academicDAO.updateEse(enrollmentId, eseMark, isAbsent);
        if (ok) {
            if (isAbsent) {
                System.out.println("[SUCCESS] Student marked as ABSENT (AB) for ESE. Grade will reflect AB (GP: 0).");
            } else {
                System.out.println("[SUCCESS] ESE mark recorded (" + eseMark + "/100). Previous CIA marks preserved!");
            }
        } else {
            System.out.println("[FAILED] Could not update ESE assessment.");
        }
    }

    // 11. Generate Semester Mark Sheet
    private static void handleGenerateMarkSheet() {
        System.out.println("--- [11] Generate Semester Mark Sheet (R2024) ---");
        String rollNo = readString("Enter Student Roll Number: ").toUpperCase();
        int sem = readIntChoice("Enter Semester (1-8): ", 1, 8);

        AcademicDAO.SemesterMarkSheet sheet = academicDAO.generateSemesterMarkSheet(rollNo, sem);
        if (sheet == null) {
            System.out.println("[ERROR] Student '" + rollNo + "' not found in the records.");
            return;
        }

        if (sheet.courses.isEmpty()) {
            System.out.println("[INFO] No courses enrolled for student " + rollNo + " in Semester " + sem + ".");
            return;
        }

        // Print Institutional Mark Sheet
        System.out.println();
        System.out.println("==========================================================================================");
        System.out.println("                 KONGU ENGINEERING COLLEGE (AUTONOMOUS) - ERODE - 638 060                 ");
        System.out.println("                      AFFILIATED TO ANNA UNIVERSITY | REGULATION 2024                     ");
        System.out.println("                            GRADE SHEET - SEMESTER EVALUATION                             ");
        System.out.println("==========================================================================================");
        System.out.printf(" Roll Number : %-25s Department : %s\n", sheet.rollNo, sheet.department);
        System.out.printf(" Student Name: %-25s Semester   : %d\n", sheet.studentName, sheet.semester);
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.printf("| %-7s | %-18s | %-4s | %-2s | %-5s | %-4s | %-4s | %-4s | %-2s | %-2s | %-7s |\n",
                "CODE", "COURSE TITLE", "TYPE", "CR", "ATT%", "CIA", "ESE", "TOT", "GR", "GP", "RESULT");
        System.out.println("------------------------------------------------------------------------------------------");

        for (AcademicDAO.CourseEvaluationResult c : sheet.courses) {
            String attdStr = String.format("%.1f%%", c.attendancePct);
            String ciaStr = (c.ciaWeighted != null) ? String.format("%.1f", c.ciaWeighted) : "--";
            String eseStr = c.isAbsentEse ? "AB" : (c.eseWeighted != null ? String.format("%.1f", c.eseWeighted) : "--");
            String totStr = (c.totalMarks != null) ? String.format("%.1f", c.totalMarks) : "--";
            String grStr = ("PENDING".equals(c.grade)) ? "--" : c.grade;
            String gpStr = (c.gradePoint != null) ? String.valueOf(c.gradePoint) : "--";
            String resStr;
            if ("PASS".equals(c.resultStatus)) resStr = "PASS";
            else if ("RE-APPEAR (RA)".equals(c.resultStatus)) resStr = "RA";
            else if ("FAIL (SA)".equals(c.resultStatus)) resStr = "SA";
            else if ("ABSENT (AB)".equals(c.resultStatus)) resStr = "AB";
            else resStr = "PENDING";

            String shortType;
            if ("THEORY".equalsIgnoreCase(c.courseType)) shortType = "THRY";
            else if ("PRACTICAL".equalsIgnoreCase(c.courseType)) shortType = "PRAC";
            else shortType = "EMBD";

            System.out.printf("| %-7s | %-18s | %-4s | %2d | %5s | %4s | %4s | %4s | %-2s | %2s | %-7s |\n",
                    c.courseCode,
                    truncate(c.title, 18),
                    shortType,
                    c.credits,
                    attdStr,
                    ciaStr,
                    eseStr,
                    totStr,
                    grStr,
                    gpStr,
                    resStr);

            if (c.failureReason != null && !c.failureReason.isEmpty() && !"PENDING".equals(c.grade)) {
                System.out.printf("|   -> Remark: %-73s |\n", truncate(c.failureReason, 73));
            }
        }
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.printf(" Registered Credits : %-2d   Earned Credits : %-2d   Passed: %-2d  Failed: %-2d  Pending: %-2d\n",
                sheet.totalRegisteredCredits, sheet.earnedCredits,
                sheet.passedCoursesCount, sheet.failedCoursesCount, sheet.pendingCoursesCount);

        if (sheet.isPending) {
            System.out.println(" SGPA               : PENDING (Cannot finalize SGPA while some courses are pending)");
            System.out.println(" Equivalent %       : PENDING");
        } else {
            System.out.printf(" Semester GPA (SGPA): %.2f / 10.00\n", sheet.sgpa);
            System.out.printf(" Equivalent Mark %%  : %.2f%%\n", sheet.equivalentPercentage);
        }
        System.out.println("==========================================================================================");
        System.out.println(" Grading: O (91-100, GP 10) | A+ (81-90, GP 9) | A (71-80, GP 8) | B+ (61-70, GP 7) | B (50-60, GP 6)");
        System.out.println(" Arrears: RA (Re-Appear, GP 0) | SA (Shortage of Attendance, GP 0) | AB (Absent, GP 0)");
        System.out.println(" Passing: Total >= 50 AND ESE Min met (Theory: 27/60, Practical: 20/40, Embedded: 22.5/50)");
        System.out.println("==========================================================================================");
    }

    // 12. Attendance Defaulter Report
    private static void handleAttendanceDefaultersReport() {
        System.out.println("--- [12] Attendance Defaulter Report (KEC R2024 Rule) ---");
        System.out.println("Criteria: Attendance < 65% OR (65% <= Attendance < 75% without Medical Condonation Approval)");
        System.out.println();

        List<AcademicDAO.AttendanceDefaulterItem> defaulters = academicDAO.getAttendanceDefaulters();
        if (defaulters.isEmpty()) {
            System.out.println("[CONGRATULATIONS] No attendance defaulters found! All students meet eligibility requirements.");
            return;
        }

        System.out.println("+---------------+----------------------+----------------------+---------+-------------+------+------+-------+---------------------------------------+");
        System.out.printf("| %-13s | %-20s | %-20s | %-7s | %-11s | %-4s | %-4s | %-5s | %-37s |\n",
                "ROLL NUMBER", "NAME", "DEPARTMENT", "SEM", "COURSE", "HELD", "ATTD", "ATTD%", "REMARK / STATUS");
        System.out.println("+---------------+----------------------+----------------------+---------+-------------+------+------+-------+---------------------------------------+");

        for (AcademicDAO.AttendanceDefaulterItem d : defaulters) {
            System.out.printf("| %-13s | %-20s | %-20s | Sem %-3d | %-11s | %-4d | %-4d | %5.1f%% | %-37s |\n",
                    d.rollNo,
                    truncate(d.studentName, 20),
                    truncate(d.department, 20),
                    d.semester,
                    d.courseCode,
                    d.classesHeld,
                    d.classesAttended,
                    d.attendancePct,
                    truncate(d.statusRemark, 37));
        }
        System.out.println("+---------------+----------------------+----------------------+---------+-------------+------+------+-------+---------------------------------------+");
        System.out.println("Total Defaulter Cases: " + defaulters.size());
        System.out.println("[ACTION REQUIRED] Students with 'SA' status are barred from appearing in End Semester Examinations.");
    }

    // 13. Department Performance Summary
    private static void handleDepartmentPerformanceSummary() {
        System.out.println("--- [13] Department Performance Summary ---");
        List<AcademicDAO.DepartmentSummary> summaries = academicDAO.getDepartmentPerformanceSummary();

        if (summaries.isEmpty()) {
            System.out.println("[INFO] No department performance records available.");
            return;
        }

        System.out.println("+--------------------------------+----------+---------+--------+--------+--------+--------+---------+-------------+");
        System.out.printf("| %-30s | %-8s | %-7s | %-6s | %-6s | %-6s | %-6s | %-7s | %-11s |\n",
                "DEPARTMENT", "STUDENTS", "ENROLLS", "PASS", "FAIL(RA)", "DEF(SA)", "ABS(AB)", "PENDING", "CLASS SGPA");
        System.out.println("+--------------------------------+----------+---------+--------+--------+--------+--------+---------+-------------+");

        for (AcademicDAO.DepartmentSummary s : summaries) {
            String sgpaStr = (s.studentsWithFinalSGPA > 0) ? String.format("%.2f", s.classAverageSGPA) : "Pending";
            System.out.printf("| %-30s | %-8d | %-7d | %-6d | %-8d | %-8d | %-8d | %-7d | %-11s |\n",
                    truncate(s.department, 30),
                    s.totalStudents,
                    s.totalEnrollments,
                    s.passedCount,
                    s.failedRACount,
                    s.shortageSACount,
                    s.absentABCount,
                    s.pendingCount,
                    sgpaStr);
        }
        System.out.println("+--------------------------------+----------+---------+--------+--------+--------+--------+---------+-------------+");
        System.out.println("* Class Average SGPA is computed exclusively from students whose semester evaluations are 100% complete.");
    }

    // =========================================================================
    // Console Input Helper Utilities
    // =========================================================================

    private static Integer promptForEnrollment() {
        String rollNo = readString("Enter Student Roll Number: ").toUpperCase();
        String courseCode = readString("Enter Course Code: ").toUpperCase();
        int sem = readIntChoice("Enter Semester (1-8): ", 1, 8);

        Integer enrollmentId = academicDAO.getEnrollmentId(rollNo, courseCode, sem);
        if (enrollmentId == null) {
            System.out.println("[ERROR] No enrollment record found for " + rollNo + " in " + courseCode + " (Sem " + sem + ").");
            System.out.println("        Please enroll the student first (Option 5).");
            return null;
        }
        return enrollmentId;
    }

    private static String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) return line;
            System.out.println("[ERROR] Input cannot be blank. Please enter a valid value.");
        }
    }

    private static int readIntChoice(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) return val;
                System.out.println("[ERROR] Please enter an integer between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid input. Please enter a valid integer.");
            }
        }
    }

    private static int readIntNonNegative(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val >= 0) return val;
                System.out.println("[ERROR] Value must be non-negative (>= 0).");
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid number format.");
            }
        }
    }

    private static int readIntWithMax(String prompt, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val >= 0 && val <= max) return val;
                System.out.println("[ERROR] Value must be between 0 and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid number format.");
            }
        }
    }

    private static double readDoubleInRange(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                double val = Double.parseDouble(input);
                if (val >= min && val <= max) return val;
                System.out.printf("[ERROR] Mark must be between %.1f and %.1f.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Invalid decimal number. Please try again.");
            }
        }
    }

    private static boolean readBoolean(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("Y") || input.equals("YES") || input.equals("TRUE") || input.equals("1")) {
                return true;
            }
            if (input.equals("N") || input.equals("NO") || input.equals("FALSE") || input.equals("0")) {
                return false;
            }
            System.out.println("[ERROR] Please enter 'Y' for Yes or 'N' for No.");
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
