package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_getRootsMap_Test {

    /**
     * The context under test.
     */
    private ConfigMappingContext context;

    /**
     * Mocked configuration used only for constructing the context.
     */
    @Mock
    private SmallRyeConfig config;

    /**
     * Roots map supplied to the constructor – can be empty or contain dummy entries.
     */
    private Map<String, List<Class<?>>> roots;

    /**
     * Names map supplied to the constructor – not used by `getRootsMap()` directly.
     */
    private Map<String, Map<String, Set<String>>> names;

    /**
     * Simple dummy root interface used in one of the tests.
     */
    interface DummyRoot {
    }

    class SimpleLoader extends ClassLoader {

        private final String targetName;

        SimpleLoader(ClassLoader parent, String targetName) {
            super(parent);
            this.targetName = targetName;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (!name.equals(targetName)) {
                return super.findClass(name);
            }
            // Load the class bytes via parent and define a new class instance.
            String resource = name.replace('.', '/') + ".class";
            try (java.io.InputStream is = parent.getResourceAsStream(resource)) {
                if (is == null)
                    throw new ClassNotFoundException(name);
                byte[] bytes = is.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (java.io.IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }

    class SimpleLoader extends ClassLoader {

        private final String targetName;

        SimpleLoader(ClassLoader parent, String targetName) {
            super(parent);
            this.targetName = targetName;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (!name.equals(targetName)) {
                return super.findClass(name);
            }
            // Load the class bytes via parent and define a new class instance.
            String resource = name.replace('.', '/') + ".class";
            try (java.io.InputStream is = parent.getResourceAsStream(resource)) {
                if (is == null)
                    throw new ClassNotFoundException(name);
                byte[] bytes = is.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (java.io.IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Start each test with fresh, empty constructor arguments.
        roots = new HashMap<>();
        names = new HashMap<>();
        // `config` is already a Mockito mock thanks to the @Mock annotation.
        // The constructor of ConfigMappingContext does not invoke any method on the config
        // during the execution of `getRootsMap()`, so a simple mock is sufficient.
        context = new ConfigMappingContext(config, roots, names);
    }

    @Test
    void getRootsMap_ShouldReturnEmptyMap_WhenNoRootsProvided() {
        // Given – the context has been created with an empty `roots` argument (set up in @BeforeEach).
        // When
        Map<Class<?>, Map<String, ConfigMappingObject>> result = context.getRootsMap();
        // Then
        assertNotNull(result, "The returned map must never be null");
        assertTrue(result.isEmpty(), "When no roots are registered the map should be empty");
    }

    @Test
    void getRootsMap_ShouldContainRegisteredRoot_WhenRootIsProvidedInConstructor() {
        // Given – a root class registered via the constructor argument.
        String path = "my.path";
        roots.put(path, Collections.singletonList(DummyRoot.class));
        // Re‑create the context so that the constructor processes the new `roots` map.
        context = new ConfigMappingContext(config, roots, names);
        // When
        Map<Class<?>, Map<String, ConfigMappingObject>> result = context.getRootsMap();
        // Then
        assertNotNull(result, "The returned map must never be null");
        assertEquals(1, result.size(), "Exactly one root class should be present");
        assertTrue(result.containsKey(DummyRoot.class), "The map should contain the DummyRoot class as a key");
        // The inner map is populated by `registerRoot` (implementation not visible here);
        // we only assert that it is non‑null – the exact contents are outside the scope of this unit test.
        Map<String, ConfigMappingObject> inner = result.get(DummyRoot.class);
        assertNotNull(inner, "The inner map for the registered root must be non‑null");
    }

    @Test
    void getRootsMap_ShouldReturnIdentityHashMap_OnNewContext() {
        // InitialState: verify concrete type and emptiness.
        Map<Class<?>, Map<String, ConfigMappingObject>> map = context.getRootsMap();
        assertNotNull(map, "Map must not be null");
        assertTrue(map.isEmpty(), "New context should have an empty map");
        assertTrue(map instanceof IdentityHashMap, "The internal map must be an IdentityHashMap to guarantee identity semantics");
    }

    @Test
    void getRootsMap_ShouldReturnSameInstance_OnSuccessiveCalls() {
        // MapReferenceEquality
        Map<Class<?>, Map<String, ConfigMappingObject>> first = context.getRootsMap();
        Map<Class<?>, Map<String, ConfigMappingObject>> second = context.getRootsMap();
        assertSame(first, second, "Successive calls must return the same map instance");
    }

    @Test
    void getRootsMap_ShouldReflectMutations_InsertedEntry() {
        // MapMutability
        Map<Class<?>, Map<String, ConfigMappingObject>> map = context.getRootsMap();
        map.put(String.class, new HashMap<>());
        // Retrieve again to ensure we see the same underlying map
        Map<Class<?>, Map<String, ConfigMappingObject>> again = context.getRootsMap();
        assertEquals(1, again.size(), "Map size must be 1 after insertion");
        assertTrue(again.containsKey(String.class), "Map must contain the inserted key");
    }

    @Test
    void getRootsMap_ShouldThrowNPE_WhenNullKeyInserted() {
        // NullKeyNotAllowed
        Map<Class<?>, Map<String, ConfigMappingObject>> map = context.getRootsMap();
        assertThrows(NullPointerException.class, () -> map.put(null, new HashMap<>()), "IdentityHashMap does not allow null keys and must throw NPE");
    }

    @Test
    void getRootsMap_ShouldAllowNullValue() {
        // NullValueAllowed
        Map<Class<?>, Map<String, ConfigMappingObject>> map = context.getRootsMap();
        map.put(String.class, null);
        assertEquals(1, map.size(), "Map size should be 1 after inserting null value");
        assertNull(map.get(String.class), "Retrieving the value for the key must return null");
    }

    @Test
    void getRootsMap_ShouldOverwriteExistingKey() {
        // OverwriteExistingKey
        Map<Class<?>, Map<String, ConfigMappingObject>> map = context.getRootsMap();
        Map<String, ConfigMappingObject> firstValue = new HashMap<>();
        Map<String, ConfigMappingObject> secondValue = new HashMap<>();
        map.put(Integer.class, firstValue);
        // overwrite
        map.put(Integer.class, secondValue);
        assertEquals(1, map.size(), "Map size must stay 1 after overwriting the same key");
        assertSame(secondValue, map.get(Integer.class), "The value must be the second one inserted");
    }

    @Test
    void getRootsMap_ShouldBeClearable() {
        // ClearMap
        Map<Class<?>, Map<String, ConfigMappingObject>> map = context.getRootsMap();
        map.put(String.class, new HashMap<>());
        map.clear();
        assertTrue(map.isEmpty(), "Map must be empty after clear()");
        assertEquals(0, map.size(), "Size must be 0 after clear()");
    }

    @Test
    void getRootsMap_ShouldDistinguishClassesFromDifferentLoaders() throws Exception {
        // IdentityEqualityWithDifferentClassLoaders
        // Create two class loaders that each load the same class name.
        ClassLoader parent = ConfigMappingContext_getRootsMap_Test.class.getClassLoader();
        // Simple byte‑code loader that delegates to parent for everything except the target class.
        class SimpleLoader extends ClassLoader {

            private final String targetName;

            SimpleLoader(ClassLoader parent, String targetName) {
                super(parent);
                this.targetName = targetName;
            }

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (!name.equals(targetName)) {
                    return super.findClass(name);
                }
                // Load the class bytes via parent and define a new class instance.
                String resource = name.replace('.', '/') + ".class";
                try (java.io.InputStream is = parent.getResourceAsStream(resource)) {
                    if (is == null)
                        throw new ClassNotFoundException(name);
                    byte[] bytes = is.readAllBytes();
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (java.io.IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
        }
        String dummyClassName = DummyRoot.class.getName();
        ClassLoader loaderA = new SimpleLoader(parent, dummyClassName);
        ClassLoader loaderB = new SimpleLoader(parent, dummyClassName);
        Class<?> classFromLoaderA = Class.forName(dummyClassName, true, loaderA);
        Class<?> classFromLoaderB = Class.forName(dummyClassName, true, loaderB);
        assertNotSame(classFromLoaderA, classFromLoaderB, "Classes loaded by different loaders must be distinct");
        Map<Class<?>, Map<String, ConfigMappingObject>> map = context.getRootsMap();
        map.put(classFromLoaderA, new HashMap<>());
        assertEquals(1, map.size(), "Map size must be 1 after inserting key from loader A");
        assertTrue(map.containsKey(classFromLoaderA), "Map must contain the key from loader A");
        assertFalse(map.containsKey(classFromLoaderB), "Map must NOT contain the key from loader B because identity semantics are used");
    }

    @Test
    void getRootsMap_ShouldAllowRemovalOfEntry() {
        // RemoveEntry
        Map<Class<?>, Map<String, ConfigMappingObject>> map = context.getRootsMap();
        map.put(String.class, new HashMap<>());
        map.remove(String.class);
        assertTrue(map.isEmpty(), "Map must be empty after removal of the only entry");
        assertNull(map.get(String.class), "Retrieving a removed key must return null");
    }
}
