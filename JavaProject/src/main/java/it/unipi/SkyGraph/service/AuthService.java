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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Autowired
    public AuthService(AuthenticationManager authManager,
                       UserRepository userRepository,
                       PasswordEncoder encoder) {
        this.authManager = authManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public void registerUser(UserRegistrationDto user) {
        if (userRepository.existsByUsername(user.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(user.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String userId = UUID.randomUUID().toString();

        UserMongo newUserMongo = new UserMongo();
        newUserMongo.setId(userId);
        newUserMongo.setUsername(user.username());
        newUserMongo.setPassword(encoder.encode(user.password()));
        newUserMongo.setEmail(user.email());

        newUserMongo.setRole(Role.REGISTERED_USER);

        userRepository.save(newUserMongo);
    }

    public String loginUser(UserLoginDto user) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.email(), user.password())
        );

        if (auth.isAuthenticated()) {
            UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

            //Get roles for the user
            List<String> roles = userPrincipal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            //Generate JWT with both UserID and user role
            return JwtUtils.generateToken(userPrincipal.getUser().getId(), roles);
        }

        throw new RuntimeException("Invalid credentials");
    }
}