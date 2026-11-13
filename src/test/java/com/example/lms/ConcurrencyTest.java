package com.example.lms;

import com.example.lms.dto.*;
import com.example.lms.exception.AlreadyEnrolledException;
import com.example.lms.repository.*;
import com.example.lms.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConcurrencyTest {
    @Autowired UserService userService;
    @Autowired CourseService courseService;
    @Autowired LessonService lessonService;
    @Autowired EnrollmentService enrollmentService;
    @Autowired CertificateRepository certificateRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired UserRepository userRepository;
    @Autowired PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            certificateRepository.deleteAll();
            enrollmentRepository.deleteAll();
            courseRepository.deleteAll();
            userRepository.deleteAll();
        });
    }

    @Test
    void concurrentCompletionsFinishCourseAndIssueOneCertificate() throws Exception {
        Data data = createCourse();
        long enrollmentId = enrollmentService.enrollStudent(data.studentId(), data.courseId()).id();
        runTogether(() -> enrollmentService.completeLesson(enrollmentId, data.firstLesson()),
                () -> enrollmentService.completeLesson(enrollmentId, data.secondLesson()));
        var progress = enrollmentService.getProgress(enrollmentId);
        assertThat(progress.progressPercent()).isEqualTo(100);
        assertThat(progress.completedLessons()).isEqualTo(2);
        assertThat(certificateRepository.findByStudentId(data.studentId())).hasSize(1);
        enrollmentService.completeLesson(enrollmentId, data.firstLesson());
        assertThat(certificateRepository.findByStudentId(data.studentId())).hasSize(1);
    }

    @Test
    void concurrentEnrollmentCreatesOnlyOneRecord() throws Exception {
        Data data = createCourse();
        var results = runTogether(() -> enroll(data), () -> enroll(data));
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(enrollmentRepository.findByStudentId(data.studentId())).hasSize(1);
    }

    private boolean enroll(Data data) {
        try {
            enrollmentService.enrollStudent(data.studentId(), data.courseId());
            return true;
        } catch (AlreadyEnrolledException e) {
            return false;
        }
    }

    private Data createCourse() {
        String suffix = UUID.randomUUID().toString();
        long student = userService.createStudent(new CreateStudentRequest("Иван", "Иванов", "s" + suffix + "@example.com")).id();
        long instructor = userService.createInstructor(new CreateInstructorRequest("Анна", "Иванова", "t" + suffix + "@example.com", null)).id();
        long course = courseService.createCourse(new CreateCourseRequest("Java", null, instructor)).id();
        var request = new CreateLessonRequest.Text("Текст", "Java", 1);
        long first = lessonService.createLesson(course, request).id();
        long second = lessonService.createLesson(course, request).id();
        courseService.publishCourse(course);
        return new Data(student, course, first, second);
    }

    private <T> java.util.List<T> runTogether(Supplier<T> first, Supplier<T> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var one = pool.submit(() -> { start.await(); return first.get(); });
            var two = pool.submit(() -> { start.await(); return second.get(); });
            start.countDown();
            return java.util.List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
        }
    }

    private record Data(long studentId, long courseId, long firstLesson, long secondLesson) {
    }
}
