package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.user.UserLoginDto;
import it.unipi.SkyGraph.dto.user.UserRegistrationDto;
import it.unipi.SkyGraph.config.JwtUtils;
import it.unipi.SkyGraph.enums.Role;
import it.unipi.SkyGraph.model.UserMongo;
import it.unipi.SkyGraph.model.UserPrincipal;
import it.unipi.SkyGraph.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    // private final UserNeo4jRepository userNeo4jRepository;

    @Autowired
    public AuthService(AuthenticationManager authManager,
                       UserRepository userRepository,
                       PasswordEncoder encoder) {
        this.authManager = authManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public void registerUser(UserRegistrationDto user) {
        // Validation using the repository from your structure
        if (userRepository.existsByUsername(user.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(user.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String userId = UUID.randomUUID().toString();

        // Creating the UserMongo entity
        UserMongo newUserMongo = new UserMongo();
        newUserMongo.setId(userId);
        newUserMongo.setUsername(user.username());
        newUserMongo.setPassword(encoder.encode(user.password()));
        newUserMongo.setEmail(user.email());
        // newUserMongo.setBirthdate(user.birthdate()); // Ensure UserMongo has this field
        newUserMongo.setRole(Role.REGISTERED_USER); // Matches your 'enums' package

        userRepository.save(newUserMongo);
    }

    public String loginUser(UserLoginDto user) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.email(), user.password())
        );

        if (auth.isAuthenticated()) {
            UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
            return JwtUtils.generateToken(userPrincipal.getUser().getId());
        }
        throw new RuntimeException("Invalid credentials");
    }
}