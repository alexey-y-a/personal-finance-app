package ru.financeapp.core;

import ru.financeapp.exceptions.InvalidCredentialsException;
import ru.financeapp.exceptions.InvalidInputException;
import ru.financeapp.exceptions.UserNotFoundException;
import ru.financeapp.infra.Storage;

public class UserService {
    private final Storage storage;
    private User currentUser;

    public UserService(Storage storage) {
        this.storage = storage;
    }

    public void register(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            throw new InvalidInputException("Username and password cannot be empty");
        }
        if (storage.findUser(username) != null) {
            throw new InvalidInputException("User already exists");
        }
        storage.saveUser(new User(username, password));
    }

    public User login(String username, String password) {
        User user = storage.findUser(username);
        if (user == null) {
            throw new UserNotFoundException("User not found: " + username);
        }
        if (!user.getPassword().equals(password)) {
            throw new InvalidCredentialsException("Invalid password");
        }
        currentUser = user;
        return user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public Storage getStorage() {
        return storage;
    }
}
