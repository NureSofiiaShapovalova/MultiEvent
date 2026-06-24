package nure.com.eventflow.notifications.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import nure.com.eventflow.bootstrap.security.SecurityConfig;
import nure.com.eventflow.notifications.application.NotificationPreferencesCommand;
import nure.com.eventflow.notifications.application.NotificationPreferencesView;
import nure.com.eventflow.notifications.application.UpdateNotificationPreferencesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationPreferencesController.class)
@Import(SecurityConfig.class)
class NotificationPreferencesControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UpdateNotificationPreferencesService updateNotificationPreferencesService;

    @Test
    void redirectsAnonymousUserToLogin() throws Exception {
        mockMvc.perform(get("/profile/notifications"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void showsNotificationPreferencesForAuthenticatedUser() throws Exception {
        when(updateNotificationPreferencesService.getOrCreate("sofi@example.com"))
                .thenReturn(new NotificationPreferencesView(true, false, true));

        mockMvc.perform(get("/profile/notifications").with(user("sofi@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/notifications"))
                .andExpect(model().attributeExists("notificationPreferencesForm"));
    }

    @Test
    void updatesNotificationPreferencesAndRedirectsBackToProfileNotifications() throws Exception {
        when(updateNotificationPreferencesService.update(anyString(), any(NotificationPreferencesCommand.class)))
                .thenReturn(new NotificationPreferencesView(false, true, false));

        mockMvc.perform(post("/profile/notifications")
                        .with(user("sofi@example.com").roles("USER"))
                        .with(csrf())
                        .param("emailEnabled", "false")
                        .param("orderUpdatesEnabled", "true")
                        .param("marketingEnabled", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/notifications"));

        verify(updateNotificationPreferencesService).update(anyString(), any(NotificationPreferencesCommand.class));
    }

    @Test
    void rejectsInvalidBooleanPayloads() throws Exception {
        mockMvc.perform(post("/profile/notifications")
                        .with(user("sofi@example.com").roles("USER"))
                        .with(csrf())
                        .param("emailEnabled", "not-a-boolean"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/notifications"))
                .andExpect(model().attributeHasFieldErrors("notificationPreferencesForm", "emailEnabled"));

        verify(updateNotificationPreferencesService, never()).update(anyString(), any(NotificationPreferencesCommand.class));
    }
}
