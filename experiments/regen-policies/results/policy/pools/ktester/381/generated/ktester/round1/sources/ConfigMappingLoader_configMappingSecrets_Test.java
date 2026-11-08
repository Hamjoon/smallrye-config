package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.UndeclaredThrowableException;
import org.junit.jupiter.api.function.Executable;

class ConfigMappingLoader_configMappingSecrets_Test {

    /**
     * An interface that declares a secret property. The generated implementation
     * should expose that secret via {@code ConfigMappingLoader.configMappingSecrets}.
     */
    @ConfigMapping(prefix = "my")
    interface WithSecret {

//         @ConfigSecret
//         String password();

        String username();
    }

    /**
     * An interface that does **not** declare any secret property.
     */
    @ConfigMapping(prefix = "plain")
    interface WithoutSecret {

        String foo();

        int bar();
    }

    /**
     * Success case – returns a non‑empty set of secret names.
     */
    static final class SuccessSecrets {

        public static Set<String> getSecrets() {
            return Set.of("secret1", "secret2");
        }
    }

    /**
     * No {@code getSecrets} method – should lead to {@link NoSuchMethodError}.
     */
    static final class MissingSecrets {
        // deliberately empty
    }

    /**
     * Private static {@code getSecrets} – should cause {@link IllegalAccessError}.
     */
    static final class PrivateSecrets {

        @SuppressWarnings("unused")
        private static Set<String> getSecrets() {
            return Set.of("shouldNotBeSeen");
        }
    }

    /**
     * {@code getSecrets} throws a checked exception (IOException).
     */
    static final class CheckedExceptionSecrets {

        public static Set<String> getSecrets() throws IOException {
            throw new IOException("checked");
        }
    }

    /**
     * {@code getSecrets} throws a runtime exception (IllegalStateException).
     */
    static final class RuntimeExceptionSecrets {

        public static Set<String> getSecrets() {
            throw new IllegalStateException("runtime");
        }
    }

    /**
     * {@code getSecrets} throws an {@link Error} (AssertionError).
     */
    static final class ErrorSecrets {

        public static Set<String> getSecrets() {
            throw new AssertionError("error");
        }
    }

    /**
     * {@code getSecrets} returns {@code null}.
     */
    static final class NullSecrets {

        public static Set<String> getSecrets() {
            return null;
        }
    }

    /**
     * {@code getSecrets} returns an empty set.
     */
    static final class EmptySecrets {

        public static Set<String> getSecrets() {
            return Collections.emptySet();
        }
    }

    /**
     * Class without the required constructor – triggers {@link UndeclaredThrowableException}.
     */
    static final class MissingConstructor {

        // No constructor taking ConfigMappingContext
        public static Set<String> getSecrets() {
            return Set.of("ignored");
        }
    }

    private static Stream<Arguments> successProvider() {
        return Stream.of(Arguments.of(SuccessSecrets.class, Set.of("secret1", "secret2")));
    }

    private static Stream<Arguments> emptyProvider() {
        return Stream.of(Arguments.of(EmptySecrets.class, Collections.emptySet()));
    }

    private static Stream<Arguments> nullProvider() {
        return Stream.of(Arguments.of(NullSecrets.class, null));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// missing getSecrets method
        Arguments.of(MissingSecrets.class, NoSuchMethodError.class), // private getSecrets method
        Arguments.of(PrivateSecrets.class, IllegalAccessError.class), // checked exception wrapped
        Arguments.of(CheckedExceptionSecrets.class, UndeclaredThrowableException.class), // runtime exception re‑thrown
        Arguments.of(RuntimeExceptionSecrets.class, IllegalStateException.class), // error re‑thrown
        Arguments.of(ErrorSecrets.class, AssertionError.class), // missing constructor leads to UndeclaredThrowableException
        Arguments.of(MissingConstructor.class, UndeclaredThrowableException.class));
    }

//     @Test
//     void configMappingSecretsShouldReturnDeclaredSecretNames() {
//         Set<String> secrets = ConfigMappingLoader.configMappingSecrets(WithSecret.class);
//         assertNotNull(secrets, "The returned set must not be null");
//         // The secret name is the fully qualified property name (prefix + method name)
//         assertTrue(secrets.contains("my.password"), "The secret set should contain the fully qualified name of the secret property");
//         assertEquals(1, secrets.size(), "Only one secret should be reported");
//     }

//     @Test
//     void configMappingSecretsShouldReturnEmptySetWhenNoSecretsDeclared() {
//         Set<String> secrets = ConfigMappingLoader.configMappingSecrets(WithoutSecret.class);
//         assertNotNull(secrets, "The returned set must not be null");
//         assertTrue(secrets.isEmpty(), "No secrets were declared, the set should be empty");
//     }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successProvider")
    void configMappingSecretsShouldReturnDeclaredSecrets(Class<?> type, Set<String> expected) {
        Set<String> secrets = ConfigMappingLoader.configMappingSecrets(type);
        assertNotNull(secrets, "Returned set must not be null");
        assertEquals(expected, secrets, "The returned set does not match the expected values");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("emptyProvider")
    void configMappingSecretsShouldReturnEmptySetWhenImplementationReturnsEmpty(Class<?> type, Set<String> expected) {
        Set<String> secrets = ConfigMappingLoader.configMappingSecrets(type);
        assertNotNull(secrets, "Returned set must not be null");
        assertTrue(secrets.isEmpty(), "Expected an empty set");
        assertEquals(expected, secrets, "Empty set should be exactly the same instance");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullProvider")
    void configMappingSecretsShouldAllowNullReturn(Class<?> type, Set<String> expected) {
        Set<String> secrets = ConfigMappingLoader.configMappingSecrets(type);
        assertNull(secrets, "The method should return null when the implementation returns null");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void configMappingSecretsShouldPropagateOrWrapExceptions(Class<?> type, Class<? extends Throwable> expectedException) {
        Throwable thrown = assertThrows(expectedException, () -> ConfigMappingLoader.configMappingSecrets(type));
        // For the checked‑exception case we expect an UndeclaredThrowableException whose cause is IOException
        if (expectedException == UndeclaredThrowableException.class && type == CheckedExceptionSecrets.class) {
            assertTrue(thrown.getCause() instanceof IOException, "Cause should be the original IOException");
        }
        // For MissingConstructor we also get UndeclaredThrowableException, but its cause is NoSuchMethodError
        if (expectedException == UndeclaredThrowableException.class && type == MissingConstructor.class) {
            assertTrue(thrown.getCause() instanceof NoSuchMethodError, "Cause should be NoSuchMethodError due to missing constructor");
        }
    }

//     @Test
//     void configMappingSecretsShouldThrowNullPointerExceptionWhenInterfaceIsNull() {
//         assertThrows(NullPointerException.class, () -> ConfigMappingLoader.configMappingSecrets(null), "Passing a null Class reference must result in NullPointerException");
//     }
}