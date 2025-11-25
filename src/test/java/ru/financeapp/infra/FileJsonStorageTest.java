package ru.financeapp.infra;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.financeapp.core.User;
import ru.financeapp.core.Wallet;

class FileJsonStorageTest {
    private FileJsonStorage storage;
    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        Path walletsPath = tempDir.resolve("wallets");
        Path usersPath = tempDir.resolve("users.json");
        storage =
                new FileJsonStorage() {
                    @Override
                    protected String getWalletsDir() {
                        return walletsPath.toString() + "/";
                    }

                    @Override
                    protected Path getUsersFilePath() {
                        return usersPath;
                    }
                };
        try {
            Files.createDirectories(walletsPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void saveUser_Valid_CreatesFiles() throws IOException {
        User user = new User("test", "pass");
        storage.saveUser(user);
        assertTrue(Files.exists(tempDir.resolve("users.json")));
        assertTrue(Files.exists(tempDir.resolve("wallets/test.wallet.json")));
    }

    @Test
    void findUser_Valid_LoadsWithWallet() {
        User saved = new User("test", "pass");
        saved.getWallet().setBudget("food", 4000.0);
        storage.saveUser(saved);
        User found = storage.findUser("test");
        assertNotNull(found);
        assertEquals("test", found.getUsername());
        assertEquals(4000.0, found.getWallet().getBudget("food"), 0.01);
    }

    @Test
    void findUser_NotExists_ReturnsNull() {
        assertNull(storage.findUser("nonexistent"));
    }

    @Test
    void getAllUsers_ReturnsList() {
        User u1 = new User("u1", "p1");
        User u2 = new User("u2", "p2");
        storage.saveUser(u1);
        storage.saveUser(u2);
        List<User> all = storage.getAllUsers();
        assertEquals(2, all.size());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(
                            path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    System.err.println(
                                            "Warning: Failed to delete "
                                                    + path
                                                    + ": "
                                                    + e.getMessage());
                                }
                            });
        }
    }

    @Test
    void loadWallet_Existing_LoadsData() throws IOException {
        User user = new User("test", "pass");
        Wallet w = new Wallet();
        w.setBudget("food", 4000.0);
        user.setWallet(w);
        Path walletFile = tempDir.resolve("wallets/test.wallet.json");
        Files.createDirectories(walletFile.getParent());

        try (FileWriter writer = new FileWriter(walletFile.toFile())) {
            new Gson().toJson(w, writer);
        }

        storage.saveUser(user);
        storage.loadWallet(user);
        assertEquals(4000.0, user.getWallet().getBudget("food"), 0.01);
    }

    @Test
    void saveWallet_Valid_SavesJson() throws IOException {
        User user = new User("test", "pass");
        Wallet w = new Wallet();
        w.setBudget("food", 4000.0);
        user.setWallet(w);
        storage.saveWallet(user);
        Path file = tempDir.resolve("wallets/test.wallet.json");
        assertTrue(Files.exists(file));
        String content = Files.readString(file);
        assertTrue(content.contains("\"food\":4000.0"));
    }

    @Test
    void loadUsers_EmptyDir_ReturnsEmptyMap() {
        Map<String, String> users = storage.loadUsers();
        assertTrue(users.isEmpty());
    }
}
