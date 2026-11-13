package com.example.lms.service;

import com.example.lms.dto.UpdateCourseRequest;
import com.example.lms.enums.CourseStatus;
import com.example.lms.model.Course;
import com.example.lms.model.Instructor;
import com.example.lms.model.TextLesson;
import com.example.lms.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {
    @Mock CourseRepository courses;
    @Mock UserService users;
    @InjectMocks CourseService service;
    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        course.setTitle("Java");
        Instructor author = new Instructor();
        author.setFirstName("Анна");
        author.setLastName("Петрова");
        course.setAuthor(author);
        when(courses.findForUpdate(1L)).thenReturn(Optional.of(course));
    }

    @Test
    void refusesToPublishEmptyCourse() {
        assertThatThrownBy(() -> service.publishCourse(1L)).isInstanceOf(IllegalStateException.class);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(course.getPublishedAt()).isNull();
    }

    @Test
    void publishesCourseWithLessons() {
        course.getLessons().add(new TextLesson());
        assertThat(service.publishCourse(1L).status()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(course.getPublishedAt()).isNotNull();
    }

    @Test
    void archivedCourseStopsAcceptingNewEnrollments() {
        course.setStatus(CourseStatus.PUBLISHED);
        assertThat(service.archiveCourse(1L).status()).isEqualTo(CourseStatus.ARCHIVED);
        assertThat(course.isAvailableForEnrollment()).isFalse();
    }

    @Test
    void refusesToChangePublishedCourse() {
        course.setStatus(CourseStatus.PUBLISHED);
        assertThatThrownBy(() -> service.updateCourse(1L, new UpdateCourseRequest("Новое название", null)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(course.getTitle()).isEqualTo("Java");
    }

    @Test
    void deletesDraft() {
        service.deleteCourse(1L);
        verify(courses).delete(course);
    }
}
