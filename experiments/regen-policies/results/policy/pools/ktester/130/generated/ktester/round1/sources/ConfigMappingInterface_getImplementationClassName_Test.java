import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getImplementationClassName_Test {

    static class StaticInner {
    }

    /**
     * Simple user‑defined class for the “user defined class” test.
     */
    static class DummyClass {
    }

    /**
     * Helper outer class that contains a non‑static inner class.
     */
    static class ConfigMappingInterfaceTestHelper {

        class Inner {
        }
    }

    class Inner {
    }

    class Inner {
    }

    /**
     * Supplies the data for {@link #getImplementationClassName_parameterised(Class, String)}.
     */
    static Stream<Arguments> provideTypesForImplementationName() {
        // Helper instance for non‑static inner class test
        ConfigMappingInterfaceTestHelper outer = new ConfigMappingInterfaceTestHelper();
        return // Standard top‑level class
        // Nested static inner class (Map.Entry)
        // Primitive array
        // Multi‑dimensional object array
        // Non‑static member inner class
        Stream.of(Arguments.of(String.class, "java.lang.String$$CMImpl"), Arguments.of(Map.Entry.class, "java.util.Map$Entry$$CMImpl"), Arguments.of(int[].class, "[I$$CMImpl"), Arguments.of(String[][].class, "[[Ljava.lang.String;$$CMImpl"), Arguments.of(outer.new Inner().getClass(), "io.smallrye.config.ConfigMappingInterface_getImplementationClassName_Test$ConfigMappingInterfaceTestHelper$Inner$$CMImpl"));
    }

    @Test
    void getImplementationClassName_simpleClass() {
        // GIVEN
        Class<?> type = ConfigMappingInterface.class;
        // WHEN
        String implName = ConfigMappingInterface.getImplementationClassName(type);
        // THEN
        assertEquals("io.smallrye.config.ConfigMappingInterface$$CMImpl", implName);
    }

    @Test
    void getImplementationClassName_userDefinedClass() {
        // GIVEN
        Class<?> type = ConfigMappingInterface_getImplementationClassName_Test.DummyClass.class;
        // WHEN
        String implName = ConfigMappingInterface.getImplementationClassName(type);
        // THEN
        assertEquals("io.smallrye.config.ConfigMappingInterface_getImplementationClassName_Test$DummyClass$$CMImpl", implName);
    }

    @Test
    void getImplementationClassName_staticInnerClass() {
        // GIVEN
        Class<?> type = ConfigMappingInterface_getImplementationClassName_Test.StaticInner.class;
        // WHEN
        String implName = ConfigMappingInterface.getImplementationClassName(type);
        // THEN
        assertEquals("io.smallrye.config.ConfigMappingInterface_getImplementationClassName_Test$StaticInner$$CMImpl", implName);
    }

    @Test
    void getImplementationClassName_anonymousClass() {
        // GIVEN
        Object anonymous = new Object() {
        };
        Class<?> type = anonymous.getClass();
        // WHEN
        String implName = ConfigMappingInterface.getImplementationClassName(type);
        // THEN
        // Anonymous class names contain a "$" followed by a number (e.g. ...$1)
        assertTrue(implName.matches("io\\.smallrye\\.config\\.ConfigMappingInterface_getImplementationClassName_Test\\$\\d+\\$\\$CMImpl"));
    }

    @Test
    void getImplementationClassName_primitiveType() {
        // GIVEN
        Class<?> type = int.class;
        // WHEN
        String implName = ConfigMappingInterface.getImplementationClassName(type);
        // THEN
        assertEquals("int$$CMImpl", implName);
    }

    @Test
    void getImplementationClassName_arrayType() {
        // GIVEN
        Class<?> type = String[].class;
        // WHEN
        String implName = ConfigMappingInterface.getImplementationClassName(type);
        // THEN
        // Array class names are like "[Ljava.lang.String;"
        assertEquals("[Ljava.lang.String;$$CMImpl", implName);
    }

    @Test
    void getImplementationClassName_nullArgument() {
        // The method dereferences the argument (type.getName()), therefore a NPE is expected.
        assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getImplementationClassName(null));
    }

    @ParameterizedTest(name = "{index}: type={0}")
    @MethodSource("provideTypesForImplementationName")
    void getImplementationClassName_parameterised(Class<?> type, String expected) {
        // WHEN
        String implName = ConfigMappingInterface.getImplementationClassName(type);
        // THEN
        assertEquals(expected, implName);
    }
}
