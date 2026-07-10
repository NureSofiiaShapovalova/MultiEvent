package nure.com.multievent.catalog.web;

import nure.com.multievent.catalog.application.EventSearchCriteria;
import org.springframework.stereotype.Component;

@Component
public class EventCatalogWebMapper {

    public EventSearchCriteria toCriteria(EventSearchForm form) {
        return new EventSearchCriteria(
                form.getKeyword(),
                form.getCategoryId(),
                form.getCity(),
                form.getDateFrom(),
                form.getDateTo(),
                form.getSort());
    }
}
