package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
// import org.eclipse.microprofile.config.inject.ConfigMapping;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_getConfigMapping_Test {

    interface Environment {

        String host();

        int port();
    }

    interface Log {

        boolean enabled();
    }

    interface Ssl {

        boolean enabled();
    }

    interface App {

        String name();
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

    /* --------------------------------------------------------------------- */
//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

    /* --------------------------------------------------------------------- */
    @ConfigMapping(prefix = "server")
    public interface SimpleServer {

        String host();

        int port();
    }

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

            boolean enabled();
        }

        interface App {

            String name();
        }
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

//     interface Environment {

        String host();

        int port();
    }

//     interface Log {

        boolean enabled();
    }

//     interface Ssl {

        boolean enabled();
    }

//     interface App {

        String name();
    }

    public static class PlainPojo {

        public String foo;
    }

    @ConfigProperties(prefix = "server")
    public static class ServerProperties {

        String host;

        int port;
    }

    // 1. Valid config‑mapping interface (annotated with @ConfigProperties for the test)
    @ConfigProperties(prefix = "valid")
    public interface ValidConfigMapping {

        String name();
    }

    // 2. No annotation at all
    public interface NoAnnotationConfig {

        String foo();
    }

    // 3. Abstract class – not an interface
    @ConfigProperties(prefix = "abs")
    public abstract static class AbstractConfigMapping {

        public abstract String bar();
    }

    // name contains characters that the loader's class‑definer cannot handle.
    @ConfigProperties(prefix = "fail")
    public interface FailingClassLoaderConfig {

        String baz();
    }

    // 5. Interface that does NOT extend ConfigMappingObject (the loader expects it)
    @ConfigProperties(prefix = "non")
    public interface NonMappingInterface {

        String qux();
    }

    // 6. Concrete class annotated with @ConfigProperties (should be rejected)
    @ConfigProperties(prefix = "concrete")
    public static class ConcreteConfigMapping {

        public String hello() {
            return "hi";
        }
    }

    // 7. Interface with a proper prefix, extending ConfigMappingObject implicitly
    @ConfigProperties(prefix = "prefixed")
    public interface PrefixedConfigMapping {

        int size();
    }

    // 8. Parent‑child inheritance example
    @ConfigProperties(prefix = "parent")
    public interface ParentConfigMapping {

        String parentProp();
    }

    @ConfigProperties(prefix = "child")
    public interface ChildConfigMapping extends ParentConfigMapping {

        String childProp();
    }

    // 9. Generic interface
    @ConfigProperties(prefix = "generic")
    public interface GenericConfigMapping<T> {

        T value();
    }

    // 10. Interface with a default method
    @ConfigProperties(prefix = "default")
    public interface DefaultMethodConfigMapping {

        String mandatory();

        default String optional() {
            return "default";
        }
    }

    // intentional duplicate for the test – only a single annotation is allowed by Java
    @ConfigProperties(prefix = "dup")
    public interface DuplicateAnnotationConfigMapping {

        String dup();
    }

    // 12. Method with parameters – not allowed for config‑mapping methods
    @ConfigProperties(prefix = "params")
    public interface MethodWithParamsConfigMapping {

        String greet(String name);
    }

    // 13. Unsupported return type (java.util.Date)
    @ConfigProperties(prefix = "unsupported")
    public interface UnsupportedReturnTypeConfigMapping {

        java.util.Date date();
    }

    // 14. Void return type
    @ConfigProperties(prefix = "void")
    public interface VoidReturnConfigMapping {

        void nothing();
    }

    // 15. Child interface with a different prefix – conflict
    @ConfigProperties(prefix = "parentPrefix")
    public interface ParentPrefixConfig {

        String a();
    }

    @ConfigProperties(prefix = "childPrefix")
    public interface ChildWithDifferentPrefixConfigMapping extends ParentPrefixConfig {

        String b();
    }

    // 16. Invalid prefix (contains a space)
    @ConfigProperties(prefix = "invalid prefix")
    public interface InvalidPrefixConfigMapping {

        String x();
    }

    // 17. Another valid mapping used for distinct‑implementation test
    @ConfigProperties(prefix = "another")
    public interface AnotherValidConfigMapping {

        String y();
    }

    /**
     * Uses reflection to invoke the package‑private static method
     * {@code ConfigMappingLoader.getConfigMappingClass(Class)}.
     *
     * @param type the type to pass to the loader
     * @return the generated implementation class
     * @throws Throwable if the reflective call fails
     */
    private static Class<?> invokeGetConfigMappingClass(Class<?> type) throws Throwable {
        var lookup = MethodHandles.lookup();
        var mh = lookup.findStatic(ConfigMappingLoader.class, "getConfigMappingClass", MethodType.methodType(Class.class, Class.class));
        return (Class<?>) mh.invoke(type);
    }

//     @Test
//     void getConfigMapping_simpleInterface() {
//         ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(SimpleServer.class);
//         assertNotNull(mapping, "ConfigMappingInterface must not be null");
//         assertEquals(SimpleServer.class, mapping.getInterfaceType(), "Returned mapping should be for SimpleServer");
//         List<String> propertyNames = Arrays.stream(mapping.getProperties()).map(p -> p.name()).collect(Collectors.toList());
//         assertTrue(propertyNames.contains("host"), "Mapping should contain property 'host'");
//         assertTrue(propertyNames.contains("port"), "Mapping should contain property 'port'");
//         assertEquals(2, propertyNames.size(), "Exactly two properties are expected");
//     }

    @Test
    void getConfigMapping_nestedInterface() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(ServerNested.class);
        assertNotNull(mapping, "Mapping for ServerNested must not be null");
        List<Class<?>> nestedTypes = mapping.getNested().stream().map(ConfigMappingInterface::getInterfaceType).collect(Collectors.toList());
        assertEquals(4, nestedTypes.size(), "ServerNested should have exactly four nested mapping interfaces");
        assertTrue(nestedTypes.contains(ServerNested.Environment.class), "Nested Environment missing");
        assertTrue(nestedTypes.contains(ServerNested.Log.class), "Nested Log missing");
        assertTrue(nestedTypes.contains(ServerNested.Ssl.class), "Nested Ssl missing");
        assertTrue(nestedTypes.contains(ServerNested.App.class), "Nested App missing");
    }

    @Test
    void getConfigMapping_nonMappingClass_returnsNull() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(PlainPojo.class);
        assertNull(mapping, "Mapping for a non‑annotated class should be null");
    }

    @Test
    void getConfigMapping_configPropertiesClass_returnsNull() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(ServerProperties.class);
        assertNull(mapping, "ConfigProperties class must not produce a ConfigMappingInterface");
    }

    @Test
    void validConfigMapping_returnsInterface() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(ValidConfigMapping.class);
        assertNotNull(mapping, "Mapping should not be null for a valid config mapping");
        assertEquals(ValidConfigMapping.class, mapping.getInterfaceType(), "Mapping should refer to the original interface");
    }

    @Test
    void missingConfigPropertiesAnnotation_throwsException() {
        Exception ex = assertThrows(RuntimeException.class, () -> ConfigMappingLoader.getConfigMapping(NoAnnotationConfig.class));
        assertTrue(ex.getMessage().toLowerCase().contains("configproperties") || ex.getMessage().toLowerCase().contains("configmapping"), "Exception message should indicate missing configuration annotation");
    }

    @Test
    void abstractClass_notInterface_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(AbstractConfigMapping.class));
        assertTrue(ex.getMessage().toLowerCase().contains("must be an interface"), "Exception should mention that the type must be an interface");
    }

    @Test
    void classLoadingFailure_wrapsUndeclaredThrowableException() {
        Throwable thrown = assertThrows(UndeclaredThrowableException.class, () -> invokeGetConfigMappingClass(FailingClassLoaderConfig.class));
        assertTrue(thrown.getCause() instanceof ClassNotFoundException, "Cause should be ClassNotFoundException");
    }

    @Test
    void nullParameter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMapping(null));
    }

    @Test
    void interfaceWithoutConfigMappingObject_throwsException() {
        Exception ex = assertThrows(RuntimeException.class, () -> ConfigMappingLoader.getConfigMapping(NonMappingInterface.class));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid") || ex.getMessage().toLowerCase().contains("configmapping"), "Exception should indicate an invalid config‑mapping interface");
    }

    @Test
    void implementationClassCaching_returnsSameClass() throws Throwable {
        Class<?> first = invokeGetConfigMappingClass(ValidConfigMapping.class);
        Class<?> second = invokeGetConfigMappingClass(ValidConfigMapping.class);
        assertSame(first, second, "Implementation class should be cached and identical across calls");
    }

    @Test
    void concreteClassWithConfigProperties_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMapping(ConcreteConfigMapping.class));
        assertTrue(ex.getMessage().toLowerCase().contains("must be an interface"), "Exception must mention that the supplied type must be an interface");
    }

    @Test
    void prefixedConfigMapping_returnsInterface() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(PrefixedConfigMapping.class);
        assertNotNull(mapping);
        assertEquals(PrefixedConfigMapping.class, mapping.getInterfaceType());
    }

//     @Test
//     void inheritanceMapping_includesParentProperties() {
//         ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(ChildConfigMapping.class);
//         assertNotNull(mapping);
//         List<String> props = Arrays.stream(mapping.getProperties()).map(p -> p.name()).collect(Collectors.toList());
//         assertTrue(props.contains("parentProp"), "Parent property must be present");
//         assertTrue(props.contains("childProp"), "Child property must be present");
//     }

    @Test
    void genericConfigMapping_returnsInterface() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(GenericConfigMapping.class);
        assertNotNull(mapping);
        assertEquals(GenericConfigMapping.class, mapping.getInterfaceType());
    }

//     @Test
//     void defaultMethodConfigMapping_returnsInterface() {
//         ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(DefaultMethodConfigMapping.class);
//         assertNotNull(mapping);
//         assertEquals(DefaultMethodConfigMapping.class, mapping.getInterfaceType());
//         List<String> props = Arrays.stream(mapping.getProperties()).map(p -> p.name()).collect(Collectors.toList());
//         assertTrue(props.contains("mandatory"));
//         assertFalse(props.contains("optional"), "Default methods are not treated as mapping properties");
//     }

    @Test
    void duplicateAnnotation_throwsSpecificException() {
        Exception ex = assertThrows(RuntimeException.class, () -> ConfigMappingLoader.getConfigMapping(DuplicateAnnotationConfigMapping.class));
        assertTrue(ex.getMessage().toLowerCase().contains("duplicate"), "Exception message should mention duplicate @ConfigProperties");
    }

    @Test
    void methodWithParameters_throwsInvalidMethodException() {
        Exception ex = assertThrows(RuntimeException.class, () -> ConfigMappingLoader.getConfigMapping(MethodWithParamsConfigMapping.class));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"), "Exception should indicate an invalid config‑mapping method");
    }

    @Test
    void unsupportedReturnType_throwsInvalidMethodException() {
        Exception ex = assertThrows(RuntimeException.class, () -> ConfigMappingLoader.getConfigMapping(UnsupportedReturnTypeConfigMapping.class));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"), "Exception should indicate an unsupported return type");
    }

    @Test
    void voidReturnType_throwsInvalidMethodException() {
        Exception ex = assertThrows(RuntimeException.class, () -> ConfigMappingLoader.getConfigMapping(VoidReturnConfigMapping.class));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"), "Exception should indicate an invalid (void) return type");
    }

    @Test
    void conflictingPrefixInheritance_throwsInheritanceException() {
        Exception ex = assertThrows(RuntimeException.class, () -> ConfigMappingLoader.getConfigMapping(ChildWithDifferentPrefixConfigMapping.class));
        assertTrue(ex.getMessage().toLowerCase().contains("inheritance"), "Exception should mention inheritance conflict due to different prefixes");
    }

    @Test
    void invalidPrefixFormat_throwsInvalidPrefixException() {
        Exception ex = assertThrows(RuntimeException.class, () -> ConfigMappingLoader.getConfigMapping(InvalidPrefixConfigMapping.class));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"), "Exception should indicate an invalid prefix format");
    }

    @Test
    void distinctInterfacesProduceDistinctImplementationClasses() throws Throwable {
        Class<?> impl1 = invokeGetConfigMappingClass(ValidConfigMapping.class);
        Class<?> impl2 = invokeGetConfigMappingClass(AnotherValidConfigMapping.class);
        assertNotSame(impl1, impl2, "Different interfaces must receive distinct implementation classes");
    }

    @Test
    void concurrentAccessConsistency() throws InterruptedException, ExecutionException {
        final int THREADS = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Class<?>>> futures = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                return invokeGetConfigMappingClass(ValidConfigMapping.class);
            }));
        }
        startLatch.countDown();
        Set<Class<?>> resultSet = new CopyOnWriteArraySet<>();
        for (Future<Class<?>> f : futures) {
            resultSet.add(f.get());
        }
        executor.shutdownNow();
        assertEquals(1, resultSet.size(), "All concurrent invocations should return the same cached implementation class");
    }
}