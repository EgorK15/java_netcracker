package com.example.lms.exception;

import com.example.lms.controller.StudentController;
import com.example.lms.dto.CreateStudentRequest;
import com.example.lms.service.CertificateService;
import com.example.lms.service.EnrollmentService;
import com.example.lms.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiExceptionHandlerTest {
    @Test
    void databaseConflictReturns409WithoutInternalDetails() throws Exception {
        var users = mock(UserService.class);
        when(users.createStudent(any(CreateStudentRequest.class)))
                .thenThrow(new DataIntegrityViolationException("internal_constraint_name"));
        var mvc = MockMvcBuilders.standaloneSetup(
                        new StudentController(users, mock(EnrollmentService.class), mock(CertificateService.class)))
                .setControllerAdvice(new ApiExceptionHandler()).build();

        mvc.perform(post("/api/students").contentType("application/json")
                        .content("""
                                {"firstName":"Ivan","lastName":"Ivanov","email":"student@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(content().string(not(containsString("internal_constraint_name"))));
    }
}
