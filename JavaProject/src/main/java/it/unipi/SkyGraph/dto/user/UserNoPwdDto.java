package it.unipi.SkyGraph.dto.user;

import it.unipi.SkyGraph.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserNoPwdDto(
        @NotBlank(message = "Username cannot be empty")
        String username,

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Must be a valid email address")
        String email,

        @NotNull(message = "Role is required")
        Role role
) {
}