package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import io.smallrye.config.ConfigMapping.NamingStrategy;
import java.util.concurrent.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getConfigurationInterface_Test {

    @ConfigMapping
    interface InnerConfig {

        String inner();
    }

    @ConfigMapping
//     interface InnerConfig {

        String inner();
    }

    // A valid inner configuration interface (used in a positive test)
    @ConfigMapping
//     interface InnerConfig {

        String inner();
    }

    @ConfigMapping
//     interface InnerConfig {

        String inner();
    }

    @ConfigMapping
//     interface InnerConfig {

        String inner();
    }

    // A valid inner configuration interface (used in a positive test)
    @ConfigMapping
//     interface InnerConfig {

        String inner();
    }

    // ----------------------------------------------------------------------
    @ConfigMapping
    interface SampleConfig {

        String name();
    }

    @ConfigMapping
    interface AnotherConfig {

        int age();
    }

    // No @ConfigMapping annotation – should be rejected
    interface NoAnnotationConfig {

        String foo();
    }

    // Generic interface – should be rejected
    @ConfigMapping
    interface GenericConfig<T> {

        T value();
    }

    @ConfigMapping
    interface DefaultMethodConfig {

        String key();

        default String defaultValue() {
            return "default";
        }
    }

    @ConfigMapping
    interface ChildConfig extends SampleConfig {

        String address();
    }

    // Super‑interface without @ConfigMapping – child is annotated
    interface NonAnnotatedSuper {

        String something();
    }

    @ConfigMapping
    interface ChildExtendsNonAnnotatedConfig extends NonAnnotatedSuper {

        String other();
    }

    @ConfigMapping
    interface MethodWithParamsConfig {

        String valid();

        // Invalid: method with a parameter
        String invalid(String param);
    }

    // Custom type without a registered converter
    static class CustomType {
    }

    @ConfigMapping
    interface UnsupportedReturnConfig {

        CustomType custom();
    }

    @ConfigMapping
    interface DuplicatePropertyConfig {

        @ConfigProperty(name = "dup")
        String first();

        @ConfigProperty(name = "dup")
        String second();
    }

    // Non‑static inner interface – must be rejected
    class Outer {

        @ConfigMapping
        interface InnerConfig {

            String inner();
        }
    }

    @ConfigMapping
//     interface InnerConfig {

        String inner();
    }

    @ConfigMapping
//     interface InnerConfig {

        String inner();
    }

    // Two super‑interfaces with conflicting naming strategies
    @ConfigMapping(namingStrategy = NamingStrategy.KEBAB_CASE)
    interface ParentA {

        String fooBar();
    }

    @ConfigMapping(namingStrategy = NamingStrategy.SNAKE_CASE)
    interface ParentB {

        String fooBar();
    }

    @ConfigMapping
    interface ConflictingNamingStrategyConfig extends ParentA, ParentB {

        String somethingElse();
    }

    // Simple configuration interface used in a positive test
    @ConfigMapping
    interface Server {

        String host();

        int port();
    }

    static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of(int.class), Arguments.of(String[].class), Arguments.of(void.class));
    }

    @Test
    void shouldRejectNullArgument() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(null));
        assertTrue(thrown.getMessage().contains("interfaceType"));
    }

    @Test
    void shouldReturnConfigMappingInterfaceForValidMapping() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(Server.class);
        assertNotNull(mapping, "Mapping must not be null for a valid @ConfigMapping interface");
        // The returned object must implement ConfigMappingMetadata (the interface that ConfigMappingInterface implements)
        assertTrue(mapping instanceof ConfigMappingMetadata);
    }

    @Test
    void shouldCacheResultForSameClass() {
        ConfigMappingInterface first = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        ConfigMappingInterface second = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        assertSame(first, second, "Subsequent calls with the same class should return the cached instance");
    }

    @Test
    void differentInterfacesProduceDifferentInstances() {
        ConfigMappingInterface sample = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        ConfigMappingInterface another = ConfigMappingInterface.getConfigurationInterface(AnotherConfig.class);
        assertNotSame(sample, another, "Two distinct @ConfigMapping interfaces must yield different ConfigMappingInterface objects");
    }

    @Test
    void nonInterfaceClassReturnsNull() {
        // According to the javadoc the method may return null for non‑configuration types.
        ConfigMappingInterface result = ConfigMappingInterface.getConfigurationInterface(String.class);
        assertNull(result, "A non‑interface class should result in a null ConfigMappingInterface");
    }

    @ParameterizedTest(name = "Invalid type {0} should cause IllegalArgumentException")
    @MethodSource("invalidTypeProvider")
    void invalidTypesShouldThrowIllegalArgumentException(Class<?> invalidType) {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(invalidType));
        assertTrue(thrown.getMessage().contains("interfaceType"));
    }

    @Test
    void interfaceWithoutConfigMappingAnnotationShouldBeRejected() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(NoAnnotationConfig.class));
        assertTrue(thrown.getMessage().contains("@ConfigMapping"));
    }

    @Test
    void genericInterfaceShouldBeRejected() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(GenericConfig.class));
        assertTrue(thrown.getMessage().toLowerCase().contains("generic"));
    }

    @Test
    void interfaceWithDefaultMethodIsAccepted() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(DefaultMethodConfig.class);
        assertNotNull(mapping, "Interface containing default methods must still be processed");
    }

    @Test
    void childConfigInterfaceIsDistinctFromParent() {
        ConfigMappingInterface parent = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        ConfigMappingInterface child = ConfigMappingInterface.getConfigurationInterface(ChildConfig.class);
        assertNotSame(parent, child, "Child @ConfigMapping interface must have its own ConfigMappingInterface instance");
    }

    @Test
    void threadSafetyConcurrentRetrievalReturnsSameInstance() throws Exception {
        final int THREAD_COUNT = 20;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<ConfigMappingInterface>> futures = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> ConfigMappingInterface.getConfigurationInterface(SampleConfig.class)));
        }
        // Wait for all tasks to finish
        ConfigMappingInterface first = futures.get(0).get();
        for (Future<ConfigMappingInterface> f : futures) {
            ConfigMappingInterface result = f.get();
            assertSame(first, result, "All threads must receive the identical ConfigMappingInterface instance");
        }
        executor.shutdownNow();
    }

    @Test
    void repeatedCallsAfterCachingMaintainIdentity() {
        // First call – caches the instance
        ConfigMappingInterface cached = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        // Interleave with a different interface to ensure the cache is not overwritten
        ConfigMappingInterface other = ConfigMappingInterface.getConfigurationInterface(AnotherConfig.class);
        assertNotNull(other);
        // Subsequent call for the original interface must return the original cached object
        ConfigMappingInterface later = ConfigMappingInterface.getConfigurationInterface(SampleConfig.class);
        assertSame(cached, later, "Caching must be stable over time");
    }

    @Test
    void childExtendingNonAnnotatedSuperinterfaceIsRejected() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(ChildExtendsNonAnnotatedConfig.class));
        assertTrue(thrown.getMessage().toLowerCase().contains("@configmapping"));
    }

    @Test
    void interfaceWithMethodHavingParametersIsRejected() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(MethodWithParamsConfig.class));
        assertTrue(thrown.getMessage().toLowerCase().contains("no‑arg"));
    }

    @Test
    void interfaceWithUnsupportedReturnTypeIsRejected() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(UnsupportedReturnConfig.class));
        assertTrue(thrown.getMessage().toLowerCase().contains("converter"));
    }

    @Test
    void interfaceWithDuplicatePropertyKeysIsRejected() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ConfigMappingInterface.getConfigurationInterface(DuplicatePropertyConfig.class));
        assertTrue(thrown.getMessage().toLowerCase().contains("duplicate"));
    }

    @Test
    void nonStaticInnerInterfaceIsRejected() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(Outer.InnerConfig.class));
        assertTrue(thrown.getMessage().toLowerCase().contains("static"));
    }

    @Test
    void conflictingNamingStrategiesInHierarchyIsRejected() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingInterface.getConfigurationInterface(ConflictingNamingStrategyConfig.class));
        assertTrue(thrown.getMessage().toLowerCase().contains("naming strategy"));
    }
}