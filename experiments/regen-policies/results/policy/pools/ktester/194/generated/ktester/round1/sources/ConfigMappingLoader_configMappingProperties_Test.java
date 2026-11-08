package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.io.IOException;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigMappingLoader_configMappingProperties_Test {

    private static final Map<Class<?>, Class<?>> IMPLEMENTATION_MAP = new HashMap<>();

    // keep a reference so we can restore it after all tests
    private static ClassValue<?> originalCache;

    @ConfigMapping(prefix = "dummy")
    interface DummyMapping {

        String name();

        int port();
    }

    interface NotAMapping {

        String something();
    }

    @ConfigMapping(prefix = "normal")
    interface NormalConfig {
    }

    @ConfigMapping(prefix = "noMethod")
    interface NoMethodConfig {
    }

    @ConfigMapping(prefix = "privateMethod")
    interface PrivateMethodConfig {
    }

    @ConfigMapping(prefix = "runtimeException")
    interface RuntimeExceptionInGetterConfig {
    }

    @ConfigMapping(prefix = "error")
    interface ErrorInGetterConfig {
    }

    @ConfigMapping(prefix = "checked")
    interface CheckedExceptionInGetterConfig {
    }

    @ConfigMapping(prefix = "directRuntime")
    interface DirectRuntimeExceptionConfig {
    }

    @ConfigMapping(prefix = "throwable")
    interface ThrowableConfig {
    }

    @ConfigMapping(prefix = "nullMap")
    interface NullMapConfig {
    }

    @ConfigMapping(prefix = "emptyMap")
    interface EmptyMapConfig {
    }

    @ConfigMapping(prefix = "multi")
    interface MultiEntryConfig {
    }

    @ConfigMapping(prefix = "directIllegalArgument")
    interface DirectIllegalArgumentExceptionConfig {
    }

    @ConfigMapping(prefix = "wrongSignature")
    interface WrongSignatureConfig {
    }

    @ConfigMapping(prefix = "null")
    interface // never instantiated – used only for the “null” case
    NullInterfaceType {
    }

    public static class NormalConfigImpl {

        public static Map<String, String> getProperties() {
            Map<String, String> map = new HashMap<>();
            map.put("key", "value");
            return map;
        }
    }

    public static class NoMethodConfigImpl {
        // intentionally empty – no getProperties()
    }

    public static class PrivateMethodConfigImpl {

        @SuppressWarnings("unused")
        private static Map<String, String> getProperties() {
            return Collections.emptyMap();
        }
    }

    public static class RuntimeExceptionInGetterConfigImpl {

        public static Map<String, String> getProperties() {
            throw new IllegalStateException("boom");
        }
    }

    public static class ErrorInGetterConfigImpl {

        public static Map<String, String> getProperties() {
            throw new AssertionError("assert");
        }
    }

    public static class CheckedExceptionInGetterConfigImpl {

        public static Map<String, String> getProperties() throws IOException {
            throw new IOException("io");
        }
    }

    public static class DirectRuntimeExceptionConfigImpl {

        public static Map<String, String> getProperties() {
            throw new NullPointerException("direct NPE");
        }
    }

    public static class ThrowableConfigImpl {

        public static Map<String, String> getProperties() throws Throwable {
            throw new Throwable("custom");
        }
    }

    public static class NullMapConfigImpl {

        public static Map<String, String> getProperties() {
            return null;
        }
    }

    public static class EmptyMapConfigImpl {

        public static Map<String, String> getProperties() {
            return Collections.emptyMap();
        }
    }

    public static class MultiEntryConfigImpl {

        public static Map<String, String> getProperties() {
            Map<String, String> map = new HashMap<>();
            map.put("host", "localhost");
            map.put("port", "8080");
            map.put("enabled", "true");
            return map;
        }
    }

    public static class DirectIllegalArgumentExceptionConfigImpl {

        public static Map<String, String> getProperties() {
            throw new IllegalArgumentException("direct IAE");
        }
    }

    public static class WrongSignatureConfigImpl {

        public static List<String> getProperties() {
            return List.of("oops");
        }
    }

    @BeforeAll
    static void replaceCache() throws Exception {
        // 1. Obtain the original cache so we can restore it later.
        Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        originalCache = (ClassValue<?>) cacheField.get(null);
        // 2. Locate the private static inner class ConfigMappingObjectHolder.
        Class<?> holderClass = null;
        for (Class<?> c : ConfigMappingLoader.class.getDeclaredClasses()) {
            if (c.getSimpleName().equals("ConfigMappingObjectHolder")) {
                holderClass = c;
                break;
            }
        }
        if (holderClass == null) {
            throw new IllegalStateException("Could not locate ConfigMappingObjectHolder");
        }
        Constructor<?> holderCtor = holderClass.getDeclaredConstructor(Class.class);
        holderCtor.setAccessible(true);
        // 3. Create a ClassValue that returns a holder wrapping the implementation class.
        ClassValue<Object> testCache = new ClassValue<>() {

            @Override
            protected Object computeValue(Class<?> type) {
                Class<?> impl = IMPLEMENTATION_MAP.get(type);
                if (impl == null) {
                    // Fallback to the original cache for types we don't control.
                    try {
                        return originalCache.get(type);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                try {
                    return holderCtor.newInstance(impl);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        // 4. Replace the static final field.
        setStaticFinalField(cacheField, testCache);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Force generation of the implementation class for the dummy mapping.
        ConfigMappingLoader.getImplementationClass(DummyMapping.class);
    }

    @AfterAll
    static void restoreCache() throws Exception {
        Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        setStaticFinalField(cacheField, originalCache);
    }

    private static Stream<Arguments> testCases() {
        return Stream.of(// Normal flow – returns properties map
        Arguments.of(NormalConfig.class, Map.of("key", "value"), null), // No static getProperties()
        Arguments.of(NoMethodConfig.class, null, NoSuchMethodError.class), // Private static getProperties()
        Arguments.of(PrivateMethodConfig.class, null, IllegalAccessError.class), // RuntimeException thrown from getter
        Arguments.of(RuntimeExceptionInGetterConfig.class, null, IllegalStateException.class), // Error thrown from getter
        Arguments.of(ErrorInGetterConfig.class, null, AssertionError.class), // Checked exception wrapped in UndeclaredThrowableException
        Arguments.of(CheckedExceptionInGetterConfig.class, null, UndeclaredThrowableException.class), // Direct RuntimeException from getter
        Arguments.of(DirectRuntimeExceptionConfig.class, null, NullPointerException.class), // Throwable thrown from getter – wrapped
        Arguments.of(ThrowableConfig.class, null, UndeclaredThrowableException.class), // Null interface type (should cause NPE)
        Arguments.of(null, null, NullPointerException.class), // Implementation returns null map (allowed)
        Arguments.of(NullMapConfig.class, null, null), // Implementation returns empty map
        Arguments.of(EmptyMapConfig.class, Collections.emptyMap(), null), // Implementation returns map with multiple entries
        Arguments.of(MultiEntryConfig.class, Map.of("host", "localhost", "port", "8080", "enabled", "true"), null), // Direct unchecked IllegalArgumentException from getter
        Arguments.of(DirectIllegalArgumentExceptionConfig.class, null, IllegalArgumentException.class), // Wrong method signature – IllegalArgumentException from lookup
        Arguments.of(WrongSignatureConfig.class, null, IllegalArgumentException.class));
    }

    /* ---------------------------------------------------------------------- */
    private static void setStaticFinalField(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        // Use Unsafe – works on all supported JDK versions.
        sun.misc.Unsafe unsafe = getUnsafe();
        Object staticBase = unsafe.staticFieldBase(field);
        long staticOffset = unsafe.staticFieldOffset(field);
        unsafe.putObject(staticBase, staticOffset, newValue);
    }

    private static sun.misc.Unsafe getUnsafe() throws Exception {
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }

    @Test
    void configMappingPropertiesReturnsNonNullMap() {
        Map<String, String> props = ConfigMappingLoader.configMappingProperties(DummyMapping.class);
        assertNotNull(props, "The returned map must never be null");
        // The dummy mapping does not declare any @WithDefault values, therefore the map is empty.
        assertTrue(props.isEmpty(), "When no defaults are defined the map should be empty");
    }

    @Test
    void configMappingPropertiesPropagatesIllegalArgumentExceptionForNonMapping() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.configMappingProperties(NotAMapping.class));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("testCases")
    @DisplayName("configMappingProperties – behaviour driven by JSON spec")
    void configMappingPropertiesBehaviour(Class<?> interfaceType, Map<String, String> expectedMap, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> ConfigMappingLoader.configMappingProperties(interfaceType));
        } else {
            Map<String, String> result = ConfigMappingLoader.configMappingProperties(interfaceType);
            if (expectedMap == null) {
                assertNull(result, "Expected a null map for interface " + interfaceType);
            } else {
                assertNotNull(result, "Resulting map must not be null for interface " + interfaceType);
                assertEquals(expectedMap, result, "Returned map does not match the expected content for interface " + interfaceType);
            }
        }
    }

    @Test
    @DisplayName("Happy‑path – DummyMapping without defaults returns empty map")
    void dummyMappingReturnsEmptyMap() {
        ConfigMappingLoader.getImplementationClass(DummyMapping.class);
        Map<String, String> props = ConfigMappingLoader.configMappingProperties(DummyMapping.class);
        assertNotNull(props);
        assertTrue(props.isEmpty(), "When no defaults are defined the map should be empty");
    }

    @Test
    @DisplayName("Non‑mapping interface propagates IllegalArgumentException")
    void nonMappingInterfaceThrows() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.configMappingProperties(NotAMapping.class));
    }
}
