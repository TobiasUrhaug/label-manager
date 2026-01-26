package org.omt.labelmanager.identity.user.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.identity.user.EmailAlreadyExistsException;
import org.omt.labelmanager.identity.user.UserCRUDHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(org.omt.labelmanager.config.SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserCRUDHandler userCRUDHandler;

    @Test
    void loginPage_isAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void registerPage_isAccessible() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registrationForm"));
    }

    @Test
    void register_createsUserAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "new@example.com")
                        .param("password", "password123")
                        .param("displayName", "New User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userCRUDHandler).registerUser("new@example.com", "password123", "New User");
    }

    @Test
    void register_showsError_whenEmailExists() throws Exception {
        doThrow(new EmailAlreadyExistsException("existing@example.com"))
                .when(userCRUDHandler)
                .registerUser(anyString(), anyString(), anyString());

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "existing@example.com")
                        .param("password", "password123")
                        .param("displayName", "Existing User"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("error"));
    }
}
