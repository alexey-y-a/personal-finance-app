package ru.financeapp.infra;

import java.util.List;

import ru.financeapp.core.User;

public interface Storage {
    void saveUser(User user);

    User findUser(String username);

    List<User> getAllUsers();

    void loadWallet(User user);

    void saveWallet(User user);
}
