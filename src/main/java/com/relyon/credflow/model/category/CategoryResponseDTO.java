package com.relyon.credflow.model.category;

import java.util.List;
import lombok.Data;

@Data
public class CategoryResponseDTO {
    private Long id;
    private String name;
    private List<Long> defaultResponsibleUserIds;
    private Long parentCategoryId;
    private String parentCategoryName;
    private List<CategoryResponseDTO> childCategories;
}