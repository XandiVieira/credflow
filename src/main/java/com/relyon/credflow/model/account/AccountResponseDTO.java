package com.relyon.credflow.model.account;

import java.util.List;
import lombok.Data;

@Data
public class AccountResponseDTO {
    private Long id;
    private String name;
    private String description;
    private List<Long> userIds;
    private String inviteCode;
}