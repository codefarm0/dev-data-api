package in.codefarm.devdataapi.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import in.codefarm.devdataapi.domain.user.User;
import in.codefarm.devdataapi.domain.user.UserCreateRequest;
import in.codefarm.devdataapi.domain.user.UserUpdateRequest;
import in.codefarm.devdataapi.storage.JsonResourceRepository;

@Repository
public class UserRepository {

    private static final String RESOURCE_NAME = "users";

    private final JsonResourceRepository repository;

    public UserRepository(JsonResourceRepository repository) {
        this.repository = repository;
    }

    public List<User> findAll() {
        return repository.readList(RESOURCE_NAME, User.class).stream()
                .sorted(Comparator.comparing(User::id))
                .toList();
    }

    public Optional<User> findById(Long id) {
        return findAll().stream()
                .filter(user -> user.id().equals(id))
                .findFirst();
    }

    public User create(UserCreateRequest request) {
        List<User> users = new ArrayList<>(findAll());
        long nextId = users.stream()
                .map(User::id)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1L;

        User user = new User(nextId, request.firstName(), request.lastName(), request.email(), request.status());
        if (users.stream().anyMatch(existing -> existing.id().equals(user.id()))) {
            throw new IllegalStateException("Duplicate user id detected: " + user.id());
        }

        users.add(user);
        repository.writeList(RESOURCE_NAME, users);
        return user;
    }

    public User update(Long id, UserUpdateRequest request) {
        List<User> users = new ArrayList<>(findAll());
        for (int i = 0; i < users.size(); i++) {
            User current = users.get(i);
            if (current.id().equals(id)) {
                User updated = new User(id, request.firstName(), request.lastName(), request.email(), request.status());
                users.set(i, updated);
                repository.writeList(RESOURCE_NAME, users);
                return updated;
            }
        }
        throw new IllegalArgumentException("User not found: " + id);
    }

    public boolean delete(Long id) {
        List<User> users = new ArrayList<>(findAll());
        boolean removed = users.removeIf(user -> user.id().equals(id));
        if (removed) {
            repository.writeList(RESOURCE_NAME, users);
        }
        return removed;
    }
}
