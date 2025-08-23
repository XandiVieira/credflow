package com.relyon.credflow.model.account;

import lombok.Data;

import java.util.List;

@Data
public class AccountResponseDTO {
    private Long id;
    private String name;
    private String description;
    private List<Long> userIds;
}