package it.unipi.SkyGraph.dto.user;

import it.unipi.SkyGraph.enums.Role;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Date;

public record UserUpdateDto(
        @NotEmpty
        String username,
        @Min(4)
        String password,
        @Email
        String email,
        Role role
){}