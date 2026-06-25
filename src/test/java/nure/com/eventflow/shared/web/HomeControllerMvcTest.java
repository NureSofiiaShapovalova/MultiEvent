package nure.com.eventflow.shared.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import nure.com.eventflow.bootstrap.security.SecurityConfig;
import nure.com.eventflow.catalog.application.EventCardView;
import nure.com.eventflow.catalog.application.GetHomepageSectionsService;
import nure.com.eventflow.catalog.application.HomepageView;
import nure.com.eventflow.shared.config.I18nConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
@Import({SecurityConfig.class, I18nConfig.class})
class HomeControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetHomepageSectionsService getHomepageSectionsService;

    @Test
    void showsHomepageWithSections() throws Exception {
        when(getHomepageSectionsService.getHomepage()).thenReturn(new HomepageView(List.of(), List.of()));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/index"))
                .andExpect(model().attributeExists("homepageView"));
    }

    @Test
    void anonymousHomepageShowsLoginAndRegisterActions() throws Exception {
        when(getHomepageSectionsService.getHomepage()).thenReturn(new HomepageView(List.of(), List.of()));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create account")))
                .andExpect(content().string(containsString("Login")));
    }

    @Test
    void authenticatedHomepageHidesLoginAndRegisterHeroActions() throws Exception {
        when(getHomepageSectionsService.getHomepage()).thenReturn(new HomepageView(List.of(), List.of()));

        mockMvc.perform(get("/").with(user("buyer@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Create account</a>"))))
                .andExpect(content().string(not(containsString("Login</a>"))))
                .andExpect(content().string(containsString("My orders")))
                .andExpect(content().string(containsString("Profile")));
    }

    @Test
    void homepageCanRenderUkrainianLocaleFromNavbarLanguageParameter() throws Exception {
        when(getHomepageSectionsService.getHomepage()).thenReturn(new HomepageView(List.of(), List.of()));

        mockMvc.perform(get("/").param("lang", "uk"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Головна")))
                .andExpect(content().string(containsString("Створити акаунт")))
                .andExpect(content().string(containsString("Харківський національний університет радіоелектроніки")));
    }
}
