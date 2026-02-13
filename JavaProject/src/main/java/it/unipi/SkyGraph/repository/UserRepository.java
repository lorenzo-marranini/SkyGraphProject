package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.model.UserMongo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserMongo, String> {
    Slice<UserMongo> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    Optional<UserMongo> findByUsername(String username);

    boolean existsByUsername(@NotBlank(message = "Username is required") @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters") String username);

    boolean existsByEmail(@NotBlank(message = "Email is required") @Email(message = "Please provide a valid email address") String email);

    Optional<UserMongo> findByEmail(String email);
}