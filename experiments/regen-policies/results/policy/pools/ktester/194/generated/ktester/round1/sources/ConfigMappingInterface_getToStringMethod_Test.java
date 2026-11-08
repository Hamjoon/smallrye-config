import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getToStringMethod_Test {

    class MyToStringMethod extends ConfigMappingInterface.ToStringMethod {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean isToStringMethod() {
            return true;
        }
    }

    /**
     * Simple dummy interface used only for constructing {@link ConfigMappingInterface} instances.
     */
    interface DummyInterface {
    }

    class MyToStringMethod extends ConfigMappingInterface.ToStringMethod {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean isToStringMethod() {
            return true;
        }
    }

    /**
     * Creates a {@link ConfigMappingInterface} instance whose {@code toStringMethod}
     * field is set to the supplied value using reflection.
     *
     * @param toStringMethod
     * 		the method to inject (may be {@code null})
     * @return a fully‑initialised {@link ConfigMappingInterface}
     */
    private io.smallrye.config.ConfigMappingInterface createWithToStringMethod(ConfigMappingInterface.ToStringMethod toStringMethod) throws Exception {
        // use the public constructor that accepts (Class<?>, ConfigMappingInterface.Type[], ConfigMappingInterface.Property[])
        ConfigMappingInterface mapping = new ConfigMappingInterface(ConfigMappingInterface_getToStringMethod_Test.DummyInterface.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
        // inject the desired value into the private final field
        Field f = ConfigMappingInterface.class.getDeclaredField("toStringMethod");
        f.setAccessible(true);
        f.set(mapping, toStringMethod);
        return mapping;
    }

    @Test
    void getToStringMethod_returnsProvidedToStringMethod() {
        // given – a Property array that contains a ToStringMethod
        ConfigMappingInterface.ToStringMethod toStringMethodMock = mock(ConfigMappingInterface.ToStringMethod.class);
        when(toStringMethodMock.isToStringMethod()).thenReturn(true);
        ConfigMappingInterface.Property[] properties = new ConfigMappingInterface.Property[] { toStringMethodMock };
        // when – the ConfigMappingInterface is created
        ConfigMappingInterface mapping = new ConfigMappingInterface(ConfigMappingInterface_getToStringMethod_Test.DummyInterface.class, ConfigMappingInterface.NO_TYPES, properties);
        // then – getToStringMethod() returns the instance we supplied
        assertSame(toStringMethodMock, mapping.getToStringMethod(), "The toStringMethod returned by getToStringMethod() should be the one supplied in the constructor");
    }

    @Test
    void getToStringMethod_returnsNoneWhenNoToStringMethodPresent() {
        // given – a Property array without any ToStringMethod
        ConfigMappingInterface.Property propertyMock = mock(ConfigMappingInterface.Property.class);
        when(propertyMock.isToStringMethod()).thenReturn(false);
        ConfigMappingInterface.Property[] properties = new ConfigMappingInterface.Property[] { propertyMock };
        // when – the ConfigMappingInterface is created
        ConfigMappingInterface mapping = new ConfigMappingInterface(ConfigMappingInterface_getToStringMethod_Test.DummyInterface.class, ConfigMappingInterface.NO_TYPES, properties);
        // then – getToStringMethod() returns the default NONE instance
        assertSame(ConfigMappingInterface.ToStringMethod.NONE, mapping.getToStringMethod(), "When no ToStringMethod is supplied the default NONE should be returned");
    }

    @Test
    void getToStringMethod_returnsNullWhenFieldIsNull() throws Exception {
        // given – a ConfigMappingInterface whose internal field is forced to null
        ConfigMappingInterface mapping = createWithToStringMethod(null);
        // when – getToStringMethod() is invoked
        ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
        // then – the method returns null
        assertNull(result, "When the internal toStringMethod field is null, getToStringMethod() should return null");
    }

    @Test
    void getToStringMethod_distinctInstancesPerConfigMappingInterface() throws Exception {
        // given – two separate ConfigMappingInterface objects each with its own ToStringMethod
        ConfigMappingInterface.ToStringMethod methodA = mock(ConfigMappingInterface.ToStringMethod.class);
        ConfigMappingInterface.ToStringMethod methodB = mock(ConfigMappingInterface.ToStringMethod.class);
        ConfigMappingInterface mappingA = createWithToStringMethod(methodA);
        ConfigMappingInterface mappingB = createWithToStringMethod(methodB);
        // then – the returned instances are not the same reference
        assertNotSame(methodA, mappingB.getToStringMethod(), "Mapping B must not return Mapping A's ToStringMethod instance");
        assertNotSame(methodB, mappingA.getToStringMethod(), "Mapping A must not return Mapping B's ToStringMethod instance");
    }

    @Test
    void getToStringMethod_isIdempotent() throws Exception {
        // given – a ConfigMappingInterface with a known ToStringMethod
        ConfigMappingInterface.ToStringMethod method = mock(ConfigMappingInterface.ToStringMethod.class);
        ConfigMappingInterface mapping = createWithToStringMethod(method);
        // when – getToStringMethod() is called repeatedly
        ConfigMappingInterface.ToStringMethod first = mapping.getToStringMethod();
        ConfigMappingInterface.ToStringMethod second = mapping.getToStringMethod();
        ConfigMappingInterface.ToStringMethod third = mapping.getToStringMethod();
        // then – all calls return the identical object reference
        assertSame(first, second, "Second call should return the same instance as the first");
        assertSame(second, third, "Third call should return the same instance as the second");
    }

    @Test
    void getToStringMethod_collectionRetrievalConsistency() throws Exception {
        // given – a ConfigMappingInterface stored inside a collection
        ConfigMappingInterface.ToStringMethod method = mock(ConfigMappingInterface.ToStringMethod.class);
        ConfigMappingInterface mapping = createWithToStringMethod(method);
        List<ConfigMappingInterface> list = new ArrayList<>();
        list.add(mapping);
        // when – the object is retrieved from the collection
        ConfigMappingInterface retrieved = list.get(0);
        ConfigMappingInterface.ToStringMethod retrievedMethod = retrieved.getToStringMethod();
        // then – the method instance matches the original one
        assertSame(method, retrievedMethod, "The ToStringMethod obtained after collection round‑trip must be the original instance");
    }

    @Test
    void getToStringMethod_afterSerialization() throws Exception {
        // given – a serializable ConfigMappingInterface with a concrete ToStringMethod
        ConfigMappingInterface.ToStringMethod method = mock(ConfigMappingInterface.ToStringMethod.class);
        ConfigMappingInterface original = createWithToStringMethod(method);
        // when – the instance is serialized and then deserialized
        byte[] serialized;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(original);
            out.flush();
            serialized = bos.toByteArray();
        }
        ConfigMappingInterface deserialized;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
            deserialized = (ConfigMappingInterface) in.readObject();
        }
        // then – the deserialized object returns the same ToStringMethod reference
        assertSame(method, deserialized.getToStringMethod(), "After Java serialization the ToStringMethod reference should be preserved");
    }

    @Test
    void getToStringMethod_customSubclassReturnedUnchanged() throws Exception {
        // given – a user‑defined subclass of ToStringMethod
        class MyToStringMethod extends ConfigMappingInterface.ToStringMethod {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isToStringMethod() {
                return true;
            }
        }
        MyToStringMethod custom = new MyToStringMethod();
        ConfigMappingInterface mapping = createWithToStringMethod(custom);
        // when – getToStringMethod() is invoked
        ConfigMappingInterface.ToStringMethod result = mapping.getToStringMethod();
        // then – the exact subclass instance is returned
        assertSame(custom, result, "The custom ToStringMethod subclass instance must be returned unchanged");
        assertTrue(result instanceof MyToStringMethod, "Returned instance should be of the custom subclass type");
    }

    @Test
    void getToStringMethod_concurrentAccessReturnsSameInstance() throws Exception {
        // given – a shared ConfigMappingInterface instance
        ConfigMappingInterface.ToStringMethod method = mock(ConfigMappingInterface.ToStringMethod.class);
        ConfigMappingInterface mapping = createWithToStringMethod(method);
        // when – many threads call getToStringMethod() concurrently
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<ConfigMappingInterface.ToStringMethod>> futures = IntStream.range(0, threadCount).mapToObj(i -> executor.submit(mapping::getToStringMethod)).collect(Collectors.toList());
            // then – every future yields the same reference and no exception occurs
            for (Future<ConfigMappingInterface.ToStringMethod> f : futures) {
                assertSame(method, f.get(), "All concurrent calls must return the identical ToStringMethod instance");
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
