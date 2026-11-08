package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getToStringMethod_Test {

    /**
     * Simple configuration interface used for the default‑instance test.
     */
    interface SimpleConfig {

        String name();
    }

    interface DummyConfig {

        String dummy();
    }

    /**
     * Helper that creates a {@link ConfigMappingInterface} instance via the
     * package‑private constructor. The constructor signature in the original
     * source is:
     * <pre>
     * ConfigMappingInterface(Class<?> interfaceType,
     *                         ConfigMappingInterface[] superTypes,
     *                         Property[] properties)
     * </pre>
     */
//     private ConfigMappingInterface createMapping(Class<?> iface, ConfigMappingInterface[] superTypes, Property[] properties) throws Exception {
//         Constructor<ConfigMappingInterface> ctor = ConfigMappingInterface.class.getDeclaredConstructor(Class.class, ConfigMappingInterface[].class, ConfigMappingInterface.Property[].class);
//         ctor.setAccessible(true);
//         return ctor.newInstance(iface, superTypes, properties);
//     }

    /**
     * Helper that creates an instance with a specific {@link ToStringMethod}
     * (or {@code null}) by first constructing a normal instance and then
     * overwriting the private {@code toStringMethod} field via reflection.
     */
//     private ConfigMappingInterface createMappingWithToString(Method dummyMethod, ToStringMethod toStringMethod) throws Exception {
//         ConfigMappingInterface mapping = createMapping(DummyConfig.class, ConfigMappingInterface.NO_TYPES, new ConfigMappingInterface.Property[0]);
//         Field f = ConfigMappingInterface.class.getDeclaredField("toStringMethod");
//         f.setAccessible(true);
//         f.set(mapping, toStringMethod);
//         return mapping;
//     }

    @Test
    void getToStringMethod_returnsProvidedToStringMethod() {
        // regular property (not a ToStringMethod)
        ConfigMappingInterface.Property regularProp = new ConfigMappingInterface.Property() {

            @Override
            public boolean isToStringMethod() {
                return false;
            }
        };
        // ToStringMethod implementation
        ConfigMappingInterface.ToStringMethod toStringMethod = new ConfigMappingInterface.ToStringMethod() {

            @Override
            public boolean isToStringMethod() {
                return true;
            }

            @Override
            public String toString() {
                return "provided";
            }
        };
        ConfigMappingInterface[] superTypes = ConfigMappingInterface.NO_TYPES;
        ConfigMappingInterface.Property[] properties = new ConfigMappingInterface.Property[] { regularProp, toStringMethod };
        ConfigMappingInterface mapping = new ConfigMappingInterface(DummyConfig.class, superTypes, properties);
        ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
        assertNotNull(result, "The returned ToStringMethod should not be null");
        assertSame(toStringMethod, result, "The mapping should return the supplied ToStringMethod");
    }

    @Test
    void getToStringMethod_returnsNoneWhenNoToStringMethodPresent() {
        ConfigMappingInterface.Property regularProp1 = new ConfigMappingInterface.Property() {

            @Override
            public boolean isToStringMethod() {
                return false;
            }
        };
        ConfigMappingInterface.Property regularProp2 = new ConfigMappingInterface.Property() {

            @Override
            public boolean isToStringMethod() {
                return false;
            }
        };
        ConfigMappingInterface[] superTypes = ConfigMappingInterface.NO_TYPES;
        ConfigMappingInterface.Property[] properties = new ConfigMappingInterface.Property[] { regularProp1, regularProp2 };
        ConfigMappingInterface mapping = new ConfigMappingInterface(DummyConfig.class, superTypes, properties);
        ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
        assertNotNull(result, "The returned ToStringMethod should never be null");
        assertSame(ConfigMappingInterface.ToStringMethod.NONE, result, "When no @ToString property is present the mapping should return ToStringMethod.NONE");
    }

    @Test
    void getToStringMethod_returnsNullWhenInternalFieldIsNull() throws Exception {
        ConfigMappingInterface mapping = createMappingWithToString(null);
        ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
        assertNull(result, "When the internal field is null, getToStringMethod() must return null");
    }

    @Test
    void getToStringMethod_returnsProvidedConcreteImplementation() throws Exception {
        ConfigMappingInterface.ToStringMethod custom = new ConfigMappingInterface.ToStringMethod() {

            @Override
            public boolean isToStringMethod() {
                return true;
            }

            @Override
            public String toString() {
                return "custom";
            }
        };
        ConfigMappingInterface mapping = createMappingWithToString(custom);
        ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
        assertSame(custom, result, "The same concrete ToStringMethod instance should be returned");
        assertEquals("custom", result.toString(), "The toString() of the returned instance must match");
    }

//     @Test
//     void getToStringMethod_returnsMockedInstance(@Mock ToStringMethod mockToString) throws Exception {
//         // manual mock implementation
//         ConfigMappingInterface.ToStringMethod mockToString = new ConfigMappingInterface.ToStringMethod() {
// 
//             @Override
//             public boolean isToStringMethod() {
//                 return true;
//             }
// 
//             @Override
//             public String toString() {
//                 return "mocked";
//             }
//         };
//         ConfigMappingInterface mapping = createMappingWithToString(mockToString);
//         ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
//         assertSame(mockToString, result, "The getter must return the exact mock supplied at construction");
//     }

    @Test
    void getToStringMethod_multipleCallsReturnSameInstance() throws Exception {
        ConfigMappingInterface.ToStringMethod lambda = () -> "multi";
        ConfigMappingInterface mapping = createMappingWithToString(lambda);
        ConfigMappingInterface.ToStringMethod first = mapping.getToStringMethod();
        ConfigMappingInterface.ToStringMethod second = mapping.getToStringMethod();
        ConfigMappingInterface.ToStringMethod third = mapping.getToStringMethod();
        assertAll(() -> assertSame(first, second, "Second call should return the same instance as first"), () -> assertSame(first, third, "Third call should also return the same instance"));
    }

    @Test
    void getToStringMethod_defaultInstanceIsNotNull() {
        ConfigMappingInterface mapping = ConfigMappingInterface.getConfigurationInterface(SimpleConfig.class);
        ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
        assertNotNull(result, "A default ConfigMappingInterface must contain a non‑null ToStringMethod");
        assertTrue(result instanceof ConfigMappingInterface.ToStringMethod, "Result must implement ToStringMethod");
    }

    @Test
    void getToStringMethod_concurrentAccessReturnsIdenticalReference() throws Exception {
        ConfigMappingInterface.ToStringMethod mock = new ConfigMappingInterface.ToStringMethod() {

            @Override
            public boolean isToStringMethod() {
                return true;
            }

            @Override
            public String toString() {
                return "mock";
            }
        };
        ConfigMappingInterface mapping = createMappingWithToString(mock);
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Callable<ConfigMappingInterface.ToStringMethod> task = mapping::getToStringMethod;
        Set<ConfigMappingInterface.ToStringMethod> results = ConcurrentHashMap.newKeySet();
        try {
            for (Future<ConfigMappingInterface.ToStringMethod> f : executor.invokeAll(Collections.nCopies(threadCount, task))) {
                results.add(f.get());
            }
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, results.size(), "All threads must obtain the identical ToStringMethod reference");
        assertSame(mock, results.iterator().next(), "The reference obtained concurrently must be the original mock");
    }

    @Test
    void getToStringMethod_lambdaImplementation() throws Exception {
        ConfigMappingInterface.ToStringMethod lambda = () -> "lambda-" + System.currentTimeMillis();
        ConfigMappingInterface mapping = createMappingWithToString(lambda);
        ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
        String value = result.toString();
        assertSame(lambda, result, "The lambda instance should be returned unchanged");
        assertTrue(value.startsWith("lambda-"), "The lambda's toString() must start with the expected prefix");
    }
}