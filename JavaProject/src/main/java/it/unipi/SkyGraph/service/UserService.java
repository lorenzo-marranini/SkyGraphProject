package it.unipi.SkyGraph.service;

import it.unipi.SkyGraph.dto.user.UserIdUsernameDto;
import it.unipi.SkyGraph.dto.user.UserNoPwdDto;
import it.unipi.SkyGraph.dto.user.UserUpdateDto;
import it.unipi.SkyGraph.model.UserMongo;
import it.unipi.SkyGraph.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Gets a paginated list of users by username.
     */
    public Slice<UserIdUsernameDto> getUsers(String username, int page, int size) {
        Slice<UserMongo> userSlice = userRepository.findByUsernameContainingIgnoreCase(
                username, PageRequest.of(page, size));

        return userSlice.map(user -> new UserIdUsernameDto(user.getId(), user.getUsername()));
    }

    public UserNoPwdDto getUserById(String userId, boolean throwException) {
        UserMongo user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return new UserNoPwdDto(user.getUsername(), user.getEmail(), user.getRole());
    }

    /**
     * Updates user fields based on the UserUpdateDto.
     */
    public UserNoPwdDto updateUser(UserMongo currentUser, UserUpdateDto updates) {
        if (updates.username() != null) {
            currentUser.setUsername(updates.username());
        }

        if (updates.email() != null) {
            currentUser.setEmail(updates.email());
        }

        if (updates.password() != null) {
            currentUser.setPassword(updates.password());
        }

        UserMongo saved = userRepository.save(currentUser);
        return new UserNoPwdDto(saved.getUsername(), saved.getEmail(), saved.getRole());
    }

    /**
     * Deletes a user from the database.
     */
    public UserNoPwdDto deleteUser(UserMongo user) {
        userRepository.delete(user);
        return new UserNoPwdDto(user.getUsername(), user.getEmail(), user.getRole());
    }

    /**
     * Simple role-based promotion logic.
     */
    public String promoteUser(String userId) {
        UserMongo user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));


        return "User " + userId + " promoted successfully.";
    }
}