package nure.com.eventflow.admin.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import nure.com.eventflow.admin.application.AdminAuditLogRowView;
import nure.com.eventflow.admin.application.AdminCategoryRowView;
import nure.com.eventflow.admin.application.AdminEventRowView;
import nure.com.eventflow.admin.application.AdminPaymentIssueRowView;
import nure.com.eventflow.admin.application.AdminUserRowView;
import nure.com.eventflow.admin.application.CancelAdminEventService;
import nure.com.eventflow.admin.application.CreateCategoryService;
import nure.com.eventflow.admin.application.GrantOrganizerRoleService;
import nure.com.eventflow.admin.application.ListAdminCategoriesService;
import nure.com.eventflow.admin.application.ListAdminEventsService;
import nure.com.eventflow.admin.application.ListAdminUsersService;
import nure.com.eventflow.admin.application.ListAuditTrailService;
import nure.com.eventflow.admin.application.ListPaymentIssuesService;
import nure.com.eventflow.admin.application.ModerateEventService;
import nure.com.eventflow.admin.application.PublishAdminEventService;
import nure.com.eventflow.admin.application.RevokeOrganizerRoleService;
import nure.com.eventflow.admin.application.ToggleUserLockService;
import nure.com.eventflow.admin.application.UpdateCategoryService;
import nure.com.eventflow.bootstrap.security.SecurityConfig;
import nure.com.eventflow.catalog.domain.EventStatus;
import nure.com.eventflow.iam.domain.RoleName;
import nure.com.eventflow.payment.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AdminUserController.class, AdminCategoryController.class, AdminEventController.class, AdminPaymentController.class, AdminAuditController.class})
@Import(SecurityConfig.class)
class AdminManagementControllersMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListAdminUsersService listAdminUsersService;

    @MockitoBean
    private GrantOrganizerRoleService grantOrganizerRoleService;

    @MockitoBean
    private RevokeOrganizerRoleService revokeOrganizerRoleService;

    @MockitoBean
    private ToggleUserLockService toggleUserLockService;

    @MockitoBean
    private ListAdminCategoriesService listAdminCategoriesService;

    @MockitoBean
    private CreateCategoryService createCategoryService;

    @MockitoBean
    private UpdateCategoryService updateCategoryService;

    @MockitoBean
    private ListAdminEventsService listAdminEventsService;

    @MockitoBean
    private PublishAdminEventService publishAdminEventService;

    @MockitoBean
    private CancelAdminEventService cancelAdminEventService;

    @MockitoBean
    private ModerateEventService moderateEventService;

    @MockitoBean
    private ListPaymentIssuesService listPaymentIssuesService;

    @MockitoBean
    private ListAuditTrailService listAuditTrailService;

    @Test
    void showsUsersPageAndRoleActions() throws Exception {
        when(listAdminUsersService.list()).thenReturn(List.of(new AdminUserRowView(
                UUID.randomUUID(),
                "user@example.com",
                "Sofi",
                "Shapovalova",
                "Kyiv",
                true,
                true,
                Set.of(RoleName.ROLE_USER))));

        mockMvc.perform(get("/admin/users").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/list"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    void grantsOrganizerRoleAndRedirectsBackToUsers() throws Exception {
        when(grantOrganizerRoleService.grant(any(), any())).thenReturn(new AdminUserRowView(
                UUID.randomUUID(),
                "user@example.com",
                null,
                null,
                null,
                true,
                true,
                Set.of(RoleName.ROLE_USER, RoleName.ROLE_ORGANIZER)));

        mockMvc.perform(post("/admin/users/{userId}/roles/organizer/grant", UUID.randomUUID())
                        .with(user("admin@example.com").roles("ADMIN"))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        mockMvc.perform(post("/admin/users/{userId}/grant-organizer", UUID.randomUUID())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    void revokesOrganizerRoleAndRedirectsBackToUsers() throws Exception {
        when(revokeOrganizerRoleService.revoke(any(), any())).thenReturn(new AdminUserRowView(
                UUID.randomUUID(),
                "user@example.com",
                null,
                null,
                null,
                true,
                true,
                Set.of(RoleName.ROLE_USER)));

        mockMvc.perform(post("/admin/users/{userId}/roles/organizer/revoke", UUID.randomUUID())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        mockMvc.perform(post("/admin/users/{userId}/revoke-organizer", UUID.randomUUID())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    void locksAndUnlocksUsers() throws Exception {
        mockMvc.perform(post("/admin/users/{userId}/lock", UUID.randomUUID())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        mockMvc.perform(post("/admin/users/{userId}/unlock", UUID.randomUUID())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    void showsCategoriesPageAndValidatesCreateForm() throws Exception {
        when(listAdminCategoriesService.list()).thenReturn(List.of(new AdminCategoryRowView(UUID.randomUUID(), "Concerts", "concerts", 3)));

        mockMvc.perform(get("/admin/categories").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/list"))
                .andExpect(model().attributeExists("categories"));

        mockMvc.perform(get("/admin/categories/new").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/create"))
                .andExpect(model().attributeExists("categoryForm"));

        mockMvc.perform(post("/admin/categories")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .param("name", "")
                        .param("slug", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/create"))
                .andExpect(model().attributeHasFieldErrors("categoryForm", "name", "slug"));
    }

    @Test
    void showsCategoryEditForm() throws Exception {
        UUID categoryId = UUID.randomUUID();
        when(listAdminCategoriesService.list()).thenReturn(List.of(new AdminCategoryRowView(categoryId, "Concerts", "concerts", 3)));

        mockMvc.perform(get("/admin/categories/{categoryId}/edit", categoryId).with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/edit"))
                .andExpect(model().attributeExists("categoryForm", "categoryId"));
    }

    @Test
    void showsEventsPaymentsAndAuditPages() throws Exception {
        when(listAdminEventsService.list()).thenReturn(List.of(new AdminEventRowView(
                UUID.randomUUID(),
                "Rock Night",
                "rock-night",
                EventStatus.PUBLISHED,
                "Concerts",
                "organizer@example.com",
                "Kyiv",
                Instant.parse("2026-09-01T18:00:00Z"),
                Instant.parse("2026-09-01T21:00:00Z"),
                Instant.parse("2026-08-01T10:00:00Z"),
                "https://example.com/cover.jpg")));
        when(listPaymentIssuesService.list()).thenReturn(List.of(new AdminPaymentIssueRowView(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Rock Night",
                "buyer@example.com",
                nure.com.eventflow.checkout.domain.OrderStatus.PENDING_PAYMENT,
                PaymentStatus.FAILED,
                new BigDecimal("120.00"),
                "USD",
                "LOCAL_MANUAL",
                "local-ref",
                Instant.parse("2026-08-15T10:00:00Z"),
                Instant.parse("2026-08-15T10:02:00Z"),
                "Gateway timeout",
                1)));
        when(listAuditTrailService.list()).thenReturn(List.of(new AdminAuditLogRowView(
                UUID.randomUUID(),
                Instant.parse("2026-08-15T12:00:00Z"),
                "admin@example.com",
                "Event",
                UUID.randomUUID(),
                "PUBLISH_EVENT",
                "Published event")));

        mockMvc.perform(get("/admin/events").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/events/list"))
                .andExpect(model().attributeExists("events"));

        mockMvc.perform(post("/admin/events/{eventId}/publish", UUID.randomUUID())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/events"));

        mockMvc.perform(post("/admin/events/{eventId}/cancel", UUID.randomUUID())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/events"));

        mockMvc.perform(post("/admin/events/{eventId}/moderate", UUID.randomUUID())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .param("action", "CANCEL")
                        .param("reason", "Policy violation"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/events"));

        mockMvc.perform(get("/admin/payments/issues").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/payments/issues"))
                .andExpect(model().attributeExists("payments"));

        mockMvc.perform(get("/admin/audit").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit/list"))
                .andExpect(model().attributeExists("auditEntries"));
    }
}
