package com.example.lms.service;

import com.example.lms.enums.*;
import com.example.lms.exception.*;
import com.example.lms.model.*;
import com.example.lms.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock LessonProgressRepository progressRepository;
    @Mock UserService userService;
    @Mock CourseService courseService;
    @Mock LessonService lessonService;
    @Mock CertificateService certificateService;
    @InjectMocks EnrollmentService service;

    Student student;
    Course course;
    Enrollment enrollment;
    TextLesson lesson;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        course = new Course();
        course.setId(2L);
        course.setTitle("Java");
        course.setStatus(CourseStatus.PUBLISHED);
        lesson = new TextLesson();
        lesson.setId(3L);
        lesson.setTitle("Введение");
        lesson.setCourse(course);
        course.getLessons().add(lesson);
        enrollment = new Enrollment();
        enrollment.setId(4L);
        enrollment.setStudent(student);
        enrollment.setCourse(course);
    }

    @Test
    void enrollsStudent() {
        when(userService.findStudent(1L)).thenReturn(student);
        when(courseService.getForUpdate(2L)).thenReturn(course);
        when(enrollmentRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        var response = service.enrollStudent(1L, 2L);
        assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(response.progressPercent()).isZero();
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void rejectsDraftCourse() {
        course.setStatus(CourseStatus.DRAFT);
        when(userService.findStudent(1L)).thenReturn(student);
        when(courseService.getForUpdate(2L)).thenReturn(course);
        assertThatThrownBy(() -> service.enrollStudent(1L, 2L))
                .isInstanceOf(CourseNotPublishedException.class);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateEnrollment() {
        when(userService.findStudent(1L)).thenReturn(student);
        when(courseService.getForUpdate(2L)).thenReturn(course);
        when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> service.enrollStudent(1L, 2L))
                .isInstanceOf(AlreadyEnrolledException.class);
    }

    @Test
    void calculatesPartialProgress() {
        course.getLessons().add(new TextLesson());
        course.getLessons().add(new TextLesson());
        LessonProgress progress = new LessonProgress();
        progress.setLesson(lesson);
        progress.setCompletedAt(LocalDateTime.now());
        enrollment.getProgress().add(progress);
        when(enrollmentRepository.findById(4L)).thenReturn(Optional.of(enrollment));
        assertThat(service.getProgress(4L).progressPercent()).isEqualTo(33);
    }

    @Test
    void completesCourseAndIssuesCertificate() {
        mockCompletion();
        var response = service.completeLesson(4L, 3L);
        assertThat(response.progressPercent()).isEqualTo(100);
        assertThat(response.status()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(enrollment.getCompletedAt()).isNotNull();
        verify(certificateService).issueCertificate(enrollment);
    }

    @Test
    void repeatedCompletionDoesNotIssueAnotherCertificate() {
        mockCompletion();
        service.completeLesson(4L, 3L);
        service.completeLesson(4L, 3L);
        assertThat(enrollment.getProgress()).hasSize(1);
        verify(certificateService, times(1)).issueCertificate(enrollment);
        verify(progressRepository, times(1)).save(any());
    }

    @Test
    void rejectsLessonFromAnotherCourse() {
        mockCompletion();
        Course other = new Course();
        other.setId(99L);
        lesson.setCourse(other);
        assertThatThrownBy(() -> service.completeLesson(4L, 3L)).isInstanceOf(IllegalArgumentException.class);
        verify(progressRepository, never()).save(any());
    }

    @Test
    void rejectsManualQuizCompletion() {
        QuizLesson quiz = new QuizLesson();
        quiz.setId(5L);
        quiz.setCourse(course);
        when(enrollmentRepository.findForUpdate(4L)).thenReturn(Optional.of(enrollment));
        when(lessonService.findLesson(5L)).thenReturn(quiz);
        assertThatThrownBy(() -> service.completeLesson(4L, 5L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void droppedEnrollmentCannotCompleteLessons() {
        mockCompletion();
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        assertThatThrownBy(() -> service.completeLesson(4L, 3L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failedQuizDoesNotIncreaseProgressAndKeepsBestScore() {
        service.saveProgress(enrollment, lesson, 60, false);
        service.saveProgress(enrollment, lesson, 20, false);
        assertThat(enrollment.getProgress()).hasSize(1);
        assertThat(enrollment.getProgress().iterator().next().getQuizScore()).isEqualTo(60);
        assertThat(enrollment.getProgressPercent()).isZero();
        verify(certificateService, never()).issueCertificate(any());
    }

    private void mockCompletion() {
        when(enrollmentRepository.findForUpdate(4L)).thenReturn(Optional.of(enrollment));
        when(lessonService.findLesson(3L)).thenReturn(lesson);
    }
}
