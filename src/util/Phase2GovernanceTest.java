package util;

import controller.CourseController;
import controller.StudentController;
import model.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Phase2GovernanceTest - Automated verification of Phase 2 governance rules:
 * 1. Term lifecycles and non-destructive reopening
 * 2. Audit trail recording for authorized actions
 * 3. Duplicate enrollment prevention in DB and Java
 * 4. Configurable course prerequisites
 * 5. Configurable lecturer credit hours workload
 * 6. Score finalization and authorized correction
 */
public class Phase2GovernanceTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("  STARTING PHASE 2 GOVERNANCE & LIFECYCLE TEST SUITE");
        System.out.println("============================================================");

        // Authenticate test session as ADMIN
        SessionManager.getInstance().login(1, "admin_tester", "Admin Tester", SessionManager.Role.ADMIN, null, null);

        StudentController studentCtrl = new StudentController();
        CourseController courseCtrl = new CourseController();
        AcademicTermDAO termDAO = new AcademicTermDAO();
        AcademicAuditDAO auditDAO = new AcademicAuditDAO();
        StudentDAO studentDAO = new StudentDAO();
        CourseDAO courseDAO = new CourseDAO();
        ScoreDAO scoreDAO = new ScoreDAO();
        LecturerDAO lecturerDAO = new LecturerDAO();

        testDuplicateEnrollmentRules(studentCtrl, studentDAO);
        testTermLifecycleAndReopen(studentCtrl, termDAO, auditDAO);
        testAuthorizedExceptionRegistration(studentCtrl, auditDAO);
        testAuthorizedCourseDeregistration(studentCtrl, auditDAO);
        testConfigurablePrerequisites(studentCtrl);
        testConfigurableLecturerWorkload(courseCtrl, lecturerDAO);
        testScoreFinalizationAndCorrection(studentCtrl, scoreDAO, auditDAO);

        System.out.println("============================================================");
        System.out.printf("  PHASE 2 TEST RESULTS: %d PASSED, %d FAILED%n", passed, failed);
        System.out.println("============================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testDuplicateEnrollmentRules(StudentController studentCtrl, StudentDAO studentDAO) {
        System.out.print("[TEST 1] Duplicate Enrollment Enforcement (Java & DB)... ");
        try {
            // Find student 1 and course 1
            List<Student> students = studentCtrl.getAllStudents();
            if (students.isEmpty()) {
                throw new IllegalStateException("No students found in DB for testing.");
            }
            int sId = students.get(0).getId();

            // Try registering for an already enrolled course or register twice
            String testYear = "2024/2025";
            int testSem = 1;

            // Deregister first if already there to have clean state
            studentDAO.deregisterCourse(sId, 1, testYear, testSem);

            // 1st registration should succeed
            studentCtrl.registerCourse(sId, 1, testYear, testSem);

            // 2nd registration should be intercepted by Java layer
            boolean javaCaught = false;
            try {
                studentCtrl.registerCourse(sId, 1, testYear, testSem);
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("already registered") || e.getMessage().contains("Duplicate")) {
                    javaCaught = true;
                }
            }

            // Also test DB direct call to ensure DB stored procedure/unique key blocks it
            boolean dbCaught = false;
            try {
                studentDAO.registerCourse(sId, 1, testYear, testSem);
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate enrollment") || e.getErrorCode() == 1062) {
                    dbCaught = true;
                }
            }

            if (javaCaught && dbCaught) {
                pass("Both Java layer and DB layer enforced duplicate enrollment rules cleanly.");
            } else {
                fail("Duplicate check missed: javaCaught=" + javaCaught + ", dbCaught=" + dbCaught);
            }
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    private static void testTermLifecycleAndReopen(StudentController studentCtrl, AcademicTermDAO termDAO, AcademicAuditDAO auditDAO) {
        System.out.print("[TEST 2] Non-Destructive Academic Term Lifecycle & Reopening... ");
        try {
            AcademicTerm closedTerm = termDAO.findByYearAndSemester("2023/2024", 2);
            if (closedTerm == null) {
                throw new IllegalStateException("Term 2023/2024 Sem 2 not found.");
            }

            // Verify standard registration into CLOSED term fails
            boolean closedBlocked = false;
            try {
                studentCtrl.registerCourse(1, 2, "2023/2024", 2);
            } catch (IllegalStateException e) {
                if (e.getMessage().contains("closed")) {
                    closedBlocked = true;
                }
            }

            // Reopen term non-destructively with documented justification
            String reopenReason = "Senate approved extension for incomplete registrations";
            studentCtrl.updateTermStatus(closedTerm.getId(), "ACTIVE", reopenReason);

            AcademicTerm reopened = termDAO.findById(closedTerm.getId());
            boolean statusUpdated = "ACTIVE".equalsIgnoreCase(reopened.getStatus());

            // Check audit log for reopen action
            List<AcademicAuditLog> logs = auditDAO.getRecentLogs(10);
            boolean auditLogged = logs.stream().anyMatch(l ->
                    "TERM".equals(l.getEntityType()) &&
                    "TERM_STATUS_CHANGE".equals(l.getAction()) &&
                    reopenReason.equals(l.getReason())
            );

            // Revert back to CLOSED
            studentCtrl.updateTermStatus(closedTerm.getId(), "CLOSED", "Restoring term closed status after test");

            if (closedBlocked && statusUpdated && auditLogged) {
                pass("Term closure enforced; term reopened non-destructively with immutable audit record.");
            } else {
                fail("Lifecycle test failed: closedBlocked=" + closedBlocked + ", statusUpdated=" + statusUpdated + ", auditLogged=" + auditLogged);
            }
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    private static void testAuthorizedExceptionRegistration(StudentController studentCtrl, AcademicAuditDAO auditDAO) {
        System.out.print("[TEST 3] Authorized Exception Registration with Audit Trail... ");
        try {
            // Course 3 in closed term 2023/2024 Sem 2
            int sId = 1;
            int cId = 3;
            String year = "2023/2024";
            int sem = 2;
            String exceptionReason = "Approved late enrollment granted by Academic Board memo #412";

            new StudentDAO().deregisterCourse(sId, cId, year, sem);

            // Register with authorized exception
            studentCtrl.registerCourseWithException(sId, cId, year, sem, exceptionReason);

            // Verify course is enrolled
            List<Course> enrolled = studentCtrl.getEnrolledCourses(sId, year, sem);
            boolean isEnrolled = enrolled.stream().anyMatch(c -> c.getId() == cId);

            // Verify audit record exists
            List<AcademicAuditLog> logs = auditDAO.getLogsForStudent(sId);
            boolean auditFound = logs.stream().anyMatch(l ->
                    "AUTHORIZED_LATE_REGISTRATION".equals(l.getAction()) &&
                    l.getCourseId() != null && l.getCourseId() == cId &&
                    exceptionReason.equals(l.getReason()) &&
                    "admin_tester".equals(l.getAuthorizedUser())
            );

            if (isEnrolled && auditFound) {
                pass("Authorized exception enrollment succeeded and preserved audit log with authorizer and reason.");
            } else {
                fail("Exception registration verification failed: isEnrolled=" + isEnrolled + ", auditFound=" + auditFound);
            }
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    private static void testAuthorizedCourseDeregistration(StudentController studentCtrl, AcademicAuditDAO auditDAO) {
        System.out.print("[TEST 4] Authorized Course Deregistration (Drop) with Audit Log... ");
        try {
            int sId = 1;
            int cId = 3;
            String year = "2023/2024";
            int sem = 2;
            String dropReason = "Medical leave approved by Dean of Students";

            if (!studentCtrl.getEnrolledCourses(sId, year, sem).stream().anyMatch(c -> c.getId() == cId)) {
                studentCtrl.registerCourseWithException(sId, cId, year, sem, "Pre-test setup enrollment");
            }

            studentCtrl.deregisterCourseWithException(sId, cId, year, sem, dropReason);

            // Verify course is removed
            List<Course> enrolled = studentCtrl.getEnrolledCourses(sId, year, sem);
            boolean stillEnrolled = enrolled.stream().anyMatch(c -> c.getId() == cId);

            // Verify audit log captured drop action
            List<AcademicAuditLog> logs = auditDAO.getLogsForStudent(sId);
            boolean auditFound = logs.stream().anyMatch(l ->
                    "AUTHORIZED_COURSE_DROP".equals(l.getAction()) &&
                    l.getCourseId() != null && l.getCourseId() == cId &&
                    dropReason.equals(l.getReason()) &&
                    "DROPPED".equals(l.getNewValue())
            );

            if (!stillEnrolled && auditFound) {
                pass("Course deregistered and captured in academic_audit_log.");
            } else {
                fail("Deregistration failed: stillEnrolled=" + stillEnrolled + ", auditFound=" + auditFound);
            }
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    private static void testConfigurablePrerequisites(StudentController studentCtrl) {
        System.out.print("[TEST 5] Configurable Course Prerequisites... ");
        try {
            // CS201 requires CS101. Let's test with a student who hasn't completed CS101.
            int randSuffix = (int)(System.currentTimeMillis() % 890 + 100);
            String regNum = "CS/" + String.format("%03d", randSuffix) + "/2024";
            Student s = studentCtrl.registerStudent("Test", "PrereqUser", "prereq" + System.currentTimeMillis() + "@test.com",
                    "+254700998877", "Male", LocalDate.of(2002, 5, 10), "Nairobi", regNum,
                    "Computer Science", 2, 1, LocalDate.now());

            Course cs201 = studentCtrl.getCourseByCode("CS201");
            if (cs201 == null) {
                throw new IllegalStateException("CS201 not found.");
            }

            // 1. When ENFORCE_PREREQUISITES = true, registration must be rejected
            SystemConfig.setEnforcePrerequisites(true);
            boolean rejected = false;
            try {
                studentCtrl.registerCourse(s.getId(), cs201.getId(), "2024/2025", 1);
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("prerequisite")) {
                    rejected = true;
                }
            }

            // 2. When ENFORCE_PREREQUISITES is toggled to false, registration must be permitted
            SystemConfig.setEnforcePrerequisites(false);
            studentCtrl.registerCourse(s.getId(), cs201.getId(), "2024/2025", 1);
            boolean allowedWhenDisabled = new StudentDAO().isEnrolled(s.getId(), cs201.getId(), "2024/2025", 1);

            // Restore default
            SystemConfig.setEnforcePrerequisites(true);

            if (rejected && allowedWhenDisabled) {
                pass("Prerequisites correctly enforced when active, and configurable/bypassable per policy.");
            } else {
                fail("Prerequisite test failed: rejected=" + rejected + ", allowedWhenDisabled=" + allowedWhenDisabled);
            }
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    private static void testConfigurableLecturerWorkload(CourseController courseCtrl, LecturerDAO lecturerDAO) {
        System.out.print("[TEST 6] Configurable Lecturer Workload Limit... ");
        try {
            int originalLimit = SystemConfig.getMaxLecturerCreditHours();
            SystemConfig.setMaxLecturerCreditHours(5); // Set strict 5 credit hour test limit

            List<Lecturer> lecturers = lecturerDAO.findAll();
            if (lecturers.isEmpty()) throw new IllegalStateException("No lecturers found.");
            int lId = lecturers.get(0).getId();

            String acadYear = "2024/2025";
            int sem = 1;

            // Course 1 is 3 credits, Course 2 is 3 credits (total 6 > 5)
            Course c1 = courseCtrl.getCourseById(1);
            Course c2 = courseCtrl.getCourseById(2);

            // Clean previous allocations for test
            try (java.sql.Connection conn = db.DatabaseConnection.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM lecturer_courses WHERE lecturer_id = ? AND academic_year = ? AND semester = ?")) {
                ps.setInt(1, lId); ps.setString(2, acadYear); ps.setInt(3, sem);
                ps.executeUpdate();
            }

            // First course (3 credits) should succeed
            courseCtrl.allocateCourseToLecturer(lId, c1.getId(), acadYear, sem);

            // Second course (4 credits -> total 7 credits > 6 limit) should fail
            boolean limitEnforced = false;
            try {
                courseCtrl.allocateCourseToLecturer(lId, c2.getId(), acadYear, sem);
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("Workload limit exceeded")) {
                    limitEnforced = true;
                }
            }

            // Now raise configurable limit to 18 credits
            SystemConfig.setMaxLecturerCreditHours(18);
            courseCtrl.allocateCourseToLecturer(lId, c2.getId(), acadYear, sem);
            boolean allowedAfterConfigChange = lecturerDAO.isCourseAssignedToLecturer(lId, c2.getId(), acadYear, sem);

            // Restore limit
            SystemConfig.setMaxLecturerCreditHours(originalLimit);

            if (limitEnforced && allowedAfterConfigChange) {
                pass("Lecturer workload limit dynamically enforced and configurable.");
            } else {
                fail("Workload test failed: limitEnforced=" + limitEnforced + ", allowedAfterConfigChange=" + allowedAfterConfigChange);
            }
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    private static void testScoreFinalizationAndCorrection(StudentController studentCtrl, ScoreDAO scoreDAO, AcademicAuditDAO auditDAO) {
        System.out.print("[TEST 7] Score Finalization & Authorized Correction Audit... ");
        try {
            int sId = 1;
            int cId = 1;
            String year = "2024/2025";
            int sem = 1;

            // Reset any existing score for test idempotency
            try (java.sql.Connection conn = db.DatabaseConnection.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM scores WHERE student_id = ? AND course_id = ? AND academic_year = ? AND semester = ?")) {
                ps.setInt(1, sId); ps.setInt(2, cId); ps.setString(3, year); ps.setInt(4, sem);
                ps.executeUpdate();
            }

            // Enter a draft score first
            Score initial = new Score(sId, cId, year, sem, 25.0, 55.0);
            initial.setStatus("DRAFT");
            initial.setEnteredBy(1);
            scoreDAO.upsert(initial);

            // Finalize course scores
            int finalizedCount = studentCtrl.finalizeCourseScores(year, sem, cId);
            Score finalized = scoreDAO.findScore(sId, cId, year, sem);

            if (finalized == null || !"FINALIZED".equalsIgnoreCase(finalized.getStatus())) {
                fail("Score was not marked as FINALIZED.");
                return;
            }

            // Normal modification must be rejected
            boolean directEditBlocked = false;
            try {
                Score draftAttempt = new Score(sId, cId, year, sem, 28.0, 60.0);
                draftAttempt.setEnteredBy(1);
                scoreDAO.upsert(draftAttempt);
            } catch (IllegalStateException e) {
                if (e.getMessage().contains("finalized")) {
                    directEditBlocked = true;
                }
            }

            // Authorized score correction must succeed and log to audit trail
            String correctionReason = "Remarking approved by Faculty Examination Board";
            studentCtrl.correctScore(finalized.getId(), 27.0, 63.0, correctionReason);

            Score corrected = scoreDAO.findById(finalized.getId());
            boolean scoreUpdated = (corrected.getCatScore() == 27.0 && corrected.getExamScore() == 63.0);

            // Verify audit trail entry
            List<AcademicAuditLog> logs = auditDAO.getRecentLogs(10);
            boolean auditFound = logs.stream().anyMatch(l ->
                    "SCORE".equals(l.getEntityType()) &&
                    "SCORE_CORRECTION".equals(l.getAction()) &&
                    correctionReason.equals(l.getReason()) &&
                    l.getPreviousValue().contains("CAT=25.00")
            );

            if (directEditBlocked && scoreUpdated && auditFound) {
                pass("Scores safely locked upon finalization; official corrections audited with old/new marks.");
            } else {
                fail("Score finalization test failed: directEditBlocked=" + directEditBlocked + ", scoreUpdated=" + scoreUpdated + ", auditFound=" + auditFound);
            }
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    private static void pass(String msg) {
        passed++;
        System.out.println("PASSED: " + msg);
    }

    private static void fail(String msg) {
        failed++;
        System.out.println("FAILED: " + msg);
    }
}
