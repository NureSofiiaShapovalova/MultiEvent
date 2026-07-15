package nure.com.multievent.iam.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Set;
import java.util.UUID;
import nure.com.multievent.bootstrap.security.SecurityConfig;
import nure.com.multievent.iam.application.ChangePasswordCommand;
import nure.com.multievent.iam.application.ChangePasswordService;
import nure.com.multievent.iam.application.CurrentUserView;
import nure.com.multievent.iam.application.FindCurrentUserService;
import nure.com.multievent.iam.application.UpdateProfileCommand;
import nure.com.multievent.iam.application.UpdateProfileService;
import nure.com.multievent.iam.domain.RoleName;
import nure.com.multievent.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfileController.class)
@Import({ProfileWebMapper.class, SecurityConfig.class})
class ProfileControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindCurrentUserService findCurrentUserService;

    @MockitoBean
    private UpdateProfileService updateProfileService;

    @MockitoBean
    private ChangePasswordService changePasswordService;

    @Test
    void redirectsAnonymousUserFromProfileToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void showsProfileForAuthenticatedUser() throws Exception {
        CurrentUserView currentUser = currentUser();
        when(findCurrentUserService.findByEmail("sofi@example.com")).thenReturn(currentUser);

        mockMvc.perform(get("/profile").with(user("sofi@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/view"))
                .andExpect(model().attribute("currentUser", currentUser));
    }

    @Test
    void showsEditProfileFormForAuthenticatedUser() throws Exception {
        when(findCurrentUserService.findByEmail("sofi@example.com")).thenReturn(currentUser());

        mockMvc.perform(get("/profile/edit").with(user("sofi@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("profileEditForm"));
    }

    @Test
    void updatesProfileAndRedirectsToProfile() throws Exception {
        CurrentUserView updatedUser = currentUser();
        when(updateProfileService.updateByEmail(any(), any(UpdateProfileCommand.class))).thenReturn(updatedUser);

        mockMvc.perform(post("/profile/edit")
                        .with(user("sofi@example.com").roles("USER"))
                        .with(csrf())
                        .param("firstName", "Sofi")
                        .param("lastName", "Shapovalova")
                        .param("city", "Kharkiv"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(updateProfileService).updateByEmail(any(), any(UpdateProfileCommand.class));
    }

    @Test
    void returnsEditProfileWhenFormIsInvalid() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .with(user("sofi@example.com").roles("USER"))
                        .with(csrf())
                        .param("firstName", "x".repeat(101)))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeHasFieldErrors("profileEditForm", "firstName"));
    }

    @Test
    void redirectsAnonymousUserFromPasswordChangeToLogin() throws Exception {
        mockMvc.perform(get("/profile/password"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void showsChangePasswordFormForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/profile/password").with(user("sofi@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/change-password"))
                .andExpect(model().attributeExists("changePasswordForm"));
    }

    @Test
    void changesPasswordAndRedirectsToProfile() throws Exception {
        mockMvc.perform(post("/profile/password")
                        .with(user("sofi@example.com").roles("USER"))
                        .with(csrf())
                        .param("currentPassword", "old-password")
                        .param("newPassword", "new-password")
                        .param("confirmPassword", "new-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(changePasswordService).changeByEmail(eq("sofi@example.com"), any(ChangePasswordCommand.class));
    }

    @Test
    void returnsChangePasswordWhenConfirmationDoesNotMatch() throws Exception {
        mockMvc.perform(post("/profile/password")
                        .with(user("sofi@example.com").roles("USER"))
                        .with(csrf())
                        .param("currentPassword", "old-password")
                        .param("newPassword", "new-password")
                        .param("confirmPassword", "different-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/change-password"))
                .andExpect(model().attributeHasFieldErrors("changePasswordForm", "confirmPassword"));

        verify(changePasswordService, never()).changeByEmail(any(), any(ChangePasswordCommand.class));
    }

    @Test
    void returnsChangePasswordWhenCurrentPasswordIsInvalid() throws Exception {
        doThrow(new BusinessRuleViolationException("Current password is incorrect."))
                .when(changePasswordService)
                .changeByEmail(any(), any(ChangePasswordCommand.class));

        mockMvc.perform(post("/profile/password")
                        .with(user("sofi@example.com").roles("USER"))
                        .with(csrf())
                        .param("currentPassword", "wrong-password")
                        .param("newPassword", "new-password")
                        .param("confirmPassword", "new-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/change-password"))
                .andExpect(model().attributeHasFieldErrors("changePasswordForm", "currentPassword"));
    }

    private static CurrentUserView currentUser() {
        return new CurrentUserView(
                UUID.randomUUID(),
                "sofi@example.com",
                "Sofi",
                "Shapovalova",
                "+380000000000",
                "Kharkiv",
                "Bio",
                Set.of(RoleName.ROLE_USER));
    }
}
