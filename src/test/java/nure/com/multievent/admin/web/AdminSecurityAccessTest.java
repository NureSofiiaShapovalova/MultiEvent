package nure.com.multievent.admin.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import nure.com.multievent.admin.application.AdminDashboardView;
import nure.com.multievent.admin.application.GetAdminDashboardService;
import nure.com.multievent.bootstrap.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminDashboardController.class)
@Import(SecurityConfig.class)
class AdminSecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAdminDashboardService getAdminDashboardService;

    @Test
    void redirectsAnonymousUsersFromAdminAreaToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void forbidsRegularUsersFromAdminArea() throws Exception {
        mockMvc.perform(get("/admin").with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void forbidsRegularUsersFromAdminSectionsToo() throws Exception {
        mockMvc.perform(get("/admin/users").with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminsAndLoadsDashboard() throws Exception {
        when(getAdminDashboardService.get()).thenReturn(new AdminDashboardView(1, 1, 1, 1, 0, 0, 1, 0, 0, 0));

        mockMvc.perform(get("/admin").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }
}
