package nure.com.multievent.admin.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import nure.com.multievent.admin.application.AdminCategoryCommand;
import nure.com.multievent.admin.application.AdminCategoryForm;
import nure.com.multievent.admin.application.CreateCategoryService;
import nure.com.multievent.admin.application.ListAdminCategoriesService;
import nure.com.multievent.admin.application.UpdateCategoryService;
import nure.com.multievent.shared.exception.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminCategoryController {

    private final ListAdminCategoriesService listAdminCategoriesService;
    private final CreateCategoryService createCategoryService;
    private final UpdateCategoryService updateCategoryService;

    public AdminCategoryController(
            ListAdminCategoriesService listAdminCategoriesService,
            CreateCategoryService createCategoryService,
            UpdateCategoryService updateCategoryService
    ) {
        this.listAdminCategoriesService = listAdminCategoriesService;
        this.createCategoryService = createCategoryService;
        this.updateCategoryService = updateCategoryService;
    }

    @GetMapping("/admin/categories")
    String list(Model model) {
        model.addAttribute("categories", listAdminCategoriesService.list());
        return "admin/categories/list";
    }

    @GetMapping("/admin/categories/new")
    String createForm(Model model) {
        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute("categoryForm", new AdminCategoryForm());
        }
        return "admin/categories/create";
    }

    @PostMapping("/admin/categories")
    String create(
            Principal principal,
            @Valid @ModelAttribute("categoryForm") AdminCategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "admin/categories/create";
        }
        createCategoryService.create(principal.getName(), new AdminCategoryCommand(form.getName(), form.getSlug()));
        redirectAttributes.addFlashAttribute("successMessage", "Category created.");
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/categories/{categoryId}/edit")
    String editForm(@PathVariable UUID categoryId, Model model) {
        if (!model.containsAttribute("categoryForm")) {
            var category = listAdminCategoriesService.list().stream()
                    .filter(row -> row.id().equals(categoryId))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Category was not found."));
            AdminCategoryForm form = new AdminCategoryForm();
            form.setName(category.name());
            form.setSlug(category.slug());
            model.addAttribute("categoryForm", form);
            model.addAttribute("categoryId", categoryId);
        }
        return "admin/categories/edit";
    }

    @PostMapping("/admin/categories/{categoryId}")
    String update(
            Principal principal,
            @PathVariable UUID categoryId,
            @Valid @ModelAttribute("categoryForm") AdminCategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryId", categoryId);
            return "admin/categories/edit";
        }
        updateCategoryService.update(principal.getName(), categoryId, new AdminCategoryCommand(form.getName(), form.getSlug()));
        redirectAttributes.addFlashAttribute("successMessage", "Category updated.");
        return "redirect:/admin/categories";
    }
}
