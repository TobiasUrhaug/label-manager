package org.omt.labelmanager.identity.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.identity.api.user.EmailAlreadyExistsException;
import org.omt.labelmanager.identity.api.user.UserCommandApi;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RegisterController.class)
@Import(TestSecurityConfig.class)
class RegisterControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserCommandApi userCommandApi;

    @Test
    void register_returns201AndNoBody() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "new@example.com",
                                          "password": "secret",
                                          "displayName": "New User"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        verify(userCommandApi).registerUser("new@example.com", "secret", "New User");
    }

    @Test
    void register_returns400ProblemDetail_whenEmailMissing() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "password": "secret",
                                          "displayName": "No Email"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(userCommandApi);
    }

    @Test
    void register_returns400ProblemDetail_whenBodyIsMalformed() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(userCommandApi);
    }

    @Test
    void register_returns409ProblemDetail_whenEmailTaken() throws Exception {
        doThrow(new EmailAlreadyExistsException("taken@example.com"))
                .when(userCommandApi)
                .registerUser(any(), any(), any());

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "taken@example.com",
                                          "password": "secret",
                                          "displayName": "Taken"
                                        }
                                        """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("An account with this email already exists."))
                .andExpect(jsonPath("$.properties").doesNotExist());
    }
}
