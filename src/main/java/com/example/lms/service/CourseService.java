package com.example.lms.service;

import com.example.lms.dto.*;
import com.example.lms.enums.CourseStatus;
import com.example.lms.exception.CourseNotFoundException;
import com.example.lms.model.Course;
import com.example.lms.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {
    private final CourseRepository courseRepository;
    private final UserService userService;

    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        Course course = new Course();
        course.setTitle(request.title().strip());
        course.setDescription(request.description());
        course.setAuthor(userService.findInstructor(request.instructorId()));
        return Mapper.course(courseRepository.save(course));
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id).orElseThrow(CourseNotFoundException::new);
    }

    public CourseDetailsResponse getDetails(Long id) {
        return Mapper.courseDetails(getCourseById(id));
    }

    public List<CourseResponse> getPublishedCourses() {
        return courseRepository.findByStatus(CourseStatus.PUBLISHED).stream().map(Mapper::course).toList();
    }

    public List<CourseResponse> getInstructorCourses(Long id) {
        userService.findInstructor(id);
        return courseRepository.findByAuthorId(id).stream().map(Mapper::course).toList();
    }

    @Transactional
    public CourseResponse updateCourse(Long id, UpdateCourseRequest request) {
        Course course = getForUpdate(id);
        requireDraft(course);
        course.setTitle(request.title().strip());
        course.setDescription(request.description());
        return Mapper.course(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = getForUpdate(id);
        requireDraft(course);
        courseRepository.delete(course);
    }

    @Transactional
    public CourseResponse publishCourse(Long id) {
        Course course = getForUpdate(id);
        course.publish();
        return Mapper.course(course);
    }

    @Transactional
    public CourseResponse archiveCourse(Long id) {
        Course course = getForUpdate(id);
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new IllegalStateException("Архивировать можно только опубликованный курс");
        }
        course.setStatus(CourseStatus.ARCHIVED);
        return Mapper.course(course);
    }

    @Transactional
    public Course getForUpdate(Long id) {
        return courseRepository.findForUpdate(id).orElseThrow(CourseNotFoundException::new);
    }

    public void requireDraft(Course course) {
        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new IllegalStateException("Изменять и удалять можно только черновик курса");
        }
    }
}
