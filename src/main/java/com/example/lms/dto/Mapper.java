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
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getCreatedAt(), bio);
    }

    public static CourseResponse course(Course course) {
        return new CourseResponse(course.getId(), course.getTitle(), course.getDescription(),
                course.getAuthor().getFullName(), course.getStatus(), course.getTotalLessons(),
                course.getTotalEstimatedMinutes());
    }

    public static CourseDetailsResponse courseDetails(Course course) {
        return new CourseDetailsResponse(course.getId(), course.getTitle(), course.getDescription(),
                course.getAuthor().getFullName(), course.getStatus(), course.getTotalLessons(),
                course.getTotalEstimatedMinutes(), course.getLessons().stream().map(Mapper::lesson).toList());
    }

    public static QuestionResponse question(Question question) {
        return new QuestionResponse(question.getId(), question.getText(), List.copyOf(question.getOptions()));
    }

    public static LessonResponse lesson(Lesson lesson) {
        if (lesson instanceof VideoLesson video) {
            return new LessonResponse(video.getId(), video.getTitle(), video.getType(), video.getOrderIndex(),
                    video.getEstimatedMinutes(), video.getVideoUrl(), video.getDurationMinutes(),
                    null, null, null, null, null);
        }
        if (lesson instanceof TextLesson text) {
            return new LessonResponse(text.getId(), text.getTitle(), text.getType(), text.getOrderIndex(),
                    text.getEstimatedMinutes(), null, null, text.getContent(), text.getReadingTimeMinutes(),
                    null, null, null);
        }
        QuizLesson quiz = (QuizLesson) lesson;
        return new LessonResponse(quiz.getId(), quiz.getTitle(), quiz.getType(), quiz.getOrderIndex(),
                quiz.getEstimatedMinutes(), null, null, null, null, quiz.getPassingScore(),
                quiz.getTimeLimitMinutes(), quiz.getQuestions().stream().map(Mapper::question).toList());
    }

    public static EnrollmentResponse enrollment(Enrollment enrollment) {
        return new EnrollmentResponse(enrollment.getId(), enrollment.getStudent().getId(),
                enrollment.getCourse().getId(), enrollment.getCourse().getTitle(), enrollment.getStatus(),
                enrollment.getProgressPercent(), enrollment.getEnrolledAt(), enrollment.getCompletedAt());
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
        return new ProgressResponse(enrollment.getId(), enrollment.getCourse().getTitle(),
                enrollment.getProgressPercent(), enrollment.getCompletedLessons(), lessons.size(),
                enrollment.getStatus(), lessons);
    }

    public static CertificateResponse certificate(Certificate certificate) {
        return new CertificateResponse(certificate.getId(), certificate.getStudent().getId(),
                certificate.getStudent().getFullName(), certificate.getCourse().getId(),
                certificate.getCourse().getTitle(), certificate.getIssuedAt(), certificate.getUniqueCode());
    }

}
