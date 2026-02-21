package it.unipi.SkyGraph.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.SkyGraph.dto.user.UserIdUsernameDto;
import it.unipi.SkyGraph.dto.user.UserNoPwdDto;
import it.unipi.SkyGraph.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Search users with pagination")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> browseUsers(
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        Slice<UserIdUsernameDto> results = userService.getUsers(username, page, size);
        if (results.isEmpty()) {
            return ResponseEntity.ok("No users found matching the criteria.");
        }
        return ResponseEntity.ok(results);
    }


    @Operation(summary = "Get a specific user by ID")
    @GetMapping("/{userId}")
    public ResponseEntity<UserNoPwdDto> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserById(userId, true));
    }


    @Operation(summary = "Promote or Demote a user (ADMIN Only)")
    @PutMapping("/assign-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(
            @RequestParam String email,
            @RequestParam String roleName
    ) {
        userService.updateUserRole(email, roleName);
        return ResponseEntity.ok("User role updated successfully to " + roleName.toUpperCase());

    }
    @Operation(summary = "Delete your own account")
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails currentUser
    ) {
        String userId = currentUser.getUsername();
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}