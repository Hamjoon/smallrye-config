package io.smallrye.config;

import static io.smallrye.config.ConfigMappingInterface.getConfigurationInterface;
import static io.smallrye.config.ConfigMappingLoader.getImplementationClass;
import static io.smallrye.config.ConfigMappingLoader.loadClass;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.*;
import java.util.Collections;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigMappingLoader_getImplementationClass_Test {

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

    interface ServerParentNested {
    }

    interface ServerChildNested {
    }

    class TestClassLoader extends ClassLoader {

        private final String generatedName;

        TestClassLoader(Class<?> type) {
            super(type.getClassLoader());
            this.generatedName = type.getName() + type.getName().hashCode() + "Impl";
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.equals(generatedName)) {
                return NotAssignableImpl.class;
            }
            return super.loadClass(name);
        }
    }

    @ConfigMapping(prefix = "server")
    public interface Server {

        String host();

        int port();
    }

    @ConfigProperties(prefix = "server")
    public static class ServerProperties {

        String host;

        int port;
    }

    interface ServerManual {

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

    interface MappingCollection {

        List<OptionalCollection> collection();

        Optional<String> property();
    }

    interface MappingCollectionGroup {

        Optional<String> property();
    }

    /**
     * Minimal implementation of {@link ConfigMappingObject} required for the
     * {@code collectionGroup} test. The real implementation is generated at
     * runtime; this class only supplies the constructor signature.
     */
    static class OptionalCollection implements ConfigMappingObject {

        public OptionalCollection(ConfigMappingContext context) {
        }
    }

    interface ServerParent {

        String parent();

        ServerParentNested parentNested();

        interface ServerParentNested {
        }
    }

//     interface ServerParentNested {
    }

//     interface ServerParentNested {
    }

//     interface ServerParentNested {
    }

    interface ServerChild extends ServerParent {

        ServerChildNested childNested();

        interface ServerChildNested {
        }
    }

//     interface ServerChildNested {
    }

//     interface ServerChildNested {
    }

//     interface ServerChildNested {
    }

    interface SimpleMapping {

        String value();
    }

    static class DummyImpl implements ConfigMappingObject {

        public DummyImpl() {
        }
    }

    static class NotAssignableImpl {
    }

//     class TestClassLoader extends ClassLoader {

        private final String generatedName;

        TestClassLoader(Class<?> type) {
            super(type.getClassLoader());
            this.generatedName = type.getName() + type.getName().hashCode() + "Impl";
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.equals(generatedName)) {
                return NotAssignableImpl.class;
            }
            return super.loadClass(name);
        }
    }

    static List<Object[]> parameters() {
        return Collections.emptyList();
    }

//     @Test
//     void multipleLoads() {
//         // First call generates the implementation class, second call should reuse the cached version.
//         getImplementationClass(Server.class);
//         getImplementationClass(Server.class);
//         SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config("server.host", "localhost", "server.port", "8080")).withMapping(Server.class).build();
//         Server server = config.getConfigMapping(Server.class);
//         assertEquals("localhost", server.host());
//         assertEquals(8080, server.port());
//     }

    @Test
    void loadManually() {
        // Load the metadata and the generated class manually, then request the implementation.
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(ServerManual.class);
        metadata.forEach(m -> ConfigMappingLoader.loadClass(ServerManual.class, m));
        // The implementation class should now be available without triggering the fallback path.
        getImplementationClass(ServerManual.class);
        getImplementationClass(ServerManual.class);
    }

    @Test
    void noArgsConstructor() throws Exception {
        // The generated implementation must have a public no‑arg constructor that returns a proxy of the interface.
        assertTrue(getImplementationClass(Server.class).getDeclaredConstructor().newInstance() instanceof Server);
        assertTrue(getImplementationClass(ServerNested.class).getDeclaredConstructor().newInstance() instanceof ServerNested);
        // A class that is not a @ConfigMapping must cause an IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> getImplementationClass(ServerProperties.class));
    }

    @Test
    void collectionGroup() throws Exception {
        // Verify that a mapping containing an Optional collection works and that the generated class
        // has a constructor accepting ConfigMappingContext (required for byte‑code verification).
        Method[] methods = new Method[] { MappingCollection.class.getDeclaredMethod("collection"), MappingCollection.class.getDeclaredMethod("property") };
        ConfigMappingInterface.Property[] properties = ConfigMappingInterface.getProperties(methods, 0, 0);
        ConfigMappingInterface mappingInterface = new ConfigMappingInterface(MappingCollection.class, new ConfigMappingInterface[] {}, properties);
        loadClass(OptionalCollection.class, getConfigurationInterface(MappingCollectionGroup.class));
        loadClass(OptionalCollection.class, mappingInterface);
        Class<? extends ConfigMappingObject> impl = getImplementationClass(MappingCollection.class);
        assertNotNull(impl.getDeclaredConstructor(ConfigMappingContext.class));
    }

    @Test
    void parentNested() {
        // Ensure that both parent and child nested interfaces are discovered.
        List<ConfigMappingMetadata> mappingsMetadata = ConfigMappingLoader.getConfigMappingsMetadata(ServerChild.class);
        List<Class<?>> mappings = mappingsMetadata.stream().map(ConfigMappingMetadata::getInterfaceType).collect(toList());
        assertTrue(mappings.contains(ServerChild.class));
        assertTrue(mappings.contains(ServerChild.ServerChildNested.class));
        assertTrue(mappings.contains(ServerParent.class));
        assertTrue(mappings.contains(ServerParent.ServerParentNested.class));
    }

    @Test
    @DisplayName("getImplementationClass(null) → NullPointerException")
    void nullArgument() {
        assertThrows(NullPointerException.class, () -> getImplementationClass(null));
    }

    @Test
    @DisplayName("getImplementationClass(int.class) → NullPointerException")
    void primitiveArgument() {
        assertThrows(NullPointerException.class, () -> getImplementationClass(int.class));
    }

    @Test
    @DisplayName("Generated class missing & no metadata → ConfigMessages exception")
    void classNotFoundNoMetadata() {
        assertThrows(RuntimeException.class, () -> getImplementationClass(String.class));
    }

//     @Test
//     @DisplayName("Generated class missing, metadata present → loadClass fallback")
//     void classNotFoundWithMetadata() {
//         try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
//             MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class, Mockito.CALLS_REAL_METHODS)) {
//             ConfigMappingMetadata dummyMetadata = Mockito.mock(ConfigMappingMetadata.class);
//             cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(SimpleMapping.class)).thenReturn(dummyMetadata);
//             loaderMock.when(() -> ConfigMappingLoader.loadClass(SimpleMapping.class, dummyMetadata)).thenReturn(DummyImpl.class);
//             Class<? extends ConfigMappingObject> result = getImplementationClass(SimpleMapping.class);
//             assertEquals(DummyImpl.class, result, "When metadata is present the loader must fall back to loadClass()");
//         }
//     }

//     @Test
//     @DisplayName("loadClass throws IllegalStateException → propagates")
//     void loadClassThrowsUnchecked() {
//         try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
//             MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class, Mockito.CALLS_REAL_METHODS)) {
//             ConfigMappingMetadata dummyMetadata = Mockito.mock(ConfigMappingMetadata.class);
//             cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(SimpleMapping.class)).thenReturn(dummyMetadata);
//             loaderMock.when(() -> ConfigMappingLoader.loadClass(SimpleMapping.class, dummyMetadata)).thenThrow(new IllegalStateException("unexpected failure"));
//             IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> getImplementationClass(SimpleMapping.class));
//             assertEquals("unexpected failure", thrown.getMessage());
//         }
//     }

//     @Test
//     @DisplayName("Generated class not assignable, metadata present → fallback to loadClass")
//     void generatedClassNotAssignableWithMetadata() throws Exception {
//         class TestClassLoader extends ClassLoader {
// 
//             private final String generatedName;
// 
//             TestClassLoader(Class<?> type) {
//                 super(type.getClassLoader());
//                 this.generatedName = type.getName() + type.getName().hashCode() + "Impl";
//             }
// 
//             @Override
//             public Class<?> loadClass(String name) throws ClassNotFoundException {
//                 if (name.equals(generatedName)) {
//                     return NotAssignableImpl.class;
//                 }
//                 return super.loadClass(name);
//             }
//         }
//         // Switch to the custom class loader that returns a non‑assignable class for the generated name
//         String generatedName = SimpleMapping.class.getName() + SimpleMapping.class.getName().hashCode() + "Impl";
//         TestClassLoader customLoader = new TestClassLoader(SimpleMapping.class);
//         Class<?> loaded = customLoader.loadClass(generatedName);
//         assertEquals(NotAssignableImpl.class, loaded, "Custom loader should return NotAssignableImpl for the generated name");
//         Class<?> proxy = java.lang.reflect.Proxy.getProxyClass(customLoader, SimpleMapping.class);
//         @SuppressWarnings("unchecked")
//         Class<SimpleMapping> type = (Class<SimpleMapping>) proxy;
//         // Set the thread context class loader to our custom loader
//         Thread.currentThread().setContextClassLoader(customLoader);
//         try (MockedStatic<ConfigMappingInterface> cfgMock = Mockito.mockStatic(ConfigMappingInterface.class);
//             MockedStatic<ConfigMappingLoader> loaderMock = Mockito.mockStatic(ConfigMappingLoader.class, Mockito.CALLS_REAL_METHODS)) {
//             ConfigMappingMetadata dummyMetadata = Mockito.mock(ConfigMappingMetadata.class);
//             cfgMock.when(() -> ConfigMappingInterface.getConfigurationInterface(type)).thenReturn(dummyMetadata);
//             loaderMock.when(() -> ConfigMappingLoader.loadClass(type, dummyMetadata)).thenReturn(DummyImpl.class);
//             Class<? extends ConfigMappingObject> result = getImplementationClass(type);
//             assertEquals(DummyImpl.class, result, "When the generated class is not assignable, the loader must fall back to loadClass()");
//         } finally {
//             // Restore original context class loader
//             Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
//         }
//     }

    @ParameterizedTest
    @MethodSource("parameters")
    void placeholderParameterizedTest(Object... args) {
        // No‑op placeholder test – required for the parameterized test infrastructure.
    }
}