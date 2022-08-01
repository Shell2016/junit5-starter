package ru.michaelshell.junit.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.michaelshell.junit.dto.User;
import ru.michaelshell.junit.paramresolver.UserServiceParamResolver;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag("fast")
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith({
        UserServiceParamResolver.class
})
class UserServiceTest {

    private static final User IVAN = User.of(1, "Ivan", "123");
    private static final User LENA = User.of(2, "Lena", "1234");
    private UserService userService;

//    public UserServiceTest(TestInfo testInfo) {
//        System.out.println();
//    }

    @BeforeAll
    void init() {
        System.out.println("BeforeAll: " + this);
    }

    @BeforeEach
    void prepare(UserService userService) {
        System.out.println("\nBeforeEach: " + this);
        this.userService = userService;
    }

    @Test
    void usersEmptyIfNoUserAdded() {
        System.out.println("Test1: " + this);
        var users = userService.findAll();

        assertThat(users).isEmpty();
    }

    @Test
    @Order(1)
    void usersSizeIfUserAdded() {
        System.out.println("Test2: " + this);
        userService.add(IVAN);
        userService.add(LENA);

        var users = userService.findAll();

        assertThat(users).hasSize(2);
    }


    @Test
    @Order(2)
    void usersConvertedToMapById() {
        userService.add(IVAN, LENA);

        Map<Integer, User> users = userService.getAllConvertedById();

        assertAll(
                () -> assertThat(users).containsKeys(IVAN.getId(), LENA.getId()),
                () -> assertThat(users).containsValues(IVAN, LENA)
        );
    }

    @AfterEach
    void deleteDataFromDatabase() {
        System.out.println("AfterEach: " + this);
    }

    @AfterAll
    void closeConnectionPool() {
        System.out.println("AfterAll: " + this);
    }


    @Tag("login")
    @Nested
    @DisplayName("Test user login functionality")
    class LoginTest {

        @Test
        void loginSuccessIfUserExists() {
            userService.add(IVAN);
            Optional<User> mayBeUser = userService.login(IVAN.getName(), IVAN.getPassword());

            assertThat(mayBeUser).isPresent();
            mayBeUser.ifPresent(user -> assertThat(user).isEqualTo(IVAN));
        }

        @Test
        void loginFailureIfWrongPassword() {
            userService.add(IVAN);
            Optional<User> mayBeUser = userService.login(IVAN.getName(), "dummy");

            assertThat(mayBeUser).isEmpty();
        }

        @Test
        void loginFailureIfWrongName() {
            userService.add(IVAN);
            Optional<User> mayBeUser = userService.login("dummy", IVAN.getPassword());

            assertThat(mayBeUser).isEmpty();
        }

        @Test
        void throwExceptionOnLoginIfUsernameOrPasswordIsNull() {
            assertAll(
                    () -> {
                        var exception = assertThrows(IllegalArgumentException.class, () -> userService.login(null, "dummy"));
                        assertThat(exception.getMessage()).isEqualTo("Username or password is null!");
                    },
                    () -> assertThrows(IllegalArgumentException.class, () -> userService.login("dummy", null))
            );

        }

        @ParameterizedTest
        @MethodSource("ru.michaelshell.junit.service.UserServiceTest#getArgumentsForLoginTest")
        void loginParameterizedTest(String username, String password, Optional<User> user) {
            userService.add(IVAN, LENA);

            var optionalUser = userService.login(username, password);
            assertThat(optionalUser).isEqualTo(user);
        }

    }

    static Stream<Arguments> getArgumentsForLoginTest() {
        return Stream.of(
                Arguments.of("Ivan", "123", Optional.of(IVAN)),
                Arguments.of("Lena", "1234", Optional.of(LENA)),
                Arguments.of("dummy", "123", Optional.empty()),
                Arguments.of("Ivan", "dummy", Optional.empty())
        );

    }
}
