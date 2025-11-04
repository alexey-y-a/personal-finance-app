package ru.financeapp.infra;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import ru.financeapp.core.User;
import ru.financeapp.core.Wallet;

public class FileJsonStorage implements Storage {
    protected String walletsDir = "wallets/";
    private static final String USERS_FILE = "users.json";
    private final Gson gson = new Gson();

    public FileJsonStorage() {
        initDirs();
    }

    public FileJsonStorage(String customWalletsDir) {
        this.walletsDir = customWalletsDir;
        initDirs();
    }

    private void initDirs() {
        try {
            Files.createDirectories(Paths.get(walletsDir));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create wallets dir", e);
        }
    }

    @Override
    public void saveUser(User user) {
        Map<String, String> users = loadUsers();
        users.put(user.getUsername(), user.getPassword());
        Path usersPath = getUsersFilePath();
        try (FileWriter writer = new FileWriter(usersPath.toString())) {
            gson.toJson(users, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save user", e);
        }
        saveWallet(user);
    }

    @Override
    public User findUser(String username) {
        @SuppressWarnings("unchecked")
        Map<String, String> users = loadUsers();
        String pass = users.get(username);
        if (pass == null) return null;
        User user = new User(username, pass);
        loadWallet(user);
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        @SuppressWarnings("unchecked")
        Map<String, String> users = loadUsers();
        return users.keySet().stream()
                .map(this::findUser)
                .filter(u -> u != null)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void loadWallet(User user) {
        Path file = Paths.get(getWalletsDir() + user.getUsername() + ".wallet.json");
        if (!Files.exists(file)) {
            user.setWallet(new Wallet());
            return;
        }
        try (FileReader reader = new FileReader(file.toString())) {
            Wallet wallet = gson.fromJson(reader, Wallet.class);
            user.setWallet(wallet != null ? wallet : new Wallet());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load wallet", e);
        }
    }

    @Override
    public void saveWallet(User user) {
        if (user.getWallet() == null) user.setWallet(new Wallet());
        Path dir = Paths.get(getWalletsDir());
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create wallet dir", e);
        }
        Path file = dir.resolve(user.getUsername() + ".wallet.json");
        try (FileWriter writer = new FileWriter(file.toString())) {
            gson.toJson(user.getWallet(), writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save wallet", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, String> loadUsers() {
        Path file = getUsersFilePath();
        if (!Files.exists(file)) return new HashMap<>();
        try (FileReader reader = new FileReader(file.toString())) {
            return gson.fromJson(reader, Map.class);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    protected String getWalletsDir() {
        return walletsDir;
    }

    protected Path getUsersFilePath() {
        return Paths.get(USERS_FILE);
    }
}
