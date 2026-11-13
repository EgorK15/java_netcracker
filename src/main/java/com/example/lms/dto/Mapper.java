package com.example.lms.dto;

import com.example.lms.model.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Mapper {
    private Mapper() {
    }

    public static UserResponse user(User user) {
        String bio = user instanceof Instructor instructor ? instructor.getBio() : null;
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .bio(bio)
                .build();
    }

    public static CourseResponse course(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .authorName(course.getAuthor().getFullName())
                .status(course.getStatus())
                .totalLessons(course.getTotalLessons())
                .duration(course.getTotalEstimatedMinutes())
                .build();
    }

    public static CourseDetailsResponse courseDetails(Course course) {
        return CourseDetailsResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .authorName(course.getAuthor().getFullName())
                .status(course.getStatus())
                .totalLessons(course.getTotalLessons())
                .duration(course.getTotalEstimatedMinutes())
                .lessons(course.getLessons().stream().map(Mapper::lesson).toList())
                .build();
    }

    public static QuestionResponse question(Question question) {
        return new QuestionResponse(question.getId(), question.getText(), List.copyOf(question.getOptions()));
    }

    public static LessonResponse lesson(Lesson lesson) {
        if (lesson instanceof VideoLesson video) {
            return LessonResponse.Video.builder()
                    .id(video.getId())
                    .title(video.getTitle())
                    .type(video.getType())
                    .orderIndex(video.getOrderIndex())
                    .estimatedMinutes(video.getEstimatedMinutes())
                    .videoUrl(video.getVideoUrl())
                    .durationMinutes(video.getDurationMinutes())
                    .build();
        }
        if (lesson instanceof TextLesson text) {
            return LessonResponse.Text.builder()
                    .id(text.getId())
                    .title(text.getTitle())
                    .type(text.getType())
                    .orderIndex(text.getOrderIndex())
                    .estimatedMinutes(text.getEstimatedMinutes())
                    .content(text.getContent())
                    .readingTimeMinutes(text.getReadingTimeMinutes())
                    .build();
        }
        QuizLesson quiz = (QuizLesson) lesson;
        return LessonResponse.Quiz.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .type(quiz.getType())
                .orderIndex(quiz.getOrderIndex())
                .estimatedMinutes(quiz.getEstimatedMinutes())
                .passingScore(quiz.getPassingScore())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .questions(quiz.getQuestions().stream().map(Mapper::question).toList())
                .build();
    }

    public static EnrollmentResponse enrollment(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .courseId(enrollment.getCourse().getId())
                .courseTitle(enrollment.getCourse().getTitle())
                .status(enrollment.getStatus())
                .progressPercent(enrollment.getProgressPercent())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .build();
    }

    public static ProgressResponse progress(Enrollment enrollment) {
        Set<Long> completed = new HashSet<>();
        for (LessonProgress item : enrollment.getProgress()) {
            if (item.getCompletedAt() != null) {
                completed.add(item.getLesson().getId());
            }
        }
        var lessons = enrollment.getCourse().getLessons().stream()
                .map(lesson -> new LessonProgressResponse(lesson.getId(), lesson.getTitle(),
                        completed.contains(lesson.getId()))).toList();
        return ProgressResponse.builder()
                .enrollmentId(enrollment.getId())
                .courseTitle(enrollment.getCourse().getTitle())
                .progressPercent(enrollment.getProgressPercent())
                .completedLessons(enrollment.getCompletedLessons())
                .totalLessons(lessons.size())
                .status(enrollment.getStatus())
                .lessons(lessons)
                .build();
    }

    public static CertificateResponse certificate(Certificate certificate) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .studentId(certificate.getStudent().getId())
                .studentName(certificate.getStudent().getFullName())
                .courseId(certificate.getCourse().getId())
                .courseTitle(certificate.getCourse().getTitle())
                .issuedAt(certificate.getIssuedAt())
                .uniqueCode(certificate.getUniqueCode())
                .build();
    }

}
