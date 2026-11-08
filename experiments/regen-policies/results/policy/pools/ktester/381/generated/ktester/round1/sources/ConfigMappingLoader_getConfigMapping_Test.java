package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// import org.mockito.MockedStatic;
// import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Tests for {@link ConfigMappingLoader#getConfigMapping(Class)}.
 */
class ConfigMappingLoader_getConfigMapping_Test {

    class LocalNonConfigClass {
    }

    abstract class LocalAbstractConfigMapping {
    }

    interface LocalSimpleInterface {
    }

    /* --------------------------------------------------------------------- */
    interface Environment {

        String host();

        int port();
    }

    interface Log {

        String level();
    }

    interface Ssl {

        boolean enabled();
    }

    interface App {

        String name();
    }

    interface ParentNested {

        String nestedProp();
    }

    interface ChildNested {

        String nestedProp();
    }

    // package‑private (no explicit public modifier)
    static class PackagePrivateConfigMapping {

        public String foo() {
            return "bar";
        }
    }

    // Class without a zero‑arg constructor
    public static class MappingWithoutDefaultConstructor {

        private MappingWithoutDefaultConstructor(String arg) {
        }
    }

    // Class whose static initializer throws
    public static class MappingWithStaticInitializerError {

        static {
            if (true) {
                throw new RuntimeException("boom");
            }
        }
    }

    // Placeholder for missing implementation scenario
    public static class MappingWithMissingImplementation {
    }

    // Placeholder for cyclic metadata scenario
    public static class CyclicConfigMapping {
    }

    // Placeholder for security‑restricted scenario
    public static class SecureConfigMapping {
    }

    interface SimpleConfig {

        String name();

        int size();
    }

    interface Server {

        String host();

        int port();

        Map<String, Environment> environments();

        Log log();

        Optional<Ssl> ssl();

        List<App> apps();

        interface Environment {

            String host();

            int port();
        }

        interface Log {

            String level();
        }

        interface Ssl {

            boolean enabled();
        }

        interface App {

            String name();
        }
    }

    interface Environment {

        String host();

        int port();
    }

    interface Log {

        String level();
    }

    interface Ssl {

        boolean enabled();
    }

    interface App {

        String name();
    }

    interface Environment {

        String host();

        int port();
    }

    interface Log {

        String level();
    }

    interface Ssl {

        boolean enabled();
    }

    interface App {

        String name();
    }

    interface Parent {

        String parentProp();

        interface ParentNested {

            String nestedProp();
        }
    }

    interface ParentNested {

        String nestedProp();
    }

    interface ParentNested {

        String nestedProp();
    }

    interface Child extends Parent {

        String childProp();

        interface ChildNested {

            String nestedProp();
        }
    }

    interface ChildNested {

        String nestedProp();
    }

    interface ChildNested {

        String nestedProp();
    }

    class NonConfigClass {
    }

    abstract class AbstractConfigMapping {
    }

    interface SimpleInterface {
    }

    public static class ValidConfigMapping {

        public String name() {
            return "valid";
        }
    }

    public interface ValidConfigMappingInterface {

        String name();
    }

    public interface ConfigMappingWithDefaults {

        default String greeting() {
            return "hello";
        }

        int size();
    }

    public interface ParentMapping {

        String parentProp();
    }

    public interface InheritedConfigMapping extends ParentMapping {

        String childProp();
    }

    class LocalNonConfigClass {
    }

    abstract class LocalAbstractConfigMapping {
    }

    interface LocalSimpleInterface {
    }

    /* --------------------------------------------------------------------- */
    static Stream<Arguments> validConfigMappingProvider() {
        return Stream.of(Arguments.of(ValidConfigMapping.class), Arguments.of(ValidConfigMappingInterface.class), Arguments.of(ConfigMappingWithDefaults.class), Arguments.of(InheritedConfigMapping.class));
    }

    @Test
    void nestedConfigMapping() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(Server.class);
        List<ConfigMappingInterface> nested = mapping.getNested();
        assertNotNull(mapping, "Mapping for Server must not be null");
        assertEquals(Server.class, mapping.getInterfaceType(), "Mapping should reference the Server interface");
        assertEquals(4, nested.size(), "Server should have exactly four nested mappings");
        assertTrue(nested.stream().anyMatch(m -> m.getInterfaceType() == Server.Environment.class), "Nested Environment should be discovered");
        assertTrue(nested.stream().anyMatch(m -> m.getInterfaceType() == Server.Log.class), "Nested Log should be discovered");
        assertTrue(nested.stream().anyMatch(m -> m.getInterfaceType() == Server.Ssl.class), "Nested Ssl should be discovered");
        assertTrue(nested.stream().anyMatch(m -> m.getInterfaceType() == Server.App.class), "Nested App should be discovered");
    }

    @Test
    void parentInterfaceMapping() {
        ConfigMappingInterface childMapping = ConfigMappingLoader.getConfigMapping(Child.class);
        ConfigMappingInterface[] superTypesArray = childMapping.getSuperTypes();
        assertNotNull(childMapping, "Mapping for Child must not be null");
        assertEquals(Child.class, childMapping.getInterfaceType(), "Mapping should reference the Child interface");
        // Child should list Parent as a super type
        boolean parentFound = false;
        for (ConfigMappingInterface cm : superTypesArray) {
            if (cm.getInterfaceType() == Parent.class) {
                parentFound = true;
                List<ConfigMappingInterface> parentNested = cm.getNested();
                assertTrue(parentNested.stream().anyMatch(m -> m.getInterfaceType() == Parent.ParentNested.class), "Parent's nested type must be present");
                break;
            }
        }
        assertTrue(parentFound, "Parent should appear in Child's super types");
        // Verify Child's own nested type is present
        List<ConfigMappingInterface> childNested = childMapping.getNested();
        assertTrue(childNested.stream().anyMatch(m -> m.getInterfaceType() == Child.ChildNested.class), "Child's own nested type must be present");
    }

    @Test
    void nonConfigMappingReturnsNull() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(String.class);
        assertNull(mapping, "Non‑config‑mapping types should result in a null mapping");
    }

    /**
     * All valid configuration‑mapping types should return a non‑null
     * {@link ConfigMappingInterface}. The concrete class and the interface
     * variants are exercised.
     */
    @ParameterizedTest(name = "{index} => type={0}")
    @MethodSource("validConfigMappingProvider")
    void validConfigMappingReturnsInterface(Class<?> type) {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(type);
        assertNotNull(mapping, "Mapping must not be null for a valid config mapping type");
        assertTrue(mapping instanceof ConfigMappingInterface, "Returned object should be an instance of ConfigMappingInterface");
    }

    @Test
    void getConfigMapping_nullArgument_throwsNPE() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMapping(null), "Passing null should cause NullPointerException");
    }

    @Test
    void getConfigMapping_nonConfigClass_throwsIllegalArgumentException() {
        class LocalNonConfigClass {
        }
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(LocalNonConfigClass.class), "A class without @ConfigProperties must cause IllegalArgumentException");
    }

    @Test
    void getConfigMapping_abstractConfigClass_throwsIllegalArgumentException() {
        abstract class LocalAbstractConfigMapping {
        }
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(LocalAbstractConfigMapping.class), "Abstract classes cannot be used as configuration mappings");
    }

    @Test
    void getConfigMapping_primitiveType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(int.class), "Primitive types are not valid configuration mapping sources");
    }

    @Test
    void getConfigMapping_unannotatedInterface_throwsIllegalArgumentException() {
        interface LocalSimpleInterface {
        }
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(LocalSimpleInterface.class), "Interface without @ConfigProperties must cause IllegalArgumentException");
    }
}
