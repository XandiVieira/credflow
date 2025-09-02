package com.relyon.credflow.model.user;

import com.relyon.credflow.validation.PasswordMatches;
import com.relyon.credflow.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@PasswordMatches(message = "Passwords do not match")
public class UserRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @StrongPassword
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    private String inviteCode;
}