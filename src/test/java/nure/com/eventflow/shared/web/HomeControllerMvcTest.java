package nure.com.eventflow.shared.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import nure.com.eventflow.bootstrap.security.SecurityConfig;
import nure.com.eventflow.catalog.application.EventCardView;
import nure.com.eventflow.catalog.application.GetHomepageSectionsService;
import nure.com.eventflow.catalog.application.HomepageView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
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
}
