package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Arrays;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigMapping;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_getConfigMapping_Test {

    interface Environment {

        String host();

        int port();
    }

    interface Log {

        boolean enabled();
    }

    interface Ssl {

        Optional<String> keystore();

        Optional<String> keystorePassword();
    }

    interface App {

        String name();

        String version();
    }

    interface Environment {

        String host();

        int port();
    }

    interface Log {

        boolean enabled();
    }

    interface Ssl {

        Optional<String> keystore();

        Optional<String> keystorePassword();
    }

    interface App {

        String name();

        String version();
    }

    @ConfigProperties
    interface Inner {

        String inner();
    }

    @ConfigProperties
    interface Inner {

        String inner();
    }

    @ConfigProperties
    interface Inner {

        String inner();
    }

    interface Environment {

        String host();

        int port();
    }

    interface Log {

        boolean enabled();
    }

    interface Ssl {

        Optional<String> keystore();

        Optional<String> keystorePassword();
    }

    interface App {

        String name();

        String version();
    }

    interface Environment {

        String host();

        int port();
    }

    interface Log {

        boolean enabled();
    }

    interface Ssl {

        Optional<String> keystore();

        Optional<String> keystorePassword();
    }

    interface App {

        String name();

        String version();
    }

    @ConfigProperties
    interface Inner {

        String inner();
    }

    @ConfigProperties
    interface Inner {

        String inner();
    }

    /**
     * Simple POJO – not a config mapping.
     */
    public static class PlainPojo {

        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }
    }

    /**
     * A set of reusable mapping interfaces used in several tests.
     */
    @ConfigProperties
    interface Inner {

        String inner();
    }

    @ConfigMapping(prefix = "server")
    public interface Server {

        String host();

        int port();
    }

    @ConfigMapping(prefix = "server")
    public interface ServerNested {

        Map<String, Environment> environments();

        Log log();

        Optional<Ssl> ssl();

        List<App> apps();

        interface Environment {

            String host();

            int port();
        }

        interface Log {

            boolean enabled();
        }

        interface Ssl {

            Optional<String> keystore();

            Optional<String> keystorePassword();
        }

        interface App {

            String name();

            String version();
        }
    }

    interface Environment {

        String host();

        int port();
    }

    interface Log {

        boolean enabled();
    }

    interface Ssl {

        Optional<String> keystore();

        Optional<String> keystorePassword();
    }

    interface App {

        String name();

        String version();
    }

    interface Environment {

        String host();

        int port();
    }

    interface Log {

        boolean enabled();
    }

    interface Ssl {

        Optional<String> keystore();

        Optional<String> keystorePassword();
    }

    interface App {

        String name();

        String version();
    }

    @ConfigProperties
    interface ValidConfigMapping {

        String name();
    }

    @ConfigProperties
    interface ParentConfigMapping {

        String parentProp();
    }

    @ConfigProperties
    interface InheritedConfigMapping extends ParentConfigMapping {

        String childProp();
    }

    @ConfigProperties
    interface DefaultMethodConfigMapping {

        default String defaultValue() {
            return "default";
        }

        String required();
    }

    @ConfigProperties
    interface EmptyConfigMapping {
        // deliberately empty
    }

    @ConfigProperties
    interface PrimitiveReturnConfigMapping {

        int intValue();

        boolean boolValue();

        long longValue();
    }

    @ConfigProperties
    interface OptionalReturnConfigMapping {

        Optional<String> optional();
    }

    @ConfigProperties
    interface CollectionReturnConfigMapping {

        List<String> list();
    }

    @ConfigProperties(prefix = "my.prefix")
    interface PrefixConfigMapping {

        String something();
    }

    @ConfigProperties
    interface ChildInheritsConfigMapping extends ParentConfigMapping {

        String child();
    }

    @ConfigProperties
    interface StaticMethodConfigMapping {

        static String staticMethod() {
            return "static";
        }

        String instance();
    }

    @ConfigProperties
    public interface NestedInterfaceConfigMapping {

        String top();

        @ConfigProperties
        interface Inner {

            String inner();
        }
    }

    @ConfigProperties
    interface Inner {

        String inner();
    }

    @ConfigProperties
    interface Inner {

        String inner();
    }

    // No @ConfigProperties annotation
    interface NotAnnotatedInterface {

        String foo();
    }

    // A class (not an interface) annotated with @ConfigProperties
    @ConfigProperties
    static class NotAnInterface {

        public String foo() {
            return "foo";
        }
    }

    // Generic interface – not supported
    @ConfigProperties
    interface GenericConfigMapping<T> {

        T value();
    }

    // Method with parameters – not a valid accessor
    @ConfigProperties
    interface UnsupportedMethodSignature {

        String foo(String bar);
    }

    // Method declares a checked exception
    @ConfigProperties
    interface InterfaceWithThrowsClause {

        String foo() throws java.io.IOException;
    }

    // Two accessor methods with the same name but different return types
    @ConfigProperties
    interface OverloadedMethodConfigMapping {

        String foo();

        // overload – different parameter list to keep the class compilable
        int foo(int ignored);
    }

    // Method returns an array type
    @ConfigProperties
    interface ArrayReturnConfigMapping {

        String[] values();
    }

    // Interface annotated but without a generated implementation class
    @ConfigProperties
    interface MissingImplementationConfigMapping {

        String missing();
    }

    // Unsupported return type: Map
    @ConfigProperties
    interface MapReturnConfigMapping {

        java.util.Map<String, String> map();
    }

    // Generic collection return type (List<T>) – unsupported
    @ConfigProperties
    interface GenericCollectionReturnConfigMapping<T> {

        java.util.List<T> list();
    }

    // Void return type – invalid for configuration mapping
    @ConfigProperties
    interface VoidReturnConfigMapping {

        void nothing();
    }

    // Package‑private (non‑public) interface – loader requires a public type
    @ConfigProperties
    interface PackagePrivateConfigMapping {

        String foo();
    }

    /**
     * Simulated implementation class whose static initializer throws.
     * The naming convention used by {@link ConfigMappingLoader} is not public,
     * but the presence of this class forces the loader to attempt loading it,
     * which will trigger the static initializer exception.
     */
    public static class FailingImplementationConfigMappingImpl {

        static {
            // Force a failure during class initialisation
            if (true) {
                throw new RuntimeException("simulated static init failure");
            }
        }
    }

    @ConfigProperties
    interface FailingImplementationConfigMapping {

        String foo();
    }

    /**
     * Simulated implementation class that is not publicly accessible.
     */
    static class IllegalAccessImplementationConfigMappingImpl {
        // empty – the loader will hit an IllegalAccessException when trying to reflectively
        // obtain the class (because it is package‑private).
    }

    @ConfigProperties
    interface IllegalAccessImplementationConfigMapping {

        String foo();
    }

    static Stream<Class<?>> successfulMappings() {
        return Stream.of(ValidConfigMapping.class, InheritedConfigMapping.class, DefaultMethodConfigMapping.class, EmptyConfigMapping.class, PrimitiveReturnConfigMapping.class, OptionalReturnConfigMapping.class, CollectionReturnConfigMapping.class, PrefixConfigMapping.class, ChildInheritsConfigMapping.class, StaticMethodConfigMapping.class, NestedInterfaceConfigMapping.Inner.class);
    }

    static Stream<Arguments> illegalArgumentMappings() {
        return Stream.of(// missing @ConfigProperties
        Arguments.of(NotAnnotatedInterface.class, "must be annotated with @ConfigProperties"), // not an interface
        Arguments.of(NotAnInterface.class, "must be an interface"), // generic interface
        Arguments.of(GenericConfigMapping.class, "must not be generic"), // method with parameters
        Arguments.of(UnsupportedMethodSignature.class, "method signatures not supported"), // method with throws clause
        Arguments.of(InterfaceWithThrowsClause.class, "throws clause not supported"), // overloaded accessor methods
        Arguments.of(OverloadedMethodConfigMapping.class, "overloaded method signatures not supported"), // array return type
        Arguments.of(ArrayReturnConfigMapping.class, "array types not supported"), // missing implementation class
        Arguments.of(MissingImplementationConfigMapping.class, "implementation class not found"), // unsupported return type: Map
        Arguments.of(MapReturnConfigMapping.class, "return type not supported"), // generic collection return type
        Arguments.of(GenericCollectionReturnConfigMapping.class, "generic return types not supported"), // void return type
        Arguments.of(VoidReturnConfigMapping.class, "void return type not supported"), // non‑public interface
        Arguments.of(PackagePrivateConfigMapping.class, "must be public"));
    }

    @Test
    void simpleConfigMapping() {
        // The focal method should return a non‑null ConfigMappingInterface for a
        // correctly annotated interface.
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(Server.class);
        assertNotNull(mapping, "ConfigMappingInterface must not be null for Server");
        // The returned interface should expose the original type.
        assertEquals(Server.class, mapping.getInterfaceType(), "Returned ConfigMappingInterface must wrap the original interface type");
        // The mapping must contain exactly the two declared properties.
        List<String> propertyNames = Arrays.stream(mapping.getProperties()).map(p -> p.getName()).collect(Collectors.toList());
        assertTrue(propertyNames.contains("host"));
        assertTrue(propertyNames.contains("port"));
        assertEquals(2, propertyNames.size(), "Server mapping should contain exactly two properties");
    }

    @Test
    void nestedConfigMappingDiscovery() {
        // ServerNested contains a mixture of map, list, optional and plain nested
        // interfaces. The loader should discover all of them.
        ConfigMappingInterface root = ConfigMappingLoader.getConfigMapping(ServerNested.class);
        assertNotNull(root, "Root ConfigMappingInterface must not be null");
        // Collect all nested mapping interfaces (directly nested + those coming from
        // super‑types – ServerNested does not extend another mapping, but the method is
        // exercised anyway).
        List<ConfigMappingInterface> nested = root.getNested();
        // Expected nested types
        Class<?>[] expected = { ServerNested.Environment.class, ServerNested.Log.class, ServerNested.Ssl.class, ServerNested.App.class };
        // Verify presence and count
        List<Class<?>> found = nested.stream().map(ConfigMappingInterface::getInterfaceType).collect(Collectors.toList());
        for (Class<?> exp : expected) {
            assertTrue(found.contains(exp), "Nested mapping should contain " + exp.getSimpleName());
        }
        assertEquals(expected.length, nested.size(), "Exactly " + expected.length + " nested mappings must be discovered");
    }

    @Test
    void unsupportedTypeReturnsNull() {
        // A plain class that is *not* a @ConfigMapping interface should yield null.
        ConfigMappingInterface result = ConfigMappingLoader.getConfigMapping(PlainPojo.class);
        assertNull(result, "ConfigMappingLoader.getConfigMapping should return null for a type that is not a config mapping");
    }

    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("successfulMappings")
    void successfulMapping(Class<?> type) {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(type);
        assertNotNull(mapping, () -> "Mapping must not be null for " + type.getSimpleName());
        // Basic sanity check – the wrapper must expose the original interface type.
        assertEquals(type, mapping.getInterfaceType(), () -> "Wrapper should contain the original type for " + type.getSimpleName());
    }

    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("illegalArgumentMappings")
    void illegalArgumentMapping(Class<?> type, String expectedMessageFragment) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(type));
        assertTrue(ex.getMessage().contains(expectedMessageFragment), () -> "Expected message to contain '" + expectedMessageFragment + "' but was: " + ex.getMessage());
    }

    @Test
    void nullArgument() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMapping(null));
    }

    @Test
    void failingImplementationLoading() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ConfigMappingLoader.getConfigMapping(FailingImplementationConfigMapping.class));
        assertTrue(ex.getMessage().contains("failed to load implementation class"));
    }

    @Test
    void illegalAccessImplementationLoading() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ConfigMappingLoader.getConfigMapping(IllegalAccessImplementationConfigMapping.class));
        assertTrue(ex.getMessage().contains("cannot access implementation class"));
    }
}
