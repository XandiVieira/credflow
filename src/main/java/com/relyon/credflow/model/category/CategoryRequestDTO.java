package com.relyon.credflow.model.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CategoryRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private List<Long> defaultResponsibleIds;

    private Long parentCategoryId;
}