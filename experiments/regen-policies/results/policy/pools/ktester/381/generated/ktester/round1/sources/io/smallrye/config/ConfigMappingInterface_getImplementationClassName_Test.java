import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.mockito.Mockito;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getImplementationClassName_Test {

    /**
     * Simple top‑level class used as a test fixture.
     */
    static final class SampleClass {
    }

    /**
     * Container for a nested static class used as a test fixture.
     */
    static final class OuterClass {

        static final class NestedStatic {
        }
    }

    static final class NestedStatic {
    }

//     static final class NestedStatic {
    }

    /**
     * Dummy interface to emulate a user‑defined interface.
     */
    interface MyInterface {
    }

    /**
     * Static nested class used for the edge‑case test.
     */
    static final class LocalClass {
    }

    static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of("Regular top‑level class (java.lang.String)", String.class, "java.lang.String$$CMImpl"), Arguments.of("Inner interface (java.util.Map.Entry)", Map.Entry.class, "java.util.Map$Entry$$CMImpl"), Arguments.of("User‑defined interface (com.example.MyInterface)", ConfigMappingInterface_getImplementationClassName_Test.MyInterface.class, "io.smallrye.config.ConfigMappingInterface_getImplementationClassName_Test$MyInterface$$CMImpl"));
    }

    static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(Arguments.of("Primitive type (int)", int.class, "int$$CMImpl"), Arguments.of("Array type (String[])", String[].class, "[Ljava.lang.String;$$CMImpl"), Arguments.of("Enum inner class (Thread.State)", Thread.State.class, "java.lang.Thread$State$$CMImpl"), Arguments.of("Static nested class defined in this test class", ConfigMappingInterface_getImplementationClassName_Test.LocalClass.class, "io.smallrye.config.ConfigMappingInterface_getImplementationClassName_Test$LocalClass$$CMImpl"));
    }

//     @Test
//     void getImplementationClassName_returnsExpectedSuffixForTopLevelClass() {
//         // GIVEN
//         Class<?> type = ConfigMappingInterface_getImplementationClassName_Test.SampleClass.class;
//         // WHEN
//         String implName = ConfigMappingInterface.getImplementationClassName(type);
//         // THEN
//         assertNotNull(implName, "Implementation class name must not be null");
//         assertEquals(ConfigMappingInterface_getImplementationClassName_Test.SampleClass.class.getName() + "$$CMImpl", implName, "Implementation class name must be the interface name plus $$CMImpl");
//         assertFalse(implName.contains(" "), "Implementation class name should not contain whitespace");
//     }

//     @Test
//     void getImplementationClassName_returnsExpectedSuffixForNestedStaticClass() {
//         // GIVEN
//         Class<?> type = ConfigMappingInterface_getImplementationClassName_Test.OuterClass.NestedStatic.class;
//         // WHEN
//         String implName = ConfigMappingInterface.getImplementationClassName(type);
//         // THEN
//         assertNotNull(implName);
//         assertEquals(ConfigMappingInterface_getImplementationClassName_Test.OuterClass.NestedStatic.class.getName() + "$$CMImpl", implName);
//     }

//     @Test
//     void getImplementationClassName_handlesAnonymousClassGracefully() {
//         // GIVEN
//         Object anonymous = new Object() {
//         };
//         Class<?> type = anonymous.getClass();
//         // WHEN
//         String implName = ConfigMappingInterface.getImplementationClassName(type);
//         // THEN
//         // The name of an anonymous class contains a "$" followed by a number.
//         // We only assert that the suffix is still correctly appended.
//         assertTrue(implName.endsWith("$$CMImpl"), "Even for anonymous classes the $$CMImpl suffix must be present");
//         assertTrue(implName.startsWith(type.getName()), "The implementation name should start with the original class name");
//     }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("basicFunctionalityProvider")
//     void getImplementationClassName_basicFunctionality(String description, Class<?> type, String expected) {
//         // WHEN
//         String implName = ConfigMappingInterface.getImplementationClassName(type);
//         // THEN
//         assertEquals(expected, implName, description);
//     }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("edgeCasesProvider")
//     void getImplementationClassName_edgeCases(String description, Class<?> type, String expected) {
//         // WHEN
//         String implName = ConfigMappingInterface.getImplementationClassName(type);
//         // THEN
//         assertEquals(expected, implName, description);
//     }

//     @Test
//     void getImplementationClassName_nullArgument_throwsNullPointerException() {
//         assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getImplementationClassName(null), "Calling with null should throw NullPointerException");
//     }

//     @Test
//     void getImplementationClassName_securityManagerDeniesAccess_throwsSecurityException() {
//         // Create a mock Class that throws SecurityException when getName() is invoked.
//         @SuppressWarnings("unchecked")
//         Class<Object> mockedClass = (Class<Object>) Mockito.mock(Class.class);
//         Mockito.when(mockedClass.getName()).thenThrow(new SecurityException("Access denied"));
//         // The method under test directly calls type.getName(); the mock will trigger the exception.
//         assertThrows(SecurityException.class, () -> ConfigMappingInterface.getImplementationClassName(mockedClass), "SecurityException should be propagated unchanged");
//     }
}