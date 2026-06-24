package nure.com.eventflow.iam.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Set;
import java.util.UUID;
import nure.com.eventflow.bootstrap.security.SecurityConfig;
import nure.com.eventflow.iam.application.CurrentUserView;
import nure.com.eventflow.iam.application.RegisterUserCommand;
import nure.com.eventflow.iam.application.RegisterUserService;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({ProfileWebMapper.class, SecurityConfig.class})
class AuthControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserService registerUserService;

    @Test
    void showsLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void showsRegisterPageWithForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerForm"));
    }

    @Test
    void registersUserAndRedirectsToLogin() throws Exception {
        when(registerUserService.register(any(RegisterUserCommand.class))).thenReturn(new CurrentUserView(
                UUID.randomUUID(),
                "sofi@example.com",
                "Sofi",
                "Shapovalova",
                null,
                null,
                null,
                Set.of(RoleName.ROLE_USER)));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "sofi@example.com")
                        .param("password", "secret-password")
                        .param("firstName", "Sofi")
                        .param("lastName", "Shapovalova"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(registerUserService).register(any(RegisterUserCommand.class));
    }

    @Test
    void returnsRegisterPageWhenFormIsInvalid() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "not-an-email")
                        .param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "email", "password"));

        verify(registerUserService, never()).register(any());
    }

    @Test
    void returnsRegisterPageWhenEmailIsDuplicate() throws Exception {
        when(registerUserService.register(any(RegisterUserCommand.class)))
                .thenThrow(new BusinessRuleViolationException("A user with this email already exists."));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "sofi@example.com")
                        .param("password", "secret-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "email"));
    }
}
