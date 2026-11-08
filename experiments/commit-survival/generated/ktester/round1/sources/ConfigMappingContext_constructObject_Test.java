package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_constructObject_Test {

    // ----------------------------------------------------------------------
    @Mock
    SmallRyeConfig // mocked dependency for the context constructor
    mockConfig;

    // instance under test
    ConfigMappingContext context;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor needs three arguments:
        //   1. SmallRyeConfig – we provide a Mockito mock.
        //   2. roots – an empty map (no root classes are needed for this test).
        //   3. names – an empty map (no naming information is needed for this test).
        Map<String, java.util.List<Class<?>>> roots = Collections.emptyMap();
        Map<String, Map<String, java.util.Set<String>>> names = Collections.emptyMap();
        context = new ConfigMappingContext(mockConfig, roots, names);
    }

    // ----------------------------------------------------------------------
    private static String getInternalPath(Object creator) {
        try {
            Field pathField = creator.getClass().getDeclaredField("path");
            pathField.setAccessible(true);
            return (String) pathField.get(creator);
        } catch (NoSuchFieldException e) {
            // Some implementations may store the path under a different name.
            // Try a more generic approach: look for any String field that matches the input.
            for (Field f : creator.getClass().getDeclaredFields()) {
                if (f.getType() == String.class) {
                    f.setAccessible(true);
                    try {
                        return (String) f.get(creator);
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
            fail("Unable to locate internal path field in ObjectCreator");
            // unreachable
            return null;
        } catch (IllegalAccessException e) {
            fail("Failed to access internal path field", e);
            // unreachable
            return null;
        }
    }

    private static Stream<Arguments> provideValidPaths() {
        // 10,000 characters
        String longString = "a".repeat(10_000);
        String newlineString = "my.path\nnewLine";
        String nullCharString = "my.path\u0000nullChar";
        String specialChars = "my.path-with-special!@#$%^&*()_+";
        String unicodeChinese = "路径.含有.中文";
        String hyphenUnderscore = "path-with-dash_and_underscore";
        String whitespace = "   ";
        return Stream.of(Arguments.of("my.path", "my.path", "normal non‑empty path"), Arguments.of("", "", "empty string path"), Arguments.of(whitespace, whitespace, "whitespace‑only path"), Arguments.of(unicodeChinese, unicodeChinese, "Unicode Chinese characters"), Arguments.of(hyphenUnderscore, hyphenUnderscore, "hyphens and underscores"), Arguments.of(longString, longString, "very long path (10,000 chars)"), Arguments.of(newlineString, newlineString, "path containing newline character"), Arguments.of(nullCharString, nullCharString, "path containing null character"), Arguments.of(specialChars, specialChars, "path with various special characters"));
    }

    // ----------------------------------------------------------------------
    @Test
    void constructObject_returnsNonNullObjectCreator() {
        // GIVEN
        String path = "my.test.path";
        // WHEN
        ObjectCreator<?> creator = context.constructObject(path);
        // THEN
        assertNotNull(creator, "constructObject should never return null");
        // The concrete type is an inner class of ConfigMappingContext named ObjectCreator.
        // We verify that the runtime class name ends with "$ObjectCreator".
        assertTrue(creator.getClass().getSimpleName().endsWith("ObjectCreator"), "Returned instance should be of type ObjectCreator");
    }

    @Test
    void constructObject_allowsNullPath() {
        // GIVEN
        String path = null;
        // WHEN
        ObjectCreator<?> creator = context.constructObject(path);
        // THEN
        assertNotNull(creator, "ObjectCreator should be created even when the path is null");
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideValidPaths")
    void constructObject_withVariousPaths_returnsCreatorWithCorrectPath(String inputPath, String expectedPath, String description) {
        // WHEN
        Object creator = context.constructObject(inputPath);
        // THEN
        assertNotNull(creator, "ObjectCreator should never be null for valid input");
        String actualPath = getInternalPath(creator);
        assertEquals(expectedPath, actualPath, () -> "Stored path should match expected for case: " + description);
    }

    // ----------------------------------------------------------------------
    @Test
    void constructObject_nullPath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> context.constructObject(null), "constructObject should throw NPE when path is null");
    }

    // ----------------------------------------------------------------------
    @Test
    void constructObject_multipleCalls_returnDistinctInstances() {
        // GIVEN
        String path = "my.path";
        // WHEN
        Object creator1 = context.constructObject(path);
        Object creator2 = context.constructObject(path);
        // THEN
        assertNotNull(creator1, "First ObjectCreator must not be null");
        assertNotNull(creator2, "Second ObjectCreator must not be null");
        assertNotSame(creator1, creator2, "Each call should return a new ObjectCreator instance");
        String internalPath1 = getInternalPath(creator1);
        String internalPath2 = getInternalPath(creator2);
        assertEquals(path, internalPath1, "First creator should store the correct path");
        assertEquals(path, internalPath2, "Second creator should store the correct path");
    }
}
