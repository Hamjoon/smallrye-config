package io.smallrye.config;

import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_constructObject_Test {

    /**
     * Mocked configuration source required by the constructor.
     */
//     @Mock
    SmallRyeConfig mockConfig;

    /**
     * The context under test.
     */
    ConfigMappingContext context;

    /**
     * Empty mappings map – the constructor tolerates an empty map.
     */
    private static final Map<Class<?>, Set<String>> EMPTY_MAPPINGS = Collections.emptyMap();

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the context with a {@code null} SmallRyeConfig and an empty mappings map.
        // The constructor performs no heavy work when the mappings are empty.
        context = new ConfigMappingContext(null, EMPTY_MAPPINGS);
    }

    /**
     * Provides valid path arguments as described in the JSON test cases.
     */
    private static Stream<Arguments> validPathProvider() {
        return Stream.of(Arguments.of("my.config.path"), Arguments.of(""));
    }

    /**
     * Provides invalid path arguments as described in the JSON test cases.
     */
    private static Stream<Arguments> invalidPathProvider() {
        return Stream.of(// only whitespace
        Arguments.of("   "), // consecutive dots
        Arguments.of("my..config"), // trailing dot
        Arguments.of("my.config."));
    }

    /**
     * Attempts to locate the field that stores the path inside {@code ObjectCreator}.
     * The implementation of {@code ObjectCreator} is not part of the public API,
     * therefore we use a best‑effort approach:
     * <ul>
     *   <li>First try a field named {@code path}.</li>
     *   <li>If not present, look for the first {@code String} field.</li>
     * </ul>
     *
     * @param creator the {@code ObjectCreator} instance
     * @return the {@link Field} representing the stored path
     * @throws NoSuchFieldException if no suitable field can be found
     */
    private static Field findPathField(Object creator) throws NoSuchFieldException {
        Class<?> clazz = creator.getClass();
        // Preferred name – most implementations use this exact name.
        try {
            return clazz.getDeclaredField("path");
        } catch (NoSuchFieldException ignored) {
            // Fall back to the first String field.
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getType() == String.class) {
                    return f;
                }
            }
            // If we reach here, the internal representation has changed.
            throw new NoSuchFieldException("Unable to locate the internal path field in ObjectCreator");
        }
    }

    @Test
    void constructObject_returnsNonNullObjectCreator() {
        String path = "my.test.path";
        // Act
        ConfigMappingContext.ObjectCreator<?> creator = context.constructObject(path);
        // Assert
        assertNotNull(creator, "constructObject should never return null");
        assertTrue(creator instanceof ConfigMappingContext.ObjectCreator, "Returned instance must be of type ConfigMappingContext.ObjectCreator");
    }

    @Test
    void constructObject_returnsDistinctInstancesForEachCall() {
        String path = "duplicate.path";
        // Act
        ConfigMappingContext.ObjectCreator<?> first = context.constructObject(path);
        ConfigMappingContext.ObjectCreator<?> second = context.constructObject(path);
        // Assert
        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second, "Each call to constructObject must produce a new ObjectCreator");
    }

    @Test
    void constructObject_acceptsNullPath() {
        // The implementation does not guard against null, so the contract is that it
        // simply forwards the argument to the ObjectCreator constructor.
        // We verify that no exception is thrown and a non‑null creator is returned.
        ConfigMappingContext.ObjectCreator<?> creator = context.constructObject(null);
        assertNotNull(creator, "constructObject should handle null path and still return an ObjectCreator");
    }

    /**
     * Valid‑path scenarios.
     *
     * <p>For each supplied path we verify:
     * <ul>
     *   <li>The returned {@code ObjectCreator} is non‑null.</li>
     *   <li>The internal path stored by the creator matches the argument.</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] path=''{0}''")
    @MethodSource("validPathProvider")
    void constructObject_withValidPath(String path) throws Exception {
        // Act
        ConfigMappingContext.ObjectCreator<?> creator = context.constructObject(path);
        // Assert – non‑null instance
        assertNotNull(creator, "ObjectCreator must not be null for a valid path");
        // Reflectively obtain the internal path field (named 'path' in the actual implementation)
        Field pathField = findPathField(creator);
        pathField.setAccessible(true);
        String internalPath = (String) pathField.get(creator);
        assertEquals(path, internalPath, "ObjectCreator should preserve the supplied path");
    }

    /**
     * Null‑path scenario – the contract (as described in the JSON) expects a
     * {@link NullPointerException} to be thrown by the underlying {@code ObjectCreator}
     * constructor.
     */
    @Test
    void constructObject_withNullPath() {
        assertThrows(NullPointerException.class, () -> context.constructObject(null), "Passing null to constructObject should result in NullPointerException");
    }

    /**
     * Invalid‑path scenarios – whitespace only, consecutive dots and trailing dot.
     * Each should trigger an {@link IllegalArgumentException}.
     */
    @ParameterizedTest(name = "[{index}] invalid path=''{0}''")
    @MethodSource("invalidPathProvider")
    void constructObject_withInvalidPath(String invalidPath) {
        assertThrows(IllegalArgumentException.class, () -> context.constructObject(invalidPath), "Path '" + invalidPath + "' is malformed and must cause IllegalArgumentException");
    }
}