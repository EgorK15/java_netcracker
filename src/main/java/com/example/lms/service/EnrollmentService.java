package com.example.lms.service;

import com.example.lms.dto.*;
import com.example.lms.enums.EnrollmentStatus;
import com.example.lms.exception.*;
import com.example.lms.model.*;
import com.example.lms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository progressRepository;
    private final UserService userService;
    private final CourseService courseService;
    private final LessonService lessonService;
    private final CertificateService certificateService;

    @Transactional
    public EnrollmentResponse enrollStudent(Long studentId, Long courseId) {
        Student student = userService.findStudent(studentId);
        Course course = courseService.getForUpdate(courseId);
        if (!course.isAvailableForEnrollment()) {
            throw new CourseNotPublishedException();
        }
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new AlreadyEnrolledException();
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        return Mapper.enrollment(enrollmentRepository.save(enrollment));
    }

    public List<EnrollmentResponse> getStudentEnrollments(Long studentId) {
        userService.findStudent(studentId);
        return enrollmentRepository.findByStudentId(studentId).stream().map(Mapper::enrollment).toList();
    }

    public ProgressResponse getProgress(Long id) {
        return Mapper.progress(findEnrollment(id));
    }

    @Transactional
    public void dropEnrollment(Long id) {
        Enrollment enrollment = getForUpdate(id);
        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            return;
        }
        requireActive(enrollment);
        enrollment.setStatus(EnrollmentStatus.DROPPED);
    }

    @Transactional
    public ProgressResponse completeLesson(Long id, Long lessonId) {
        Enrollment enrollment = getForUpdate(id);
        Lesson lesson = lessonService.findLesson(lessonId);
        requireCourseLesson(enrollment, lesson);
        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new IllegalStateException("Запись на курс отменена");
        }
        LessonProgress progress = findProgress(enrollment, lessonId);
        if (progress != null && progress.getCompletedAt() != null) {
            return Mapper.progress(enrollment);
        }
        requireActive(enrollment);
        if (lesson instanceof QuizLesson) {
            throw new IllegalStateException("Для завершения теста нужно набрать проходной балл");
        }
        saveProgress(enrollment, lesson, null, true);
        return Mapper.progress(enrollment);
    }

    @Transactional
    public Enrollment getForUpdate(Long id) {
        return enrollmentRepository.findForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Запись на курс не найдена"));
    }

    public void requireActive(Enrollment enrollment) {
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Запись на курс не активна");
        }
    }

    public void requireCourseLesson(Enrollment enrollment, Lesson lesson) {
        if (!lesson.getCourse().getId().equals(enrollment.getCourse().getId())) {
            throw new IllegalArgumentException("Урок не принадлежит курсу этой записи");
        }
    }

    @Transactional
    public void saveProgress(Enrollment enrollment, Lesson lesson, Integer score, boolean completed) {
        LessonProgress progress = findProgress(enrollment, lesson.getId());
        if (progress == null) {
            progress = new LessonProgress();
            progress.setEnrollment(enrollment);
            progress.setLesson(lesson);
            enrollment.getProgress().add(progress);
        }
        if (score != null && (progress.getQuizScore() == null || score > progress.getQuizScore())) {
            progress.setQuizScore(score);
        }
        if (completed && progress.getCompletedAt() == null) {
            progress.setCompletedAt(LocalDateTime.now());
        }
        progressRepository.save(progress);
        if (enrollment.getCourse().getTotalLessons() > 0
                && enrollment.getCompletedLessons() == enrollment.getCourse().getTotalLessons()) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setCompletedAt(LocalDateTime.now());
            certificateService.issueCertificate(enrollment);
        }
    }

    private Enrollment findEnrollment(Long id) {
        return enrollmentRepository.findWithProgressById(id)
                .orElseThrow(() -> new NotFoundException("Запись на курс не найдена"));
    }

    private LessonProgress findProgress(Enrollment enrollment, Long lessonId) {
        for (LessonProgress progress : enrollment.getProgress()) {
            if (progress.getLesson().getId().equals(lessonId)) {
                return progress;
            }
        }
        return null;
    }
}
