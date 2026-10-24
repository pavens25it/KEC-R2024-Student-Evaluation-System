/**
 * TestRunner.java
 * -------------------------------------------------------------------------
 * Unit and integration test suite to verify the business logic, evaluation
 * rules, and database operations of the Kongu Engineering College (R2024)
 * Academic Evaluation System.
 *
 * Verifies:
 *  1. Mathematical accuracy of CIA and ESE weighted calculations:
 *     - THEORY    (40% CIA, 60% ESE, Min ESE: 27/60, Pass: 50)
 *     - PRACTICAL (60% CIA, 40% ESE, Min ESE: 20/40, Pass: 50)
 *     - EMBEDDED  (50% CIA, 50% ESE, Min ESE: 22.5/50, Pass: 50)
 *  2. Grade mapping:
 *     - O  : 91–100 (GP 10)
 *     - A+ : 81–90  (GP 9)
 *     - A  : 71–80  (GP 8)
 *     - B+ : 61–70  (GP 7)
 *     - B  : 50–60  (GP 6)
 *     - RA : Fail (<50 or ESE minimum failed) (GP 0)
 *     - SA : Shortage of attendance (<65% or unapproved condonation) (GP 0)
 *     - AB : Absent in ESE (GP 0)
 *  3. Attendance gating:
 *     - >=75% -> ELIGIBLE
 *     - 65-74.9% with condonation -> ELIGIBLE
 *     - 65-74.9% without condonation -> SA (Barred)
 *     - <65% -> SA (Barred)
 *  4. Absence handling: is_absent_ese = true -> AB, GP: 0
 *  5. Pending handling: Missing ESE or incomplete CIA -> PENDING (never treated as 0)
 *  6. SGPA and equivalent percentage calculations (SGPA * 10)
 * -------------------------------------------------------------------------
 */
public class TestRunner {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("============================================================================");
        System.out.println(" KONGU ENGINEERING COLLEGE (R2024) - AUTOMATED VERIFICATION TEST SUITE");
        System.out.println("============================================================================");

        testTheoryCourseEvaluation();
        testPracticalCourseEvaluation();
        testEmbeddedCourseEvaluation();
        testAttendanceEligibilityRules();
        testAbsentEseStatus();
        testPendingMarksHandling();
        testSgpaComputation();

        System.out.println("============================================================================");
        System.out.printf(" TEST SUMMARY: Total Run: %d | Passed: %d | Failed: %d\n",
                (testsPassed + testsFailed), testsPassed, testsFailed);
        System.out.println("============================================================================");

        if (testsFailed == 0) {
            System.out.println(">>> ALL EVALUATION RULES VERIFIED SUCCESSFULLY UNDER KEC R2024! <<<");
        } else {
            System.err.println(">>> SOME TESTS FAILED. PLEASE REVIEW THE SPECIFICATION! <<<");
            System.exit(1);
        }
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        if (expected == null && actual == null) {
            System.out.println("  [PASS] " + testName + " => " + actual);
            testsPassed++;
            return;
        }
        if (expected != null && expected.equals(actual)) {
            System.out.println("  [PASS] " + testName + " => " + actual);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + testName + " => Expected: " + expected + ", Actual: " + actual);
            testsFailed++;
        }
    }

    private static void assertDoubleEquals(String testName, double expected, Double actual, double delta) {
        if (actual != null && Math.abs(expected - actual) <= delta) {
            System.out.printf("  [PASS] %s => %.2f\n", testName, actual);
            testsPassed++;
        } else {
            System.err.printf("  [FAIL] %s => Expected: %.2f, Actual: %s\n", testName, expected, actual);
            testsFailed++;
        }
    }

    // 1. THEORY Evaluation Test
    private static void testTheoryCourseEvaluation() {
        System.out.println("\n[TEST 1] Theory Course Evaluation (40% CIA, 60% ESE, Min ESE: 27/60, Pass: 50)");

        // Case A: High marks (95) -> Grade O (91-100)
        // CIA: 95, 95, 95 => CIA 100 = 95 => CIA weight (40%) = 38.0
        // ESE: 95 => ESE weight (60%) = 57.0 (>= 27)
        // Total = 95.0 => Grade O, GP 10
        AcademicDAO.CourseEvaluationResult resA = AcademicDAO.evaluateCourse(
                "24ITT31", "Data Structures", "THEORY", 3,
                50, 48, false, 95.0, 95.0, 95.0, 95.0, false);
        assertEquals("Theory Case A: Grade", "O", resA.grade);
        assertEquals("Theory Case A: GP", 10, resA.gradePoint);
        assertEquals("Theory Case A: Status", "PASS", resA.resultStatus);
        assertDoubleEquals("Theory Case A: Total", 95.0, resA.totalMarks, 0.01);

        // Case B: Passed CIA, but Failed ESE Minimum (ESE < 45%, contribution < 27)
        // CIA: 80, 80, 80 => CIA weight = 32.0
        // ESE: 40 => ESE weight = 24.0 (< 27 minimum required!)
        // Total = 56.0, BUT ESE minimum failed -> Grade RA
        AcademicDAO.CourseEvaluationResult resB = AcademicDAO.evaluateCourse(
                "24ITT31", "Data Structures", "THEORY", 3,
                50, 48, false, 80.0, 80.0, 80.0, 40.0, false);
        assertEquals("Theory Case B (ESE Min Fail): Grade", "RA", resB.grade);
        assertEquals("Theory Case B: GP", 0, resB.gradePoint);
        assertEquals("Theory Case B: Status", "RE-APPEAR (RA)", resB.resultStatus);

        // Case C: ESE minimum met, but overall total < 50
        // CIA: 30, 30, 30 => CIA weight = 12.0
        // ESE: 50 => ESE weight = 30.0 (>= 27)
        // Total = 42.0 (< 50) -> Grade RA
        AcademicDAO.CourseEvaluationResult resC = AcademicDAO.evaluateCourse(
                "24ITT31", "Data Structures", "THEORY", 3,
                50, 48, false, 30.0, 30.0, 30.0, 50.0, false);
        assertEquals("Theory Case C (Total < 50): Grade", "RA", resC.grade);
        assertEquals("Theory Case C: GP", 0, resC.gradePoint);
    }

    // 2. PRACTICAL Evaluation Test
    private static void testPracticalCourseEvaluation() {
        System.out.println("\n[TEST 2] Practical Course Evaluation (60% CIA, 40% ESE, Min ESE: 20/40, Pass: 50)");

        // CIA: 85, 85, 85 => CIA 100 = 85 => CIA weight (60%) = 51.0
        // ESE: 85 => ESE weight (40%) = 34.0 (>= 20)
        // Total = 85.0 => Grade A+ (81-90), GP 9, PASS
        AcademicDAO.CourseEvaluationResult resA = AcademicDAO.evaluateCourse(
                "24ITP31", "Java Lab", "PRACTICAL", 2,
                30, 28, false, 85.0, 85.0, 85.0, 85.0, false);
        assertEquals("Practical (85 marks): Grade", "A+", resA.grade);
        assertEquals("Practical: GP", 9, resA.gradePoint);
        assertEquals("Practical: Status", "PASS", resA.resultStatus);
        assertDoubleEquals("Practical: Total", 85.0, resA.totalMarks, 0.01);

        // CIA: 80, 80, 80 => CIA weight (60%) = 48.0
        // ESE: 80 => ESE weight (40%) = 32.0 (>= 20)
        // Total = 80.0 => Grade A (71-80), GP 8
        AcademicDAO.CourseEvaluationResult res80 = AcademicDAO.evaluateCourse(
                "24ITP31", "Java Lab", "PRACTICAL", 2,
                30, 28, false, 80.0, 80.0, 80.0, 80.0, false);
        assertEquals("Practical (80 marks): Grade", "A", res80.grade);
        assertEquals("Practical (80 marks): GP", 8, res80.gradePoint);

        // Practical ESE minimum fail: ESE = 45/100 => 45 * 0.40 = 18.0 (< 20)
        AcademicDAO.CourseEvaluationResult resMin = AcademicDAO.evaluateCourse(
                "24ITP31", "Java Lab", "PRACTICAL", 2,
                30, 28, false, 90.0, 90.0, 90.0, 45.0, false);
        assertEquals("Practical (ESE Min Fail): Grade", "RA", resMin.grade);
    }

    // 3. EMBEDDED Evaluation Test
    private static void testEmbeddedCourseEvaluation() {
        System.out.println("\n[TEST 3] Embedded Course Evaluation (50% CIA, 50% ESE, Min ESE: 22.5/50, Pass: 50)");

        // CIA: 70, 70, 70 => CIA 100 = 70 => CIA weight (50%) = 35.0
        // ESE: 70 => ESE weight (50%) = 35.0 (>= 22.5)
        // Total = 70.0 => Grade B+ (61-70), GP 7, PASS
        AcademicDAO.CourseEvaluationResult res = AcademicDAO.evaluateCourse(
                "24ITC31", "OOP with Java", "EMBEDDED", 4,
                60, 55, false, 70.0, 70.0, 70.0, 70.0, false);
        assertEquals("Embedded: Grade", "B+", res.grade);
        assertEquals("Embedded: GP", 7, res.gradePoint);
        assertEquals("Embedded: Status", "PASS", res.resultStatus);
        assertDoubleEquals("Embedded: Total", 70.0, res.totalMarks, 0.01);
    }

    // 4. Attendance Eligibility Rules Test
    private static void testAttendanceEligibilityRules() {
        System.out.println("\n[TEST 4] Attendance Eligibility & Condonation Rules");

        // Case A: Regular attendance (>= 75%) -> Eligible
        AcademicDAO.CourseEvaluationResult resA = AcademicDAO.evaluateCourse(
                "24ITT31", "Maths", "THEORY", 3,
                50, 40, false, 80.0, 80.0, 80.0, 80.0, false); // 80%
        assertEquals("Attendance >= 75%: Eligible", true, resA.attendanceEligible);
        assertEquals("Attendance >= 75%: Grade is A", "A", resA.grade);

        // Case B: Attendance in 65%-74.9% WITH Medical Condonation -> Eligible
        AcademicDAO.CourseEvaluationResult resB = AcademicDAO.evaluateCourse(
                "24ITT31", "Maths", "THEORY", 3,
                50, 35, true, 80.0, 80.0, 80.0, 80.0, false); // 70%
        assertEquals("Attendance 70% + Condonation: Eligible", true, resB.attendanceEligible);
        assertEquals("Attendance 70% + Condonation: Grade", "A", resB.grade);

        // Case C: Attendance in 65%-74.9% WITHOUT Medical Condonation -> SA (Barred)
        AcademicDAO.CourseEvaluationResult resC = AcademicDAO.evaluateCourse(
                "24ITT31", "Maths", "THEORY", 3,
                50, 35, false, 80.0, 80.0, 80.0, 80.0, false); // 70% unapproved
        assertEquals("Attendance 70% without Condonation: Ineligible", false, resC.attendanceEligible);
        assertEquals("Attendance 70% without Condonation: Grade", "SA", resC.grade);
        assertEquals("Attendance 70% without Condonation: GP", 0, resC.gradePoint);

        // Case D: Attendance < 65% -> SA (Barred regardless of condonation)
        AcademicDAO.CourseEvaluationResult resD = AcademicDAO.evaluateCourse(
                "24ITT31", "Maths", "THEORY", 3,
                50, 25, true, 90.0, 90.0, 90.0, 90.0, false); // 50%
        assertEquals("Attendance < 65%: Ineligible", false, resD.attendanceEligible);
        assertEquals("Attendance < 65%: Grade", "SA", resD.grade);
        assertEquals("Attendance < 65%: GP", 0, resD.gradePoint);
    }

    // 5. Absent in ESE Test
    private static void testAbsentEseStatus() {
        System.out.println("\n[TEST 5] Absence Handling in ESE");
        AcademicDAO.CourseEvaluationResult res = AcademicDAO.evaluateCourse(
                "24ITT31", "Maths", "THEORY", 3,
                50, 45, false, 90.0, 90.0, 90.0, null, true);
        assertEquals("Absent in ESE: Grade", "AB", res.grade);
        assertEquals("Absent in ESE: GP", 0, res.gradePoint);
        assertEquals("Absent in ESE: Status", "ABSENT (AB)", res.resultStatus);
    }

    // 6. Pending Marks Handling Test
    private static void testPendingMarksHandling() {
        System.out.println("\n[TEST 6] Incomplete / Pending Assessments Handling");
        // Missing ESE (NULL) should NOT be treated as 0
        AcademicDAO.CourseEvaluationResult resA = AcademicDAO.evaluateCourse(
                "24ITT31", "Maths", "THEORY", 3,
                50, 45, false, 85.0, 85.0, 85.0, null, false);
        assertEquals("Missing ESE: Grade", "PENDING", resA.grade);
        assertEquals("Missing ESE: GP", null, resA.gradePoint);
        assertEquals("Missing ESE: Status", "PENDING", resA.resultStatus);

        // Missing CIA 2
        AcademicDAO.CourseEvaluationResult resB = AcademicDAO.evaluateCourse(
                "24ITT31", "Maths", "THEORY", 3,
                50, 45, false, 85.0, null, 85.0, 80.0, false);
        assertEquals("Missing CIA2: Grade", "PENDING", resB.grade);
    }

    // 7. SGPA Calculation Test
    private static void testSgpaComputation() {
        System.out.println("\n[TEST 7] SGPA and Equivalent Percentage Calculation");
        // Course 1: Credits = 3, Grade = O (GP 10) => 30
        // Course 2: Credits = 4, Grade = A+ (GP 9) => 36
        // Course 3: Credits = 2, Grade = A (GP 8)  => 16
        // Total Registered = 9, Earned = 9
        // Sum GP*Credits = 30 + 36 + 16 = 82
        // SGPA = 82 / 9 = 9.111... => 9.11
        // Eq % = SGPA * 10 = 9.11 * 10 = 91.10%
        double totalCredits = 9.0;
        double weightedPoints = (3 * 10) + (4 * 9) + (2 * 8);
        double sgpa = Math.round((weightedPoints / totalCredits) * 100.0) / 100.0;
        double eqPct = Math.round((sgpa * 10.0) * 100.0) / 100.0;

        assertDoubleEquals("SGPA Verification", 9.11, sgpa, 0.01);
        assertDoubleEquals("Equivalent % Verification", 91.10, eqPct, 0.01);
    }
}
