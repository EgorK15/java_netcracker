package com.example.lms.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenApiCustomizer describeResponses() {
        return api -> {
            var problem = ModelConverters.getInstance(true).readAllAsResolvedSchema(ProblemDetail.class);
            problem.referencedSchemas.forEach(api.getComponents()::addSchemas);

            var createCourse = api.getPaths().get("/api/courses").getPost().getResponses();
            createCourse.get("201").setDescription("Курс создан");
            createCourse.addApiResponse("400", new ApiResponse().description("Некорректные данные")
                    .content(new Content().addMediaType("application/problem+json", new MediaType().schema(problem.schema))));
            createCourse.addApiResponse("404", new ApiResponse().description("Инструктор не найден")
                    .content(new Content().addMediaType("application/problem+json", new MediaType().schema(problem.schema))));

            var enroll = api.getPaths().get("/api/enrollments").getPost().getResponses();
            enroll.get("201").setDescription("Запись создана");
            enroll.addApiResponse("409", new ApiResponse().description("Студент уже записан на курс")
                    .content(new Content().addMediaType("application/problem+json", new MediaType().schema(problem.schema))));

            var download = api.getPaths().get("/api/certificates/{id}/download").getGet().getResponses();
            download.addApiResponse("200", new ApiResponse().description("Текст сертификата")
                    .content(new Content().addMediaType("text/plain", new MediaType().schema(new StringSchema()))));

            var lessonType = new Discriminator().propertyName("type");
            describeLessonType(api, lessonType, "VIDEO", "VideoLessonRequest");
            describeLessonType(api, lessonType, "TEXT", "TextLessonRequest");
            describeLessonType(api, lessonType, "QUIZ", "QuizLessonRequest");
            for (var operation : List.of(api.getPaths().get("/api/courses/{courseId}/lessons").getPost(),
                    api.getPaths().get("/api/lessons/{id}").getPut())) {
                operation.getRequestBody().getContent().get("application/json").getSchema().setDiscriminator(lessonType);
            }
        };
    }

    private void describeLessonType(io.swagger.v3.oas.models.OpenAPI api, Discriminator discriminator,
                                    String type, String schemaName) {
        var schema = api.getComponents().getSchemas().get(schemaName);
        schema.addProperty("type", new StringSchema()._enum(List.of(type)));
        schema.addRequiredItem("type");
        discriminator.mapping(type, "#/components/schemas/" + schemaName);
    }
}
