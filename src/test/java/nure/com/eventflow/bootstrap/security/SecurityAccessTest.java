package nure.com.eventflow.bootstrap.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import nure.com.eventflow.catalog.application.GetHomepageSectionsService;
import nure.com.eventflow.catalog.application.HomepageView;
import nure.com.eventflow.shared.web.HomeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetHomepageSectionsService getHomepageSectionsService;

    @Test
    void permitsHomeForAnonymousUsers() throws Exception {
        when(getHomepageSectionsService.getHomepage()).thenReturn(new HomepageView(List.of(), List.of()));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/index"));
    }

    @Test
    void redirectsAnonymousUsersFromProfileAreaToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void redirectsAnonymousUsersFromOrganizerAreaToLogin() throws Exception {
        mockMvc.perform(get("/organizer/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void forbidsRegularUsersFromOrganizerArea() throws Exception {
        mockMvc.perform(get("/organizer/dashboard").with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsOrganizersThroughSecurityForOrganizerArea() throws Exception {
        mockMvc.perform(get("/organizer/dashboard").with(user("organizer@example.com").roles("ORGANIZER")))
                .andExpect(status().isNotFound());
    }

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
    void allowsAdminsThroughSecurityForAdminArea() throws Exception {
        mockMvc.perform(get("/admin").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}
