package nure.com.eventflow.admin.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCategoryCommand(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 120) String slug
) {
}
