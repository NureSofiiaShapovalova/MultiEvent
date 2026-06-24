package nure.com.eventflow.shared.web;

import nure.com.eventflow.catalog.application.GetHomepageSectionsService;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final GetHomepageSectionsService getHomepageSectionsService;

    public HomeController(GetHomepageSectionsService getHomepageSectionsService) {
        this.getHomepageSectionsService = getHomepageSectionsService;
    }

    @GetMapping("/")
    String home(Model model) {
        model.addAttribute("homepageView", getHomepageSectionsService.getHomepage());
        return "home/index";
    }
}
