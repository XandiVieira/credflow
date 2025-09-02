package com.relyon.credflow.model.user;

import com.relyon.credflow.validation.PasswordMatches;
import com.relyon.credflow.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@PasswordMatches
public class UserRequestDTO {

    @NotBlank(message = "{user.name.required}")
    private String name;

    @Email(message = "{user.email.invalid}")
    @NotBlank(message = "{user.email.required}")
    private String email;

    @NotBlank(message = "{user.password.required}")
    @StrongPassword
    private String password;

    @NotBlank(message = "{user.confirm.required}")
    private String confirmPassword;

    private String inviteCode;
}