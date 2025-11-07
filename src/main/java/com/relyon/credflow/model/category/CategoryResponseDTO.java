package com.relyon.credflow.model.category;

import lombok.Data;

import java.util.List;

@Data
public class CategoryResponseDTO {
    private Long id;
    private String name;
    private List<Long> defaultResponsibleIds;
    private Long parentCategoryId;
    private String parentCategoryName;
    private List<CategoryResponseDTO> childCategories;
}