package com.example.lms;

import com.example.lms.repository.*;
import com.fasterxml.jackson.databind.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired CertificateRepository certificateRepository;
    @Autowired EntityManager entityManager;

    @Test
    void readsProfilesAndLessonDetails() throws Exception {
        Data data = createCourse();
        mvc.perform(get("/api/students/{id}", data.studentId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("student@example.com"));
        mvc.perform(get("/api/instructors/{id}", data.instructorId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("teacher@example.com"));
        mvc.perform(get("/api/lessons/{id}", data.videoId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.durationMinutes").value(7))
                .andExpect(jsonPath("$.questions").doesNotExist());
    }

    @Test
    void rejectsInvalidCourseTransitions() throws Exception {
        long instructor = create("/api/instructors", student("teacher@example.com")).get("id").asLong();
        long draft = create("/api/courses", Map.of("title", "Java", "instructorId", instructor)).get("id").asLong();
        mvc.perform(patch("/api/courses/{id}/archive", draft)).andExpect(status().isBadRequest());
        create("/api/courses/" + draft + "/lessons", text("Введение"));
        mvc.perform(patch("/api/courses/{id}/publish", draft)).andExpect(status().isOk());
        mvc.perform(patch("/api/courses/{id}/publish", draft)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/courses/{id}", draft)).andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void distinguishesMissingLessonAndWrongLessonType() throws Exception {
        Data data = createCourse();
        mvc.perform(delete("/api/lessons/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/lessons/{id}/questions", data.textId())).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/enrollments/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/certificates/999999/download")).andExpect(status().isNotFound());
    }

    @Test
    void enrollmentListShowsSameProgressAsDetails() throws Exception {
        Data data = createCourse();
        complete(data, data.textId(), 33);
        complete(data, data.videoId(), 66);
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/api/students/{id}/enrollments", data.studentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(data.enrollmentId()), Long.class))
                .andExpect(jsonPath("$[0].progressPercent").value(66));
        mvc.perform(get("/api/enrollments/{id}/progress", data.enrollmentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(66));
    }

    @Test
    void completesMixedCourseAndDownloadsOneCertificate() throws Exception {
        Data data = createCourse();
        mvc.perform(get("/api/courses/{id}", data.courseId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.duration").value(12))
                .andExpect(content().string(not(containsString("correctOptionIndex"))));
        mvc.perform(get("/api/courses")).andExpect(jsonPath("$[0].status").value("PUBLISHED"));
        mvc.perform(get("/api/lessons/{id}/questions", data.quizId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].options[1]").value("4"))
                .andExpect(content().string(not(containsString("correctOptionIndex"))));
        complete(data, data.textId(), 33);
        complete(data, data.videoId(), 66);
        mvc.perform(post("/api/enrollments/{id}/lessons/{lessonId}/complete", data.enrollmentId(), data.quizId()))
                .andExpect(status().isBadRequest());
        submit(data, 0, 1).andExpect(status().isOk()).andExpect(jsonPath("$.passed").value(false));
        mvc.perform(get("/api/enrollments/{id}/progress", data.enrollmentId()))
                .andExpect(jsonPath("$.progressPercent").value(66));
        submit(data, 1, 0).andExpect(status().isOk()).andExpect(jsonPath("$.scorePercent").value(100))
                .andExpect(jsonPath("$.passed").value(true));
        mvc.perform(get("/api/enrollments/{id}", data.enrollmentId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressPercent").value(100));
        complete(data, data.textId(), 100);
        complete(data, data.quizId(), 100);
        JsonNode certificates = read(mvc.perform(get("/api/students/{id}/certificates", data.studentId()))
                .andExpect(status().isOk()).andReturn());
        assertThat(certificates.size()).isEqualTo(1);
        long certificateId = certificates.get(0).get("id").asLong();
        String code = certificates.get(0).get("uniqueCode").asText();
        mvc.perform(get("/api/certificates/{code}", code)).andExpect(status().isOk())
                .andExpect(jsonPath("$.courseTitle").value("Java"));
        mvc.perform(get("/api/certificates/{id}/download", certificateId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("Иван Иванов")))
                .andExpect(content().string(containsString(code)));
    }

    @Test
    void keepsBestQuizResultAcrossRetakes() throws Exception {
        Data data = createCourse();
        submit(data, 1, 1).andExpect(status().isOk()).andExpect(jsonPath("$.scorePercent").value(50));
        submit(data, 0, 1).andExpect(status().isOk()).andExpect(jsonPath("$.scorePercent").value(0));
        entityManager.flush();
        entityManager.clear();
        var enrollment = enrollmentRepository.findById(data.enrollmentId()).orElseThrow();
        assertThat(enrollment.getProgress()).hasSize(1);
        var progress = enrollment.getProgress().iterator().next();
        assertThat(progress.getQuizScore()).isEqualTo(50);
        assertThat(progress.getCompletedAt()).isNull();
        assertThat(enrollment.getProgressPercent()).isZero();
        submit(data, 1, 0).andExpect(status().isOk());
        submit(data, 0, 1).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        var reloaded = enrollmentRepository.findById(data.enrollmentId()).orElseThrow();
        var reloadedProgress = reloaded.getProgress().iterator().next();
        assertThat(reloadedProgress.getQuizScore()).isEqualTo(100);
        assertThat(reloadedProgress.getCompletedAt()).isNotNull();
        assertThat(reloaded.getProgressPercent()).isEqualTo(33);
        assertThat(certificateRepository.findByStudentId(data.studentId())).isEmpty();
    }

    @Test
    void rejectsInvalidQuizAnswersWithoutSavingProgress() throws Exception {
        Data data = createCourse();
        List<List<Map<String, Object>>> invalidAnswers = List.of(
                List.of(answer(data.firstQuestion(), 1), answer(data.firstQuestion(), 1)),
                List.of(answer(data.firstQuestion(), 1), answer(999999L, 0)),
                List.of(answer(data.firstQuestion(), 10), answer(data.secondQuestion(), 0)),
                List.of(answer(data.firstQuestion(), 1)));
        for (var answers : invalidAnswers) {
            mvc.perform(post("/api/lessons/{id}/submit", data.quizId()).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("enrollmentId", data.enrollmentId(), "answers", answers))))
                    .andExpect(status().isBadRequest());
        }
        assertThat(enrollmentRepository.findById(data.enrollmentId()).orElseThrow().getProgress()).isEmpty();
    }

    @Test
    void rejectsDuplicateEnrollmentsAndForeignLessons() throws Exception {
        Data data = createCourse();
        mvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("studentId", data.studentId(), "courseId", data.courseId()))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").exists());
        long otherCourse = create("/api/courses", Map.of("title", "SQL", "instructorId", data.instructorId())).get("id").asLong();
        long otherLesson = create("/api/courses/" + otherCourse + "/lessons", text("SQL")).get("id").asLong();
        mvc.perform(post("/api/enrollments/{id}/lessons/{lessonId}/complete", data.enrollmentId(), otherLesson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void droppedEnrollmentCannotContinueOrEnrollAgain() throws Exception {
        Data data = createCourse();
        mvc.perform(delete("/api/enrollments/{id}", data.enrollmentId())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/enrollments/{id}", data.enrollmentId())).andExpect(status().isNoContent());
        mvc.perform(get("/api/students/{id}/enrollments", data.studentId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("DROPPED"));
        mvc.perform(post("/api/enrollments/{id}/lessons/{lessonId}/complete", data.enrollmentId(), data.textId()))
                .andExpect(status().isBadRequest());
        submit(data, 1, 0).andExpect(status().isBadRequest());
        mvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("studentId", data.studentId(), "courseId", data.courseId()))))
                .andExpect(status().isConflict());
    }

    @Test
    void archivedCourseAllowsExistingStudentsToFinish() throws Exception {
        Data data = createCourse();
        mvc.perform(patch("/api/courses/{id}/archive", data.courseId())).andExpect(status().isOk());
        long studentId = create("/api/students", student("other@example.com")).get("id").asLong();
        mvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("studentId", studentId, "courseId", data.courseId()))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/courses")).andExpect(jsonPath("$").isEmpty());
        complete(data, data.textId(), 33);
        complete(data, data.videoId(), 66);
        submit(data, 1, 0).andExpect(status().isOk());
        mvc.perform(get("/api/enrollments/{id}", data.enrollmentId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(delete("/api/enrollments/{id}", data.enrollmentId())).andExpect(status().isBadRequest());
    }

    @Test
    void publishedCourseCannotChangeItsLessons() throws Exception {
        Data data = createCourse();
        mvc.perform(post("/api/courses/{id}/lessons", data.courseId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(text("Новый урок")))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/lessons/{id}", data.textId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(text("Изменено")))).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/lessons/{id}", data.textId())).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/courses/{id}", data.courseId())).andExpect(status().isBadRequest());
        mvc.perform(put("/api/courses/{id}", data.courseId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Новый курс\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void draftCourseAndLessonsCanBeUpdatedAndDeleted() throws Exception {
        long instructor = create("/api/instructors", student("teacher@example.com")).get("id").asLong();
        long course = create("/api/courses", Map.of("title", "Черновик", "instructorId", instructor)).get("id").asLong();
        mvc.perform(patch("/api/courses/{id}/publish", course)).andExpect(status().isBadRequest());
        long lesson = create("/api/courses/" + course + "/lessons", text("Начало")).get("id").asLong();
        mvc.perform(put("/api/lessons/{id}", lesson).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(text("Введение"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Введение"));
        mvc.perform(put("/api/courses/{id}", course).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Java 21\",\"description\":\"Основы\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Java 21"));
        mvc.perform(get("/api/instructors/{id}/courses", instructor))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Java 21"));
        mvc.perform(delete("/api/lessons/{id}", lesson)).andExpect(status().isNoContent());
        create("/api/courses/" + course + "/lessons", quiz());
        mvc.perform(delete("/api/courses/{id}", course)).andExpect(status().isNoContent());
        mvc.perform(get("/api/courses/{id}", course)).andExpect(status().isNotFound());
    }

    @Test
    void validatesUsersAndKeepsEmailUniqueAcrossRoles() throws Exception {
        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\" \",\"lastName\":\"Иванов\",\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isArray());
        create("/api/students", student("Student@Example.com"));
        mvc.perform(post("/api/instructors").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(student("student@example.com"))))
                .andExpect(status().isConflict());
    }

    @Test
    void validatesNestedLessonFields() throws Exception {
        long instructor = create("/api/instructors", student("teacher@example.com")).get("id").asLong();
        long course = create("/api/courses", Map.of("title", "Java", "instructorId", instructor)).get("id").asLong();
        for (String body : List.of(
                "{\"title\":\"Видео\",\"type\":\"VIDEO\",\"videoUrl\":\"file:///test\",\"durationMinutes\":5}",
                "{\"title\":\"Видео\",\"type\":\"VIDEO\"}",
                "{\"title\":\"Текст\",\"type\":\"TEXT\",\"content\":\"x\",\"readingTimeMinutes\":0}",
                "{\"title\":\"Тест\",\"type\":\"QUIZ\",\"passingScore\":70,\"questions\":[]}",
                "{\"title\":\"Тест\",\"type\":\"QUIZ\",\"passingScore\":70,\"questions\":[null]}",
                "{\"title\":\"Тест\",\"type\":\"QUIZ\",\"passingScore\":70,\"questions\":[{\"text\":\"?\",\"options\":[\"a\",\"b\"],\"correctOptionIndex\":2}]}")) {
            mvc.perform(post("/api/courses/{id}/lessons", course).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void missingResourcesAndMalformedRequestsHaveExpectedStatuses() throws Exception {
        for (String path : List.of("/api/courses/999999", "/api/students/999999", "/api/instructors/999999",
                "/api/lessons/999999", "/api/enrollments/999999", "/api/certificates/unknown")) {
            mvc.perform(get(path)).andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").exists());
        }
        mvc.perform(get("/api/courses/abc")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sumsLongCourseDurationInListAndDetails() throws Exception {
        long instructor = create("/api/instructors", student("teacher@example.com")).get("id").asLong();
        long course = create("/api/courses", Map.of("title", "Java", "instructorId", instructor)).get("id").asLong();
        var lesson = Map.of("title", "Видео", "type", "VIDEO", "videoUrl", "https://example.com/java.mp4",
                "durationMinutes", 1_500_000_000);
        create("/api/courses/" + course + "/lessons", lesson);
        create("/api/courses/" + course + "/lessons", lesson);
        mvc.perform(patch("/api/courses/{id}/publish", course)).andExpect(status().isOk());
        mvc.perform(get("/api/courses/{id}", course)).andExpect(jsonPath("$.duration").value(3_000_000_000L));
        mvc.perform(get("/api/courses")).andExpect(jsonPath("$[0].duration").value(3_000_000_000L));
    }

    @Test
    void rejectsFractionalIntegersAndNumericLessonType() throws Exception {
        Data data = createCourse();
        long draft = create("/api/courses", Map.of("title", "Черновик", "instructorId", data.instructorId()))
                .get("id").asLong();
        for (var body : List.of(
                Map.of("title", "Видео", "type", 0, "videoUrl", "https://example.com/java.mp4", "durationMinutes", 2),
                Map.of("title", "Видео", "type", "VIDEO", "videoUrl", "https://example.com/java.mp4", "durationMinutes", 2.5))) {
            mvc.perform(post("/api/courses/{id}/lessons", draft).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
        var answers = List.of(Map.of("questionId", data.firstQuestion(), "selectedOption", 1.5),
                answer(data.secondQuestion(), 0));
        mvc.perform(post("/api/lessons/{id}/submit", data.quizId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("enrollmentId", data.enrollmentId(), "answers", answers))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/courses/{id}/lessons", draft)).andExpect(jsonPath("$").isEmpty());
        assertThat(enrollmentRepository.findById(data.enrollmentId()).orElseThrow().getProgress()).isEmpty();
    }

    @Test
    void openApiAndSwaggerAreAvailable() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/courses'].post.responses['201'].content['application/json']").exists())
                .andExpect(jsonPath("$.paths['/api/courses'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/courses'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/enrollments'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/certificates/{id}/download'].get.responses['200'].content['text/plain']").exists())
                .andExpect(jsonPath("$.components.schemas.CreateLessonRequest.discriminator.mapping.VIDEO")
                        .value("#/components/schemas/VideoLessonRequest"))
                .andExpect(jsonPath("$.components.schemas.VideoLessonRequest.required", hasItem("type")))
                .andExpect(jsonPath("$.components.schemas.VideoLessonRequest.allOf[1].properties.type.enum[0]").value("VIDEO"))
                .andExpect(jsonPath("$.paths['/api/courses/{courseId}/lessons'].post.requestBody.content['application/json'].schema.oneOf[*]['$ref']",
                        containsInAnyOrder("#/components/schemas/VideoLessonRequest", "#/components/schemas/TextLessonRequest",
                                "#/components/schemas/QuizLessonRequest")))
                .andExpect(jsonPath("$.paths['/api/lessons/{id}'].put.requestBody.content['application/json'].schema.oneOf", hasSize(3)))
                .andExpect(jsonPath("$.paths['/api/lessons/{id}/submit']").exists());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    private Data createCourse() throws Exception {
        long instructor = create("/api/instructors", student("teacher@example.com")).get("id").asLong();
        long student = create("/api/students", student("student@example.com")).get("id").asLong();
        long course = create("/api/courses", Map.of("title", "Java", "instructorId", instructor)).get("id").asLong();
        long text = create("/api/courses/" + course + "/lessons", text("Введение")).get("id").asLong();
        long video = create("/api/courses/" + course + "/lessons", Map.of("title", "Видео", "type", "VIDEO",
                "videoUrl", "https://example.com/java.mp4", "durationMinutes", 7)).get("id").asLong();
        JsonNode quiz = create("/api/courses/" + course + "/lessons", quiz());
        assertThat(quiz.toString()).doesNotContain("correctOptionIndex");
        mvc.perform(patch("/api/courses/{id}/publish", course)).andExpect(status().isOk());
        long enrollment = create("/api/enrollments", Map.of("studentId", student, "courseId", course)).get("id").asLong();
        return new Data(instructor, student, course, text, video, quiz.get("id").asLong(), enrollment,
                quiz.get("questions").get(0).get("id").asLong(), quiz.get("questions").get(1).get("id").asLong());
    }

    private Map<String, Object> student(String email) {
        return Map.of("firstName", "Иван", "lastName", "Иванов", "email", email);
    }

    private Map<String, Object> text(String title) {
        return Map.of("title", title, "type", "TEXT", "content", "# Java", "readingTimeMinutes", 3);
    }

    private Map<String, Object> quiz() {
        return Map.of("title", "Тест", "type", "QUIZ", "passingScore", 70, "questions", List.of(
                Map.of("text", "2+2?", "options", List.of("3", "4"), "correctOptionIndex", 1),
                Map.of("text", "Java — язык?", "options", List.of("Да", "Нет"), "correctOptionIndex", 0)));
    }

    private Map<String, Object> answer(long id, int selected) {
        return Map.of("questionId", id, "selectedOption", selected);
    }

    private ResultActions submit(Data data, int first, int second) throws Exception {
        return mvc.perform(post("/api/lessons/{id}/submit", data.quizId()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("enrollmentId", data.enrollmentId(), "answers",
                        List.of(answer(data.firstQuestion(), first), answer(data.secondQuestion(), second))))));
    }

    private void complete(Data data, long lesson, int percent) throws Exception {
        mvc.perform(post("/api/enrollments/{id}/lessons/{lessonId}/complete", data.enrollmentId(), lesson))
                .andExpect(status().isOk()).andExpect(jsonPath("$.progressPercent").value(percent));
    }

    private JsonNode create(String path, Object body) throws Exception {
        return read(mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn());
    }

    private JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private record Data(long instructorId, long studentId, long courseId, long textId, long videoId,
                        long quizId, long enrollmentId, long firstQuestion, long secondQuestion) {
    }
}
