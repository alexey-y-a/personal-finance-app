package ru.financeapp.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.financeapp.exceptions.InvalidCredentialsException;
import ru.financeapp.exceptions.InvalidInputException;
import ru.financeapp.exceptions.UserNotFoundException;
import ru.financeapp.infra.Storage;

class UserServiceTest {
    private UserService service;
    @Mock private Storage storage;
    private User mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UserService(storage);
        mockUser = new User("test", "pass");
    }

    @Test
    void register_Valid_SavesUser() {
        when(storage.findUser("test")).thenReturn(null);
        assertDoesNotThrow(() -> service.register("test", "pass"));
        verify(storage).saveUser(any(User.class));
    }

    @Test
    void register_EmptyUsername_Throws() {
        assertThrows(InvalidInputException.class, () -> service.register("", "pass"));
    }

    @Test
    void register_EmptyPassword_Throws() {
        assertThrows(InvalidInputException.class, () -> service.register("test", ""));
    }

    @Test
    void register_Exists_Throws() {
        when(storage.findUser("test")).thenReturn(mockUser);
        assertThrows(InvalidInputException.class, () -> service.register("test", "pass"));
    }

    @Test
    void login_Valid_ReturnsUser() {
        when(storage.findUser("test")).thenReturn(mockUser);
        User logged = service.login("test", "pass");
        assertEquals(mockUser, logged);
        assertTrue(service.isLoggedIn());
    }

    @Test
    void login_NotFound_Throws() {
        when(storage.findUser("test")).thenReturn(null);
        assertThrows(UserNotFoundException.class, () -> service.login("test", "pass"));
    }

    @Test
    void login_WrongPass_Throws() {
        when(storage.findUser("test")).thenReturn(mockUser);
        assertThrows(InvalidCredentialsException.class, () -> service.login("test", "wrong"));
    }

    @Test
    void logout_ResetsCurrentUser() {
        when(storage.findUser("test")).thenReturn(mockUser);
        service.login("test", "pass");
        service.logout();
        assertFalse(service.isLoggedIn());
    }

    @Test
    void isLoggedIn_FalseBeforeLogin() {
        assertFalse(service.isLoggedIn());
    }
}
