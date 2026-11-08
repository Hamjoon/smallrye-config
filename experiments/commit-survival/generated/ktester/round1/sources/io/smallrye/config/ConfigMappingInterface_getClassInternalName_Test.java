package io.smallrye.config;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.Field;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getClassInternalName_Test {

    interface TestConfig {
        // No methods – the interface is only a marker for the test.
    }

    static interface NestedConfig {
        // No methods – used to test a static nested interface.
    }

    /* --------------------------------------------------------------------- */
    private ConfigMappingInterface createWithClassName(String className) throws Exception {
        // Use the package‑private constructor (accessible because test class is in the same package)
        ConfigMappingInterface mapping = new ConfigMappingInterface(// dummy interface type – not used for the test
        Object.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // Inject the desired className via reflection
        Field field = ConfigMappingInterface.class.getDeclaredField("className");
        field.setAccessible(true);
        field.set(mapping, className);
        return mapping;
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> normalAndEdgeCases() {
        return Stream.of(// typical fully‑qualified name
        Arguments.of("com.example.Config", "com/example/Config"), // no dot
        Arguments.of("Config", "Config"), // empty string
        Arguments.of("", ""), // leading dot
        Arguments.of(".leading.dot", "/leading/dot"), // trailing dot
        Arguments.of("trailing.dot.", "trailing/dot/"), // consecutive dots
        Arguments.of("multiple..dots", "multiple//dots"));
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> specialCharacterCases() {
        return Stream.of(// dollar sign (inner class separator)
        Arguments.of("com.example$Inner", "com/example$Inner"), // consecutive dots with empty segments
        Arguments.of("com..example..Config", "com//example//Config"), // space adjacent to dot
        Arguments.of("com.example .Config", "com/example .Config"), // Unicode emoji
        Arguments.of("com.example.😀", "com/example/😀"), // escaped null character (literal backslash + u0000)
        Arguments.of("com.example.\\u0000", "com/example/\\u0000"));
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> idempotenceCases() {
        return Stream.of(// already contains '/' and no dots
        Arguments.of("already/slash", "already/slash"), // typical name – calling twice should give same result
        Arguments.of("com.example.Config", "com/example/Config"));
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> nullClassNameCases() {
        return Stream.of(Arguments.of(null, NullPointerException.class));
    }

    /**
     * Verify that the internal name is correctly derived for a top‑level interface.
     */
    @Test
    void getClassInternalName_topLevelInterface() {
        // GIVEN
        ConfigMappingInterface mapping = new ConfigMappingInterface(TestConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // The constructor builds the class name as:
        //   interfaceType.getName() + interfaceType.getName().hashCode() + "Impl"
        String expectedClassName = TestConfig.class.getName() + TestConfig.class.getName().hashCode() + "Impl";
        // WHEN
        String internalName = mapping.getClassInternalName();
        // THEN
        assertEquals(expectedClassName.replace('.', '/'), internalName, "The internal name should replace all '.' with '/'");
    }

    /**
     * Verify that the method works for a nested (static) interface.
     */
    @Test
    void getClassInternalName_nestedInterface() {
        // GIVEN
        ConfigMappingInterface mapping = new ConfigMappingInterface(NestedConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        String expectedClassName = NestedConfig.class.getName() + NestedConfig.class.getName().hashCode() + "Impl";
        // WHEN
        String internalName = mapping.getClassInternalName();
        // THEN
        assertEquals(expectedClassName.replace('.', '/'), internalName, "Nested interface internal name should also replace '.' with '/'");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] className=''{0}'' → expected=''{1}''")
    @MethodSource("normalAndEdgeCases")
    void getClassInternalName_normalAndEdgeCases(String className, String expected) throws Exception {
        // GIVEN
        ConfigMappingInterface mapping = createWithClassName(className);
        // WHEN
        String internalName = mapping.getClassInternalName();
        // THEN
        assertEquals(expected, internalName, () -> "Internal name should replace '.' with '/' for input: " + className);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] className=''{0}'' → expected=''{1}''")
    @MethodSource("specialCharacterCases")
    void getClassInternalName_specialCharacters(String className, String expected) throws Exception {
        // GIVEN
        ConfigMappingInterface mapping = createWithClassName(className);
        // WHEN
        String internalName = mapping.getClassInternalName();
        // THEN
        assertEquals(expected, internalName, () -> "Special‑character handling failed for: " + className);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] className=''{0}'' → expected=''{1}''")
    @MethodSource("idempotenceCases")
    void getClassInternalName_idempotence(String className, String expected) throws Exception {
        // GIVEN
        ConfigMappingInterface mapping = createWithClassName(className);
        // WHEN – invoke twice to ensure idempotence
        String first = mapping.getClassInternalName();
        String second = mapping.getClassInternalName();
        // THEN
        assertEquals(expected, first, "First invocation did not produce expected result.");
        assertEquals(expected, second, "Second invocation should be identical (idempotent).");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] className=null → throws {1}")
    @MethodSource("nullClassNameCases")
    void getClassInternalName_nullClassName(String className, Class<? extends Throwable> expectedException) throws Exception {
        // GIVEN
        ConfigMappingInterface mapping = createWithClassName(className);
        // WHEN / THEN
        assertThrows(expectedException, mapping::getClassInternalName, "Calling getClassInternalName() with a null className should throw NullPointerException.");
    }
}