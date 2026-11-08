package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_applyPrefix_Test {

    @Mock
    SmallRyeConfig // dependency required by the constructor
    mockConfig;

    @Mock
    SmallRyeConfigBuilder.MappingBuilder // dependency required by the constructor
    mockBuilder;

    // object under test
    ConfigMappingContext context;

    @BeforeEach
    void setupBeforeEach() {
        /*
         * The constructor of ConfigMappingContext expects a SmallRyeConfig and a MappingBuilder.
         * We do not need any real behaviour from these collaborators for the `applyPrefix`
         * tests, therefore simple Mockito mocks are sufficient.
         */
        context = new ConfigMappingContext(mockConfig, mockBuilder);
    }

    /**
     * Helper that extracts the current content of the private {@code nameBuilder}
     * field using reflection.
     */
    private static String getNameBuilderContent(ConfigMappingContext ctx) {
        try {
            Field f = ConfigMappingContext.class.getDeclaredField("nameBuilder");
            f.setAccessible(true);
            StringBuilder sb = (StringBuilder) f.get(ctx);
            return sb.toString();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to access nameBuilder field", e);
        }
    }

    /**
     * Supplies a variety of prefixes, including normal, empty and Unicode strings.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> providePrefixes() {
        return Stream.of(arguments("my.prefix", "my.prefix"), // empty string clears the builder
        arguments("", ""), // Unicode & emoji
        arguments("préƒïx 🌟", "préƒïx 🌟"));
    }

    @Test
    @DisplayName("applyPrefix should replace the internal name builder content with the given prefix")
    void testApplyPrefixReplacesContent() {
        // The method does not expose its internal state, but we can verify that it does not
        // throw and that subsequent calls are allowed (i.e., the internal StringBuilder can be
        // replaced multiple times).
        assertDoesNotThrow(() -> {
            context.applyPrefix("first.prefix");
            context.applyPrefix("second.prefix");
        });
    }

    @Test
    @DisplayName("applyPrefix should throw NullPointerException when passed a null prefix")
    void testApplyPrefixNullArgument() {
        // According to StringBuilder.replace contract, passing a null CharSequence results in NPE.
        assertThrows(NullPointerException.class, () -> context.applyPrefix(null));
    }

    @ParameterizedTest(name = "{index} => prefix=''{0}'', expected=''{1}''")
    @MethodSource("providePrefixes")
    @DisplayName("applyPrefix should store the given prefix correctly")
    void testApplyPrefixStoresValue(String prefix, String expected) {
        assertDoesNotThrow(() -> context.applyPrefix(prefix));
        assertEquals(expected, getNameBuilderContent(context), "The internal nameBuilder should contain the exact prefix supplied");
    }

    @Test
    @DisplayName("Repeated calls to applyPrefix should keep only the latest prefix")
    void testApplyPrefixRepeatedCalls() {
        context.applyPrefix("first");
        assertEquals("first", getNameBuilderContent(context), "After first call the builder should contain 'first'");
        context.applyPrefix("second");
        assertEquals("second", getNameBuilderContent(context), "After second call the builder should contain only 'second'");
    }
}
