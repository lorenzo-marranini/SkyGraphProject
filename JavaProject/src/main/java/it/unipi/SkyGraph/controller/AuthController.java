package it.unipi.SkyGraph.controller;

import it.unipi.SkyGraph.dto.user.UserLoginDto;
import it.unipi.SkyGraph.dto.user.UserRegistrationDto;
import it.unipi.SkyGraph.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationDto registrationDto) {
        authService.registerUser(registrationDto);
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody UserLoginDto loginDto) {
        String token = authService.loginUser(loginDto);
        return ResponseEntity.ok(token);
    }
}