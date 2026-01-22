package com.relyon.credflow.model.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class CategoryRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    private List<Long> defaultResponsibleUserIds;

    private Long parentCategoryId;
}