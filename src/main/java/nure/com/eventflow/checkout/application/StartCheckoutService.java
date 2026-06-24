package nure.com.eventflow.checkout.application;

import nure.com.eventflow.catalog.application.EventDetailsView;
import nure.com.eventflow.catalog.application.GetPublishedEventDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StartCheckoutService {

    private final GetPublishedEventDetailsService getPublishedEventDetailsService;

    public StartCheckoutService(GetPublishedEventDetailsService getPublishedEventDetailsService) {
        this.getPublishedEventDetailsService = getPublishedEventDetailsService;
    }

    @Transactional(readOnly = true)
    public EventDetailsView getBySlug(String slug) {
        return getPublishedEventDetailsService.getBySlug(slug);
    }
}
