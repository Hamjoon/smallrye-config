package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.util.Collections;
import java.util.HashMap;
import java.lang.reflect.UndeclaredThrowableException;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_configMappingDefaults_Test {

    /**
     * Mapping from the interface supplied to the test to the synthetic implementation class.
     */
    private static final Map<Class<?>, Class<?>> IMPLEMENTATION_MAP = new HashMap<>();

    /**
     * Original value of the private static final CACHE field – restored after all tests.
     */
    private static Object originalCache;

    /**
     * Holds the reference to the private holder class (ConfigMappingLoader$ConfigMappingObjectHolder).
     */
    private static Class<?> holderClass;

    /**
     * Constructor of the holder class (takes a {@code Class<? extends ConfigMappingObject>}).
     */
    private static Constructor<?> holderCtor;

    @ConfigMapping(prefix = "test")
    interface WithDefaults {

        @WithDefault("localhost")
        String host();

        @WithDefault("8080")
        int port();
    }

    @ConfigMapping(prefix = "empty")
    interface NoDefaults {

        String name();

        int size();
    }

    /**
     * A plain class – not a config‑mapping interface. The loader will still try to
     * generate an implementation class, but that generated class does not provide
     * a static {@code getDefaults()} method, which triggers the expected error.
     */
    static class NotAMapping {
        // No annotations, no methods – just a regular class.
    }

    @ConfigMapping(prefix = "success")
    interface SuccessfulDefaults {
    }

    @ConfigMapping(prefix = "noMethod")
    interface NoDefaultsMethod {
    }

    @ConfigMapping(prefix = "private")
    interface PrivateDefaults {
    }

    @ConfigMapping(prefix = "runtime")
    interface RuntimeExceptionDefaults {
    }

    @ConfigMapping(prefix = "error")
    interface ErrorDefaults {
    }

    @ConfigMapping(prefix = "checked")
    interface CheckedExceptionDefaults {
    }

    @ConfigMapping(prefix = "nullMap")
    interface NullMapDefaults {
    }

    @ConfigMapping(prefix = "empty")
    interface EmptyDefaults {
    }

    @ConfigMapping(prefix = "protected")
    interface ProtectedDefaults {
    }

    @ConfigMapping(prefix = "customRuntime")
    interface CustomRuntimeExceptionDefaults {
    }

    @ConfigMapping(prefix = "customError")
    interface CustomErrorDefaults {
    }

    @ConfigMapping(prefix = "generic")
    interface GenericDefaults {
    }

    @ConfigMapping(prefix = "wrapped")
    interface CheckedWrappedInRuntimeDefaults {
    }

    @ConfigMapping(prefix = "instance")
    interface InstanceDefaults {
    }

    @ConfigMapping(prefix = "param")
    interface ParametrizedDefaults {
    }

    @ConfigMapping(prefix = "secure")
    interface SecureDefaults {
    }

    @ConfigMapping(prefix = "package")
    interface PackagePrivateDefaults {
    }

    @ConfigMapping(prefix = "nullImpl")
    interface NullImplementationDefaults {
    }

    @ConfigMapping(prefix = "inherit")
    interface InheritedDefaults {
    }

    // 1. Successful defaults
    public static class SuccessfulDefaultsImpl {

        public static Map<String, String> getDefaults() {
            Map<String, String> map = new HashMap<>();
            map.put("key1", "value1");
            map.put("key2", "value2");
            return map;
        }
    }

    // 2. No getDefaults method
    public static class NoDefaultsMethodImpl {
        // intentionally empty
    }

    // 3. Private static getDefaults
    public static class PrivateDefaultsImpl {

        private static Map<String, String> getDefaults() {
            return Collections.singletonMap("a", "b");
        }
    }

    // 4. RuntimeException cause
    public static class RuntimeExceptionDefaultsImpl {

        public static Map<String, String> getDefaults() {
            throw new IllegalArgumentException("runtime failure");
        }
    }

    // 5. Error cause
    public static class ErrorDefaultsImpl {

        public static Map<String, String> getDefaults() {
            throw new AssertionError("error condition");
        }
    }

    // 6. Checked exception cause
    public static class CheckedExceptionDefaultsImpl {

        public static Map<String, String> getDefaults() throws IOException {
            throw new IOException("checked failure");
        }
    }

    // 7. Returns null
    public static class NullMapDefaultsImpl {

        public static Map<String, String> getDefaults() {
            return null;
        }
    }

    // 8. Returns empty map
    public static class EmptyDefaultsImpl {

        public static Map<String, String> getDefaults() {
            return Collections.emptyMap();
        }
    }

    // 9. Protected static method
    public static class ProtectedDefaultsImpl {

        protected static Map<String, String> getDefaults() {
            return Collections.singletonMap("x", "y");
        }
    }

    // 10. Custom unchecked runtime exception
    public static class CustomRuntimeExceptionDefaultsImpl {

        public static Map<String, String> getDefaults() {
            throw new CustomRuntimeException("custom runtime failure");
        }
    }

    // 11. Custom Error subclass
    public static class CustomErrorDefaultsImpl {

        public static Map<String, String> getDefaults() {
            throw new CustomError("custom error condition");
        }
    }

    // 12. Generic interface implementation (still static method)
    public static class GenericDefaultsImpl {

        public static Map<String, String> getDefaults() {
            return Collections.singletonMap("genericKey", "genericValue");
        }
    }

    // 13. Checked exception wrapped in RuntimeException
    public static class CheckedWrappedInRuntimeDefaultsImpl {

        public static Map<String, String> getDefaults() {
            try {
                throw new IOException("checked");
            } catch (IOException e) {
                throw new RuntimeException("wrapped checked", e);
            }
        }
    }

    // 14. Non‑static getDefaults
    public static class InstanceDefaultsImpl {

        public Map<String, String> getDefaults() {
            return Collections.singletonMap("instance", "value");
        }
    }

    // 15. Parameterised static method
    public static class ParametrizedDefaultsImpl {

        public static Map<String, String> getDefaults(String prefix) {
            return Collections.singletonMap("p", prefix);
        }
    }

    // 16. SecureDefaults – method exists but lookup is blocked by SecurityManager
    public static class SecureDefaultsImpl {

        public static Map<String, String> getDefaults() {
            return Collections.singletonMap("s", "v");
        }
    }

    // 17. Package‑private static method
    static class PackagePrivateDefaultsImpl {

        static Map<String, String> getDefaults() {
            return Collections.singletonMap("pkg", "val");
        }
    }

    // 18. Inherited getDefaults (superclass provides it, subclass does not declare)
    public static class SuperWithDefaults {

        public static Map<String, String> getDefaults() {
            return Collections.singletonMap("super", "value");
        }
    }

    public static class InheritedDefaultsImpl extends SuperWithDefaults {
        // no own getDefaults declaration
    }

    public static class CustomRuntimeException extends RuntimeException {

        public CustomRuntimeException(String message) {
            super(message);
        }
    }

    public static class CustomError extends Error {

        public CustomError(String message) {
            super(message);
        }
    }

    @BeforeAll
    static void captureOriginalCache() throws Exception {
        // Keep a reference to the original cache so we can restore it later.
        Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        originalCache = cacheField.get(null);
        // Prepare reflective access to the private holder class.
        holderClass = Class.forName("io.smallrye.config.ConfigMappingLoader$ConfigMappingObjectHolder");
        holderCtor = holderClass.getDeclaredConstructor(Class.class);
        holderCtor.setAccessible(true);
    }

    @BeforeEach
    void replaceCache() throws Exception {
        // Build a fresh ClassValue that consults IMPLEMENTATION_MAP.
        ClassValue<?> testCache = new ClassValue<Object>() {

            @Override
            protected Object computeValue(Class<?> type) {
                Class<?> impl = IMPLEMENTATION_MAP.get(type);
                try {
                    // The holder constructor expects a Class<? extends ConfigMappingObject>
                    return holderCtor.newInstance(impl);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        // Replace the private static final CACHE field with our test cache.
        Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(cacheField, cacheField.getModifiers() & ~Modifier.FINAL);
        cacheField.set(null, testCache);
    }

    @AfterEach
    void clearMap() {
        IMPLEMENTATION_MAP.clear();
    }

    @AfterAll
    static void restoreOriginalCache() throws Exception {
        // Restore the original cache after the whole suite.
        Field cacheField = ConfigMappingLoader.class.getDeclaredField("CACHE");
        cacheField.setAccessible(true);
        // Remove final modifier (required for JDK < 12; harmless on newer JDKs)
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(cacheField, cacheField.getModifiers() & ~Modifier.FINAL);
        cacheField.set(null, originalCache);
    }

    /**
     * Verify that default values defined on a {@code @ConfigMapping} interface are
     * correctly extracted by {@code configMappingDefaults}.
     */
    @Test
    void defaultsAreReturned() {
        // Act
        Map<String, String> defaults = ConfigMappingLoader.configMappingDefaults(WithDefaults.class);
        // Assert – the map must contain exactly the entries defined via @WithDefault
        assertEquals(2, defaults.size(), "Exactly two defaults are expected");
        assertEquals("localhost", defaults.get("host"));
        assertEquals("8080", defaults.get("port"));
    }

    /**
     * Verify that an interface without any {@code @WithDefault} annotations yields
     * an empty defaults map.
     */
    @Test
    void emptyWhenNoDefaultsDefined() {
        Map<String, String> defaults = ConfigMappingLoader.configMappingDefaults(NoDefaults.class);
        assertTrue(defaults.isEmpty(), "Defaults map should be empty when no defaults are defined");
    }

    /**
     * Verify that invoking the method with a class ConfigMappingLoader_configMappingDefaults_Testnot a config‑mapping
     * interface results in a {@link NoSuchMethodError}. The generated implementation
     * for such a type does not contain a {@code getDefaults()} method.
     */
    @Test
    void throwsWhenImplementationHasNoGetDefaults() {
        assertThrows(NoSuchMethodError.class, () -> ConfigMappingLoader.configMappingDefaults(NotAMapping.class));
    }

    /**
     * Successful default retrieval – the implementation returns a non‑null map.
     */
    @Test
    void successfulDefaultRetrieval() {
        IMPLEMENTATION_MAP.put(SuccessfulDefaults.class, SuccessfulDefaultsImpl.class);
        Map<String, String> defaults = ConfigMappingLoader.configMappingDefaults(SuccessfulDefaults.class);
        assertEquals(2, defaults.size());
        assertEquals("value1", defaults.get("key1"));
        assertEquals("value2", defaults.get("key2"));
    }

    /**
     * No getDefaults method – should result in NoSuchMethodError.
     */
    @Test
    void noSuchMethodErrorBranch() {
        IMPLEMENTATION_MAP.put(NoDefaultsMethod.class, NoDefaultsMethodImpl.class);
        assertThrows(NoSuchMethodError.class, () -> ConfigMappingLoader.configMappingDefaults(NoDefaultsMethod.class));
    }

    /**
     * Private static getDefaults – IllegalAccessError is expected.
     */
    @Test
    void illegalAccessErrorBranch_PrivateMethod() {
        IMPLEMENTATION_MAP.put(PrivateDefaults.class, PrivateDefaultsImpl.class);
        assertThrows(IllegalAccessError.class, () -> ConfigMappingLoader.configMappingDefaults(PrivateDefaults.class));
    }

    /**
     * RuntimeException caused by getDefaults – should be re‑thrown unchanged.
     */
    @Test
    void invocationTargetExceptionRuntimeExceptionCause() {
        IMPLEMENTATION_MAP.put(RuntimeExceptionDefaults.class, RuntimeExceptionDefaultsImpl.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.configMappingDefaults(RuntimeExceptionDefaults.class));
        assertEquals("runtime failure", ex.getMessage());
    }

    /**
     * Error caused by getDefaults – should be propagated unchanged.
     */
    @Test
    void invocationTargetExceptionErrorCause() {
        IMPLEMENTATION_MAP.put(ErrorDefaults.class, ErrorDefaultsImpl.class);
        AssertionError err = assertThrows(AssertionError.class, () -> ConfigMappingLoader.configMappingDefaults(ErrorDefaults.class));
        assertEquals("error condition", err.getMessage());
    }

    /**
     * Checked exception cause – must be wrapped in UndeclaredThrowableException.
     */
    @Test
    void invocationTargetExceptionCheckedExceptionCause() {
        IMPLEMENTATION_MAP.put(CheckedExceptionDefaults.class, CheckedExceptionDefaultsImpl.class);
        UndeclaredThrowableException ute = assertThrows(UndeclaredThrowableException.class, () -> ConfigMappingLoader.configMappingDefaults(CheckedExceptionDefaults.class));
        assertTrue(ute.getCause() instanceof IOException);
        assertEquals("checked failure", ute.getCause().getMessage());
    }

    /**
     * Null interface type – should raise NullPointerException from the cache lookup.
     */
    @Test
    void nullInterfaceTypeHandling() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.configMappingDefaults(null));
    }

    /**
     * Implementation inherits getDefaults – NoSuchMethodError because method is not declared.
     */
    @Test
    void inheritedGetDefaultsNotDeclared() {
        IMPLEMENTATION_MAP.put(InheritedDefaults.class, InheritedDefaultsImpl.class);
        assertThrows(NoSuchMethodError.class, () -> ConfigMappingLoader.configMappingDefaults(InheritedDefaults.class));
    }

    /**
     * getDefaults returns null – the loader should return null.
     */
    @Test
    void implementationReturnsNullMap() {
        IMPLEMENTATION_MAP.put(NullMapDefaults.class, NullMapDefaultsImpl.class);
        Map<String, String> result = ConfigMappingLoader.configMappingDefaults(NullMapDefaults.class);
        assertNull(result, "Expected null map when implementation returns null");
    }

    /**
     * getDefaults returns an empty map – the loader should return the same empty map.
     */
    @Test
    void implementationReturnsEmptyMap() {
        IMPLEMENTATION_MAP.put(EmptyDefaults.class, EmptyDefaultsImpl.class);
        Map<String, String> result = ConfigMappingLoader.configMappingDefaults(EmptyDefaults.class);
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Expected empty map");
    }

    /**
     * Protected static getDefaults – IllegalAccessError expected.
     */
    @Test
    void protectedStaticGetDefaults() {
        IMPLEMENTATION_MAP.put(ProtectedDefaults.class, ProtectedDefaultsImpl.class);
        assertThrows(IllegalAccessError.class, () -> ConfigMappingLoader.configMappingDefaults(ProtectedDefaults.class));
    }

    /**
     * Custom unchecked runtime exception – should be re‑thrown unchanged.
     */
    @Test
    void customRuntimeExceptionPropagation() {
        IMPLEMENTATION_MAP.put(CustomRuntimeExceptionDefaults.class, CustomRuntimeExceptionDefaultsImpl.class);
        CustomRuntimeException ex = assertThrows(CustomRuntimeException.class, () -> ConfigMappingLoader.configMappingDefaults(CustomRuntimeExceptionDefaults.class));
        assertEquals("custom runtime failure", ex.getMessage());
    }

    /**
     * Custom Error subclass – should be propagated unchanged.
     */
    @Test
    void customErrorPropagation() {
        IMPLEMENTATION_MAP.put(CustomErrorDefaults.class, CustomErrorDefaultsImpl.class);
        CustomError err = assertThrows(CustomError.class, () -> ConfigMappingLoader.configMappingDefaults(CustomErrorDefaults.class));
        assertEquals("custom error condition", err.getMessage());
    }

    /**
     * Generic interface implementation – static getDefaults must be found.
     */
    @Test
    void genericInterfaceImplementation() {
        IMPLEMENTATION_MAP.put(GenericDefaults.class, GenericDefaultsImpl.class);
        Map<String, String> result = ConfigMappingLoader.configMappingDefaults(GenericDefaults.class);
        assertEquals(1, result.size());
        assertEquals("genericValue", result.get("genericKey"));
    }

    /**
     * Checked exception wrapped in RuntimeException – RuntimeException should be propagated.
     */
    @Test
    void checkedExceptionWrappedInRuntime() {
        IMPLEMENTATION_MAP.put(CheckedWrappedInRuntimeDefaults.class, CheckedWrappedInRuntimeDefaultsImpl.class);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ConfigMappingLoader.configMappingDefaults(CheckedWrappedInRuntimeDefaults.class));
        assertEquals("wrapped checked", ex.getMessage());
    }

    /**
     * Non‑static getDefaults – NullPointerException is expected (method invoked on null).
     */
    @Test
    void nonStaticGetDefaults() {
        IMPLEMENTATION_MAP.put(InstanceDefaults.class, InstanceDefaultsImpl.class);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> ConfigMappingLoader.configMappingDefaults(InstanceDefaults.class));
        // Message may be null; we only assert the type.
    }

    /**
     * getDefaults with parameters – IllegalArgumentException due to argument mismatch.
     */
    @Test
    void getDefaultsWithParameters() {
        IMPLEMENTATION_MAP.put(ParametrizedDefaults.class, ParametrizedDefaultsImpl.class);
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.configMappingDefaults(ParametrizedDefaults.class));
        assertTrue(iae.getMessage().contains("argument type mismatch"));
    }

    /**
     * SecurityException during method lookup – should propagate unchanged.
     */
    @Test
    void securityExceptionDuringLookup() {
        IMPLEMENTATION_MAP.put(SecureDefaults.class, SecureDefaultsImpl.class);
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkMemberAccess(Class<?> clazz, int which) {
                if (clazz == SecureDefaultsImpl.class) {
                    throw new SecurityException("access denied");
                }
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }
        });
        try {
            SecurityException se = assertThrows(SecurityException.class, () -> ConfigMappingLoader.configMappingDefaults(SecureDefaults.class));
            assertTrue(se.getMessage().contains("access denied"));
        } finally {
            System.setSecurityManager(original);
        }
    }

    /**
     * Package‑private static getDefaults – IllegalAccessError expected.
     */
    @Test
    void packagePrivateStaticGetDefaults() {
        IMPLEMENTATION_MAP.put(PackagePrivateDefaults.class, PackagePrivateDefaultsImpl.class);
        assertThrows(IllegalAccessError.class, () -> ConfigMappingLoader.configMappingDefaults(PackagePrivateDefaults.class));
    }

    /**
     * Implementation class is null – NullPointerException referencing implementationClass.
     */
    @Test
    void nullImplementationClassHandling() {
        IMPLEMENTATION_MAP.put(NullImplementationDefaults.class, null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> ConfigMappingLoader.configMappingDefaults(NullImplementationDefaults.class));
        assertTrue(npe.getMessage().contains("implementationClass"));
    }
}
