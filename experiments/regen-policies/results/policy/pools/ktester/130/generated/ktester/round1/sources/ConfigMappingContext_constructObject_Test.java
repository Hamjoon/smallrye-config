package io.smallrye.config;

import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_constructObject_Test {

//     @Mock
    SmallRyeConfig // mocked – no behaviour required for this test
    mockConfig;

    private ConfigMappingContext context;

    @BeforeEach
    void setupBeforeEach() {
        // No real SmallRyeConfig is needed for these tests – the constructObject method
        // does not depend on configuration data. Passing null is sufficient.
        SmallRyeConfig config = null;
        // Use the package‑private constructor that accepts the pre‑computed name map.
        // Empty maps are sufficient because the method under test does not depend on any
        // configuration data – it only creates a new ObjectCreator instance.
        Map<String, Map<String, java.util.Set<String>>> emptyNames = Collections.emptyMap();
        Map<Class<?>, java.util.Set<String>> emptyRoots = Collections.emptyMap();
        context = new ConfigMappingContext(config, emptyNames, emptyRoots);
    }

    /**
     * Provides the data for {@link #constructObject_variousInputs(String, String)}.
     *
     * The second element of each {@link Arguments} tuple is either:
     * <ul>
     *   <li>"valid" – the path is accepted and stored unchanged,</li>
     *   <li>"null"  – a {@link NullPointerException} is expected.</li>
     * </ul>
     */
    private static Stream<Arguments> providePathCases() {
        return Stream.of(// Basic valid cases
        Arguments.of("my.config.path", "valid"), Arguments.of("", "valid"), Arguments.of("   ", "valid"), Arguments.of("!@#$%^&*()_+-=[]{}|;:',.<>/?", "valid"), Arguments.of("my.config[0].value", "valid"), Arguments.of("パス.設定.テスト", "valid"), // Very long path (10 000 characters)
        Arguments.of("a".repeat(10_000), "valid"), // Path with consecutive dots
        Arguments.of("my..config..path", "valid"), // Null path – should throw NPE
        Arguments.of(null, "null"));
    }

    @Test
    void constructObject_returnsNonNullObjectCreator() {
        String path = "my.custom.path";
        ConfigMappingContext.ObjectCreator<?> creator = context.constructObject(path);
        assertNotNull(creator, "constructObject should never return null");
        assertEquals(ConfigMappingContext.ObjectCreator.class, creator.getClass(), "Returned instance should be of type ObjectCreator");
    }

    @Test
    void constructObject_returnsNewInstanceOnEachCall() {
        String path = "another.path";
        ConfigMappingContext.ObjectCreator<?> first = context.constructObject(path);
        ConfigMappingContext.ObjectCreator<?> second = context.constructObject(path);
        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second, "Each call to constructObject should produce a distinct ObjectCreator");
    }

    /**
     * Parameterised test covering a wide variety of valid and invalid path inputs.
     *
     * @param path          the path supplied to {@code constructObject}
     * @param expectation   a description of the expected outcome:
     *                      <ul>
     *                        <li>"valid" – the created ObjectCreator must contain the same path</li>
     *                        <li>"null"  – a {@link NullPointerException} is expected</li>
     *                      </ul>
     */
    @ParameterizedTest(name = "[{index}] path={0} → {1}")
    @MethodSource("providePathCases")
    void constructObject_variousInputs(String path, String expectation) throws Exception {
        if ("null".equals(expectation)) {
            assertThrows(NullPointerException.class, () -> context.constructObject(null), "Passing null should result in NullPointerException");
            return;
        }
        // Normal (valid) case – create the ObjectCreator and verify the stored path
        ConfigMappingContext.ObjectCreator<?> creator = context.constructObject(path);
        assertNotNull(creator, "ObjectCreator must not be null for path: " + path);
        // Use reflection to read the internal 'path' field of ObjectCreator
        Field pathField = creator.getClass().getDeclaredField("path");
        pathField.setAccessible(true);
        String internalPath = (String) pathField.get(creator);
        assertEquals(path, internalPath, () -> "The internal path should be exactly the supplied value. Expected: \"" + path + "\"");
    }

    @Test
    void constructObject_samePathProducesDistinctInstances() {
        String path = "duplicate.path";
        ConfigMappingContext.ObjectCreator<?> first = context.constructObject(path);
        ConfigMappingContext.ObjectCreator<?> second = context.constructObject(path);
        assertNotSame(first, second, "Two calls with the same path must return distinct ObjectCreator instances");
        // Verify that both instances store the same path value
        try {
            Field pathField = first.getClass().getDeclaredField("path");
            pathField.setAccessible(true);
            String firstPath = (String) pathField.get(first);
            String secondPath = (String) pathField.get(second);
            assertEquals(path, firstPath);
            assertEquals(path, secondPath);
        } catch (ReflectiveOperationException e) {
            fail("Reflection failed while checking internal path fields", e);
        }
    }
}