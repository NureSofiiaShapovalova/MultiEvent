package nure.com.multievent.checkout.web;

import java.security.Principal;
import java.util.UUID;
import nure.com.multievent.checkout.application.GetUserOrderDetailsService;
import nure.com.multievent.checkout.application.ListUserOrdersService;
import nure.com.multievent.checkout.application.UserOrderDetailsView;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class OrderController {

    private final ListUserOrdersService listUserOrdersService;
    private final GetUserOrderDetailsService getUserOrderDetailsService;

    public OrderController(
            ListUserOrdersService listUserOrdersService,
            GetUserOrderDetailsService getUserOrderDetailsService
    ) {
        this.listUserOrdersService = listUserOrdersService;
        this.getUserOrderDetailsService = getUserOrderDetailsService;
    }

    @GetMapping("/orders")
    String list(Principal principal, Model model) {
        model.addAttribute("orders", listUserOrdersService.list(principal.getName()));
        return "orders/list";
    }

    @GetMapping("/orders/{orderId}")
    String details(Principal principal, @PathVariable UUID orderId, Model model) {
        UserOrderDetailsView order = getUserOrderDetailsService.getById(principal.getName(), orderId);
        model.addAttribute("order", order);
        return "orders/details";
    }
}
