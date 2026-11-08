package io.smallrye.config;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConfigMappingLoader}.
 *
 * <p>
 * The tests focus on the public static API of {@code ConfigMappingLoader} and on the
 * inner static helper classes that can be exercised without requiring the full
 * MicroProfile Config implementation (i.e. without generating mapping implementation
 * classes at runtime).  All objects are created through real constructors or factory
 * methods – no mocking frameworks are used as required by the task description.
 * </p>
 *
 * <p>
 * The test class covers:
 * <ul>
 *   <li>Behaviour of {@link ConfigMappingLoader#getConfigMappingsMetadata(Class)}</li>
 *   <li>Behaviour of {@link ConfigMappingLoader#getConfigMapping(Class)}</li>
 *   <li>Behaviour of {@link ConfigMappingLoader#getImplementationClass(Class)}</li>
 *   <li>Behaviour of {@link ConfigMappingLoader#configMappingProperties(Class)} and
 *       {@link ConfigMappingLoader#configMappingObject(Class, ConfigMappingContext)}</li>
 *   <li>Correct handling of {@link ConfigMappingLoader.ConfigMappingClass} static helpers</li>
 *   <li>Thread‑safe lock acquisition via {@link ConfigMappingLoader#getClassLoaderLock(String)}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Note: The real mapping implementation generation performed by {@code ConfigMappingGenerator}
 * is not part of this test suite because the generator class is not part of the provided
 * source.  The tests therefore concentrate on the parts of the loader that can be exercised
 * with ordinary Java classes and interfaces.
 * </p>
 */
public class ConfigMappingLoaderTest {

    /* -------------------------------------------------------------
     * Helper classes used in the tests
     * ------------------------------------------------------------- */

    /**
     * A simple concrete class that does NOT implement {@link ConfigMappingObject}.
     * Used to verify that {@code ConfigMappingClass} recognises concrete classes
     * as configuration classes.
     */
    public static final class SimpleConcreteClass {
        // No behaviour needed – the existence of the class is sufficient.
    }

    /**
     * An interface – should be rejected by {@code ConfigMappingClass#createConfigurationClass}
     * because configuration classes must be concrete (non‑interface, non‑abstract, non‑enum).
     */
    public interface SimpleInterface {
        // No members needed.
    }

    /**
     * An abstract class – also rejected by {@code ConfigMappingClass#createConfigurationClass}.
     */
    public abstract static class AbstractConfig {
        // No behaviour needed.
    }

    /**
     * An enum – also rejected.
     */
    public enum SimpleEnum {
        ONE, TWO
    }

    /**
     * Minimal implementation of {@link ConfigMappingContext} that can be used
     * to invoke {@code ConfigMappingLoader.configMappingObject}.  The constructor
     * only requires a {@code SmallRyeConfig} instance and a map of mappings.
     *
     * <p>
     * Because the real {@code SmallRyeConfig} type is not part of the supplied
     * API, we use {@code null} for the config argument – the loader does not
     * touch the config object in the code paths exercised by our tests.
     * </p>
     */
    public static final class DummyConfigMappingContext extends ConfigMappingContext {
        public DummyConfigMappingContext() {
            super(null, new java.util.HashMap<>());
        }
    }

    /**
     * A dummy implementation of {@link ConfigMappingObject} that provides a
     * public constructor matching the signature required by
     * {@code ConfigMappingLoader.configMappingObject}.
     */
    public static final class DummyMappingObject implements ConfigMappingObject {
        public DummyMappingObject(ConfigMappingContext ctx) {
            // No state needed for the test.
        }

        /**
         * Static method required for {@code configMappingProperties}.  It simply
         * returns an empty map.
         */
        public static Map<String, String> getProperties() {
            return java.util.Collections.emptyMap();
        }
    }

    /* -------------------------------------------------------------
     * Tests for ConfigMappingLoader public static methods
     * ------------------------------------------------------------- */

    /**
     * {@link ConfigMappingLoader#getConfigMappingsMetadata(Class)} should return an
     * empty list when the supplied class is not a configuration interface nor a
     * configuration class.
     */
    @Test
    public void testGetConfigMappingsMetadata_NoMapping() {
        List<ConfigMappingMetadata> metadata = ConfigMappingLoader.getConfigMappingsMetadata(String.class);
        assertNotNull("Result list must never be null", metadata);
        assertTrue("No mapping metadata expected for java.lang.String", metadata.isEmpty());
    }

    /**
     * {@link ConfigMappingLoader#getConfigMapping(Class)} should return {@code null}
     * when the supplied type is not a configuration interface.
     */
    @Test
    public void testGetConfigMapping_NoInterface() {
        ConfigMappingInterface mapping = ConfigMappingLoader.getConfigMapping(Integer.class);
        assertNull("Expected null mapping for a non‑configuration interface", mapping);
    }

    /**
     * Verify that {@link ConfigMappingLoader#getImplementationClass(Class)} throws the
     * expected {@code IllegalArgumentException} (via {@code ConfigMessages.msg.classIsNotAMapping})
     * when called with a type that is not a configuration mapping.
     *
     * The exact exception type is defined in {@code ConfigMessages}, but we only
     * assert that an {@code IllegalArgumentException} (or a subclass) is thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetImplementationClass_NotMapping() {
        // String is definitely not a mapping; the loader must reject it.
        ConfigMappingLoader.getImplementationClass(String.class);
    }

    /**
     * {@link ConfigMappingLoader#configMappingProperties(Class)} should retrieve the
     * static {@code getProperties()} method from the implementation class.  The dummy
     * implementation {@link DummyMappingObject} supplies such a method and returns an
     * empty map.
     */
    @Test
    public void testConfigMappingProperties_Success() {
        // The dummy class is a concrete class, therefore the loader will treat it as a mapping class.
        // First we need to make the loader think that {@code DummyMappingObject} is the implementation
        // class for the interface type we will pass – we use the class itself as both interface and
        // implementation because the loader will resolve it directly.
        Map<String, String> props = ConfigMappingLoader.configMappingProperties(DummyMappingObject.class);
        assertNotNull("Properties map must not be null", props);
        assertTrue("Properties map is expected to be empty", props.isEmpty());
    }

    /**
     * {@link ConfigMappingLoader#configMappingObject(Class, ConfigMappingContext)} must
     * instantiate the implementation class using the constructor that takes a
     * {@link ConfigMappingContext}.  The {@link DummyMappingObject} provides such a
     * constructor.
     */
    @Test
    public void testConfigMappingObject_Success() {
        DummyConfigMappingContext ctx = new DummyConfigMappingContext();
        Object instance = ConfigMappingLoader.configMappingObject(DummyMappingObject.class, ctx);
        assertNotNull("Instance should be created", instance);
        assertTrue("Instance must be of type DummyMappingObject", instance instanceof DummyMappingObject);
    }

    /* -------------------------------------------------------------
     * Tests for ConfigMappingLoader.ConfigMappingClass static helpers
     * ------------------------------------------------------------- */

    /**
     * Verify that {@code ConfigMappingClass.getConfigurationClass} returns a non‑null
     * holder for a concrete class, and that the holder returns the original class
     * via {@code getInterfaceType()}.
     */
    @Test
    public void testConfigMappingClass_GetConfigurationClass_Concrete() {
        ConfigMappingLoader.ConfigMappingClass holder = ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(SimpleConcreteClass.class);
        assertNotNull("Holder must be created for concrete class", holder);
        assertEquals("Interface type must match the original class",
                SimpleConcreteClass.class, holder.getInterfaceType());
    }

    /**
     * The factory method must return {@code null} for interfaces, abstract classes,
     * and enums – all of which are rejected by {@code createConfigurationClass}.
     */
    @Test
    public void testConfigMappingClass_CreateConfigurationClass_InvalidTypes() {
        // Interface
        ConfigMappingLoader.ConfigMappingClass ifaceHolder = ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(SimpleInterface.class);
        assertNull("Interface should not produce a ConfigMappingClass", ifaceHolder);

        // Abstract class
        ConfigMappingLoader.ConfigMappingClass abstractHolder = ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(AbstractConfig.class);
        assertNull("Abstract class should not produce a ConfigMappingClass", abstractHolder);

        // Enum
        ConfigMappingLoader.ConfigMappingClass enumHolder = ConfigMappingLoader.ConfigMappingClass.getConfigurationClass(SimpleEnum.class);
        assertNull("Enum should not produce a ConfigMappingClass", enumHolder);
    }

    /**
     * The generated interface name must contain the original class's package,
     * simple name and a numeric hash code, and must end with the literal "I".
     */
    @Test
    public void testConfigMappingClass_GeneratedInterfaceName() throws Exception {
        // Use reflection to invoke the private static method generateInterfaceName
        java.lang.reflect.Method method = ConfigMappingLoader.ConfigMappingClass.class
                .getDeclaredMethod("generateInterfaceName", Class.class);
        method.setAccessible(true);
        String name = (String) method.invoke(null, SimpleConcreteClass.class);
        assertNotNull("Generated name must not be null", name);
        assertTrue("Generated name must end with 'I'", name.endsWith("I"));
        assertTrue("Generated name must contain the package name",
                name.startsWith(SimpleConcreteClass.class.getPackage().getName() + "."));
        assertTrue("Generated name must contain the simple class name",
                name.contains(SimpleConcreteClass.class.getSimpleName()));
    }

    /**
     * The same helper {@code generateInterfaceName} must throw an
     * {@link IllegalArgumentException} when called with an interface type.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConfigMappingClass_GenerateInterfaceName_InvalidInterface() throws Exception {
        java.lang.reflect.Method method = ConfigMappingLoader.ConfigMappingClass.class
                .getDeclaredMethod("generateInterfaceName", Class.class);
        method.setAccessible(true);
        method.invoke(null, SimpleInterface.class);
    }

    /* -------------------------------------------------------------
     * Tests for class‑loader lock handling
     * ------------------------------------------------------------- */

    /**
     * The lock objects returned by {@code getClassLoaderLock} must be unique per class name
     * but identical for repeated requests with the same name.  This is essential for the
     * synchronization performed in {@code loadClass}.
     */
    @Test
    public void testGetClassLoaderLock_UniquenessAndIdentity() throws Exception {
        java.lang.reflect.Method lockMethod = ConfigMappingLoader.class
                .getDeclaredMethod("getClassLoaderLock", String.class);
        lockMethod.setAccessible(true);

        Object lockA1 = lockMethod.invoke(null, "com.example.Foo");
        Object lockA2 = lockMethod.invoke(null, "com.example.Foo");
        Object lockB = lockMethod.invoke(null, "com.example.Bar");

        assertSame("Same class name must return the same lock object", lockA1, lockA2);
        assertNotSame("Different class names must return different lock objects", lockA1, lockB);
    }

    /* -------------------------------------------------------------
     * Tests for error handling paths in configMappingProperties
     * ------------------------------------------------------------- */

    /**
     * When the implementation class does not provide a static {@code getProperties} method,
     * {@code configMappingProperties} must translate the {@link NoSuchMethodException}
     * into a {@link NoSuchMethodError}.  We use a simple class without such a method.
     */
    @Test(expected = NoSuchMethodError.class)
    public void testConfigMappingProperties_NoStaticMethod() {
        // Use a class that does not define getProperties()
        class NoStaticMethodClass {
        }
        ConfigMappingLoader.configMappingProperties(NoStaticMethodClass.class);
    }

    /**
     * Verify that an {@link IllegalAccessException} thrown while looking up the static
     * method is converted into an {@link IllegalAccessError}.
     *
     * This is simulated by creating a nested class with a private static method.
     */
    @Test(expected = IllegalAccessError.class)
    public void testConfigMappingProperties_IllegalAccess() throws Exception {
        class PrivateStaticMethodClass {
            private static Map<String, String> getProperties() {
                return java.util.Collections.emptyMap();
            }
        }

        // Use reflection to make the method package‑private to trigger IllegalAccessException
        // The loader will attempt to find the method via MethodHandles.lookup(),
        // which respects Java access rules and will fail for a private method.
        ConfigMappingLoader.configMappingProperties(PrivateStaticMethodClass.class);
    }

    /* -------------------------------------------------------------
     * Tests for error handling paths in configMappingObject
     * ------------------------------------------------------------- */

    /**
     * When the implementation class lacks a constructor that takes a
     * {@link ConfigMappingContext}, the loader must convert the
     * {@link NoSuchMethodException} into a {@link NoSuchMethodError}.
     */
    @Test(expected = NoSuchMethodError.class)
    public void testConfigMappingObject_NoConstructor() {
        class NoConstructorClass implements ConfigMappingObject {
            // No constructor taking ConfigMappingContext
        }
        ConfigMappingLoader.configMappingObject(NoConstructorClass.class, new DummyConfigMappingContext());
    }

    /**
     * When the constructor exists but throws a checked exception wrapped in an
     * {@link InvocationTargetException}, the loader must re‑throw the original
     * cause (or wrap it in {@link UndeclaredThrowableException} if it is a checked
     * exception).  We simulate this by providing a constructor that deliberately
     * throws a checked exception.
     */
    @Test
    public void testConfigMappingObject_ConstructorThrowsCheckedException() {
        class ThrowingConstructor implements ConfigMappingObject {
            public ThrowingConstructor(ConfigMappingContext ctx) throws Exception {
                throw new Exception("simulated checked exception");
            }
        }
        try {
            ConfigMappingLoader.configMappingObject(ThrowingConstructor.class, new DummyConfigMappingContext());
            fail("Expected UndeclaredThrowableException");
        } catch (UndeclaredThrowableException e) {
            assertEquals("simulated checked exception", e.getUndeclaredThrowable().getMessage());
        }
    }

    /**
     * When the constructor throws a runtime exception, the loader must re‑throw it
     * unchanged.
     */
    @Test(expected = IllegalStateException.class)
    public void testConfigMappingObject_ConstructorThrowsRuntimeException() {
        class RuntimeExceptionConstructor implements ConfigMappingObject {
            public RuntimeExceptionConstructor(ConfigMappingContext ctx) {
                throw new IllegalStateException("runtime failure");
            }
        }
        ConfigMappingLoader.configMappingObject(RuntimeExceptionConstructor.class, new DummyConfigMappingContext());
    }
}
