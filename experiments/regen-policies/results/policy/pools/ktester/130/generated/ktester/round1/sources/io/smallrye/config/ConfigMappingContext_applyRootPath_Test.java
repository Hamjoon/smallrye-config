package io.smallrye.config;

import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.BeforeEach;

class ConfigMappingContext_applyRootPath_Test {

    //     @Mock
    SmallRyeConfig // mocked dependency – its behaviour is irrelevant for applyRootPath
    mockConfig;

    ConfigMappingContext context;

    // will be set to {@code null}
    SmallRyeConfig config;

    //     @BeforeEach
    void setupBeforeEach() {
        // An empty roots map is sufficient – the constructor will not invoke applyRootPath itself
        Map<Class<?>, java.util.Set<String>> emptyRoots = Collections.emptyMap();
        config = null;
        context = new ConfigMappingContext(config, emptyRoots);
    }

    /**
     * Supplies test data for {@link #applyRootPathVarious(String[], String, String)}.
     *
     * @return a stream of arguments covering normal, edge‑case and large inputs
     */
    static Stream<Arguments> applyRootPathProvider() {
        String longPath = "a.really.long.path.that.goes.on.and.on.and.might.exceed.typical.lengths";
        // generate a very long string (10,000 'x' characters) for the "very long path" case
        String veryLongPath = "x".repeat(10_000);
        return Stream.of(// empty string
        Arguments.of(new String[] { "" }, "", ""), // normal dot‑separated path
        Arguments.of(new String[] { "my.root" }, "my.root", "my.root"), // leading and trailing whitespace preserved
        Arguments.of(new String[] { "  leading trailing  " }, "  leading trailing  ", "  leading trailing  "), // long path (still well below the 10k limit)
        Arguments.of(new String[] { longPath }, longPath, longPath), // two calls – second replaces first
        Arguments.of(new String[] { "first.path", "second.path" }, "second.path", "second.path"), // newline characters preserved
        Arguments.of(new String[] { "path\nwith\nnewlines" }, "path\nwith\nnewlines", "path\nwith\nnewlines"), // Unicode emoji at start
        Arguments.of(new String[] { "😀emoji" }, "😀emoji", "😀emoji"), // wide range of punctuation / special characters
        Arguments.of(new String[] { "path.with.special!@#$%^&*()_+-=[]{}|;':\",.<>/?" }, "path.with.special!@#$%^&*()_+-=[]{}|;':\",.<>/?", "path.with.special!@#$%^&*()_+-=[]{}|;':\",.<>/?"), // three spaces only
        Arguments.of(new String[] { "   " }, "   ", "   "), // two tab characters only
        Arguments.of(new String[] { "\t\t" }, "\t\t", "\t\t"), // very long path (10,000 characters) – should be stored unchanged
        Arguments.of(new String[] { veryLongPath }, veryLongPath, veryLongPath), // non‑empty followed by empty – final state must be empty
        Arguments.of(new String[] { "first.path", "" }, "", ""), // bracketed index notation
        Arguments.of(new String[] { "path.with.index[0]" }, "path.with.index[0]", "path.with.index[0]"), // identical consecutive calls – state stays the same
        Arguments.of(new String[] { "same.path", "same.path" }, "same.path", "same.path"));
    }

    /**
     * Parameterized test that covers a wide variety of valid inputs.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>an array of strings to be passed sequentially to {@code applyRootPath}</li>
     *   <li>the expected final value of the private {@code rootPath} field</li>
     *   <li>the expected final content of the {@link StringBuilder} returned by {@code getNameBuilder()}</li>
     * </ul>
     * </p>
     */
    @ParameterizedTest(name = "{index}: applyRootPath sequence {0} → expected rootPath=''{1}''")
    @MethodSource("applyRootPathProvider")
    void applyRootPathVarious(String[] inputs, String expectedRootPath, String expectedBuilder) throws Exception {
        // Apply each input in order
        for (String input : inputs) {
            context.applyRootPath(input);
        }
        // Verify the public StringBuilder state
        assertEquals(expectedBuilder, context.getNameBuilder().toString(), "StringBuilder content does not match expected value");
        // Verify the private rootPath field via reflection
        Field rootPathField = ConfigMappingContext.class.getDeclaredField("rootPath");
        rootPathField.setAccessible(true);
        String actualRootPath = (String) rootPathField.get(context);
        assertEquals(expectedRootPath, actualRootPath, "Private rootPath field does not match expected value");
    }
}
