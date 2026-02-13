package it.unipi.SkyGraph.repository;

import it.unipi.SkyGraph.model.UserMongo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<UserMongo, String> {
    Slice<UserMongo> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    Optional<UserMongo> findByUsername(String username);
}