import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
// import org.mockito.junit.jupiter.MockitoExtension;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingInterface_getImplementationClassName_Test {

    class Outer {

        class Inner {
        }
    }

    class Inner {
    }

//     class Outer {

        class Inner {
        }
    }

//     class Inner {
    }

//     class Inner {
    }

    // Used by the nested‑class test
    private static Stream<Arguments> standardClassNamesProvider() {
        // java.lang.String
        // java.util.Map.Entry (static nested interface)
        // java.util.ArrayList
        return Stream.of(Arguments.of(String.class, String.class.getName() + "$$CMImpl"), Arguments.of(Map.Entry.class, Map.Entry.class.getName() + "$$CMImpl"), Arguments.of(ArrayList.class, ArrayList.class.getName() + "$$CMImpl"));
    }

    private static Stream<Arguments> edgeClassNamesProvider() {
        // java.lang.Integer[] – array type
        // java.util.Map$Entry – binary name with '$' (inner class)
        // void (primitive type)
        // java.lang.String[] – array of String
        return Stream.of(Arguments.of(Integer[].class, Integer[].class.getName() + "$$CMImpl"), Arguments.of(Map.Entry.class, Map.Entry.class.getName() + "$$CMImpl"), Arguments.of(void.class, void.class.getName() + "$$CMImpl"), Arguments.of(String[].class, String[].class.getName() + "$$CMImpl"));
    }

//     @Test
//     void shouldAppendSuffixToSimpleClassName() {
//         // GIVEN
//         Class<?> type = String.class;
//         String expected = String.class.getName() + "$$CMImpl";
//         // WHEN
//         String actual = ConfigMappingInterface.getImplementationClassName(type);
//         // THEN
//         assertEquals(expected, actual, "The implementation class name should be the original name plus $$CMImpl");
//     }

//     @Test
//     void shouldAppendSuffixToNestedClassName() {
//         // GIVEN
//         class Outer {
// 
//             class Inner {
//             }
//         }
//         Class<?> type = Outer.Inner.class;
//         String expected = type.getName() + "$$CMImpl";
//         // WHEN
//         String actual = ConfigMappingInterface.getImplementationClassName(type);
//         // THEN
//         assertEquals(expected, actual);
//     }

//     @Test
//     void shouldAppendSuffixToArrayClassName() {
//         // GIVEN
//         // name will be "[I"
//         Class<?> type = int[].class;
//         String expected = type.getName() + "$$CMImpl";
//         // WHEN
//         String actual = ConfigMappingInterface.getImplementationClassName(type);
//         // THEN
//         assertEquals(expected, actual);
//     }

//     @Test
//     void shouldAppendSuffixToPrimitiveClassName() {
//         // GIVEN
//         // name will be "boolean"
//         Class<?> type = boolean.class;
//         String expected = type.getName() + "$$CMImpl";
//         // WHEN
//         String actual = ConfigMappingInterface.getImplementationClassName(type);
//         // THEN
//         assertEquals(expected, actual);
//     }

//     @Test
//     void shouldThrowNpeWhenTypeIsNull() {
//         // GIVEN
//         Class<?> type = null;
//         // WHEN / THEN
//         assertThrows(NullPointerException.class, () -> ConfigMappingInterface.getImplementationClassName(type), "Calling with null should result in NPE");
//     }

//     @Test
//     void implementationClassNameShouldNeverBeEqualToOriginalClassName() {
//         // GIVEN
//         Class<?> type = LocalDateTime.class;
//         // WHEN
//         String implName = ConfigMappingInterface.getImplementationClassName(type);
//         // THEN
//         assertNotEquals(type.getName(), implName, "Implementation name must differ from the original class name");
//         assertTrue(implName.endsWith("$$CMImpl"), "Implementation name must end with the expected suffix");
//     }

    /**
     * Standard class names – top‑level and static nested types.
     */
//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("standardClassNamesProvider")
//     void shouldGenerateImplementationClassName_ForStandardClasses(Class<?> type, String expected) {
//         // WHEN
//         String actual = ConfigMappingInterface.getImplementationClassName(type);
//         // THEN
//         assertEquals(expected, actual, () -> "Implementation name for " + type + " should be " + expected);
//     }

    /**
     * Edge and special class names – arrays, inner classes (binary name with '$'), primitives.
     */
//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("edgeClassNamesProvider")
//     void shouldGenerateImplementationClassName_ForEdgeCases(Class<?> type, String expected) {
//         // WHEN
//         String actual = ConfigMappingInterface.getImplementationClassName(type);
//         // THEN
//         assertEquals(expected, actual, () -> "Implementation name for " + type + " should be " + expected);
//     }
}