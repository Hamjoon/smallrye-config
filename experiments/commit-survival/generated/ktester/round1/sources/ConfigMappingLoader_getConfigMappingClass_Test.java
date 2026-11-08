package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.*;

@ExtendWith(MockitoExtension.class)
class ConfigMappingLoader_getConfigMappingClass_Test {

    /**
     * Simple interface without any mapping annotation.
     */
    interface PlainInterface {

        String value();
    }

    /**
     * Valid configuration‑mapping interface used in the positive test.
     */
    @ConfigMapping(prefix = "server")
    interface Server {

        String host();

        int port();
    }

    /**
     * A class incorrectly annotated with @ConfigProperties – should be rejected.
     */
    @ConfigProperties(prefix = "server")
    static class ServerProperties {

        String host;

        int port;
    }

    /**
     * Simple interface without any mapping annotation – used for the “null metadata” case.
     */
    interface SimpleInterface {

        String value();
    }

    /**
     * Interface that pretends to be a configuration‑mapping type – used for the “non‑null metadata” case.
     */
    @ConfigMapping(prefix = "cfg")
    interface ConfigInterface {

        String name();
    }

    /**
     * Another dummy interface – used for the “non‑null metadata” case with a different expected name.
     */
    @ConfigMapping(prefix = "another")
    interface AnotherConfig {

        int size();
    }

    /**
     * Dummy class that will cause {@code loadClass} to throw an {@link UndeclaredThrowableException}.
     */
    @ConfigMapping(prefix = "faulty")
    interface FaultyConfig {

        String data();
    }

    /**
     * A concrete class (not an interface) – used to provoke an IllegalArgumentException from the metadata lookup.
     */
    static class NotAnInterface {
        // empty
    }

    /**
     * Dummy class used as the “generated” implementation for {@link ConfigInterface}.
     */
    static class ConfigInterfaceImpl implements ConfigMappingObject {
        // empty – only the type matters for the assertion
    }

    /**
     * Dummy class used as the “generated” implementation for {@link AnotherConfig}.
     */
    static class AnotherConfigGenerated implements ConfigMappingObject {
        // empty – only the type matters for the assertion
    }

    static Stream<Arguments> nullMetadataProvider() {
        return Stream.of(// JDK class – also has no metadata
        Arguments.of(SimpleInterface.class), Arguments.of(String.class));
    }

    static Stream<Arguments> nonNullMetadataProvider() {
        return Stream.of(Arguments.of(ConfigInterface.class, ConfigInterfaceImpl.class), Arguments.of(AnotherConfig.class, AnotherConfigGenerated.class));
    }

    /**
     * When the supplied type is *not* a {@code @ConfigMapping} interface,
     * the method must return the original class unchanged.
     */
    @Test
    void getConfigMappingClass_returnsOriginalWhenNoMappingAnnotation() {
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(PlainInterface.class);
        assertSame(PlainInterface.class, result, "Expected the original class to be returned when no @ConfigMapping is present");
    }

    /**
     * When the supplied type is a {@code @ConfigMapping} interface,
     * the method must return a generated implementation class that is
     * different from the original interface.
     */
    @Test
    void getConfigMappingClass_returnsGeneratedClassForConfigMapping() {
        Class<?> result = ConfigMappingLoader.getConfigMappingClass(Server.class);
        assertNotNull(result, "Generated class must not be null");
        assertNotEquals(Server.class, result, "Generated class should be different from the original interface");
        assertTrue(ConfigMappingObject.class.isAssignableFrom(result), "Generated class should implement ConfigMappingObject");
    }

    /**
     * A type annotated with {@code @ConfigProperties} is *not* a valid
     * configuration‑mapping type. The loader must reject it with an
     * {@link IllegalArgumentException}.
     */
    @Test
    void getConfigMappingClass_throwsWhenConfigPropertiesAnnotationPresent() {
        assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMappingClass(ServerProperties.class), "Expected IllegalArgumentException for @ConfigProperties annotated class");
    }

    @ParameterizedTest(name = "[{index}] type={0}")
    @MethodSource("nullMetadataProvider")
    void getConfigMappingClass_returnsOriginalWhenMetadataIsNull(Class<?> type) {
        // -----------------------------------------------------------------
        // Arrange – mock static collaborators
        // -----------------------------------------------------------------
        try (MockedStatic<ConfigMappingClass> cmcMock = mockStatic(ConfigMappingClass.class);
            MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
            // ConfigMappingClass.getConfigurationClass(type) → null
            cmcMock.when(() -> ConfigMappingClass.getConfigurationClass(type)).thenReturn(null);
            // validateAnnotations should be a no‑op for these happy‑path cases
            loaderMock.when(() -> ConfigMappingLoader.validateAnnotations(type)).thenAnswer(invocation -> null);
            // -----------------------------------------------------------------
            // Act
            // -----------------------------------------------------------------
            Class<?> result = ConfigMappingLoader.getConfigMappingClass(type);
            // -----------------------------------------------------------------
            // Assert
            // -----------------------------------------------------------------
            assertSame(type, result, "When metadata is null the original class must be returned unchanged");
        }
    }

    @ParameterizedTest(name = "[{index}] type={0} → expected={1}")
    @MethodSource("nonNullMetadataProvider")
    void getConfigMappingClass_returnsGeneratedClassWhenMetadataPresent(Class<?> type, Class<?> expectedGenerated) {
        // -----------------------------------------------------------------
        // Arrange – mock static collaborators
        // -----------------------------------------------------------------
        try (MockedStatic<ConfigMappingClass> cmcMock = mockStatic(ConfigMappingClass.class);
            MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
            // Simulate a non‑null metadata object (the actual type is irrelevant)
            Object dummyMetadata = new Object();
            cmcMock.when(() -> ConfigMappingClass.getConfigurationClass(type)).thenReturn(dummyMetadata);
            // validateAnnotations passes silently
            loaderMock.when(() -> ConfigMappingLoader.validateAnnotations(type)).thenAnswer(invocation -> null);
            // loadClass(parent, metadata) → expectedGenerated
            loaderMock.when(() -> ConfigMappingLoader.loadClass(type, dummyMetadata)).thenReturn(expectedGenerated);
            // -----------------------------------------------------------------
            // Act
            // -----------------------------------------------------------------
            Class<?> result = ConfigMappingLoader.getConfigMappingClass(type);
            // -----------------------------------------------------------------
            // Assert
            // -----------------------------------------------------------------
            assertEquals(expectedGenerated, result, "When metadata is present the generated implementation class must be returned");
        }
    }

    @Test
    void getConfigMappingClass_propagatesExceptionFromValidateAnnotations() {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        try (MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
            loaderMock.when(() -> ConfigMappingLoader.validateAnnotations(ServerProperties.class)).thenThrow(new IllegalArgumentException("Invalid @ConfigProperties usage"));
            // The metadata lookup is irrelevant because validation fails first.
            // No need to stub ConfigMappingClass.
            // -----------------------------------------------------------------
            // Act & Assert
            // -----------------------------------------------------------------
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMappingClass(ServerProperties.class));
            assertEquals("Invalid @ConfigProperties usage", thrown.getMessage(), "Exception message must be propagated unchanged");
        }
    }

    @Test
    void getConfigMappingClass_throwsNullPointerExceptionWhenArgumentIsNull() {
        assertThrows(NullPointerException.class, () -> ConfigMappingLoader.getConfigMappingClass(null), "A null argument should cause a NullPointerException");
    }

    @Test
    void getConfigMappingClass_propagatesUndeclaredThrowableExceptionFromLoadClass() {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        try (MockedStatic<ConfigMappingClass> cmcMock = mockStatic(ConfigMappingClass.class);
            MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
            Object dummyMetadata = new Object();
            cmcMock.when(() -> ConfigMappingClass.getConfigurationClass(FaultyConfig.class)).thenReturn(dummyMetadata);
            loaderMock.when(() -> ConfigMappingLoader.validateAnnotations(FaultyConfig.class)).thenAnswer(invocation -> null);
            loaderMock.when(() -> ConfigMappingLoader.loadClass(FaultyConfig.class, dummyMetadata)).thenThrow(new UndeclaredThrowableException(new RuntimeException("boom")));
            // -----------------------------------------------------------------
            // Act & Assert
            // -----------------------------------------------------------------
            assertThrows(UndeclaredThrowableException.class, () -> ConfigMappingLoader.getConfigMappingClass(FaultyConfig.class), "UndeclaredThrowableException must propagate unchanged");
        }
    }

    @Test
    void getConfigMappingClass_propagatesIllegalArgumentExceptionFromMetadataLookup() {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        try (MockedStatic<ConfigMappingClass> cmcMock = mockStatic(ConfigMappingClass.class);
            MockedStatic<ConfigMappingLoader> loaderMock = mockStatic(ConfigMappingLoader.class, CALLS_REAL_METHODS)) {
            cmcMock.when(() -> ConfigMappingClass.getConfigurationClass(NotAnInterface.class)).thenThrow(new IllegalArgumentException("Not an interface"));
            // Validation will not be reached, but we stub it to avoid accidental NPE.
            loaderMock.when(() -> ConfigMappingLoader.validateAnnotations(NotAnInterface.class)).thenAnswer(invocation -> null);
            // -----------------------------------------------------------------
            // Act & Assert
            // -----------------------------------------------------------------
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ConfigMappingLoader.getConfigMappingClass(NotAnInterface.class));
            assertEquals("Not an interface", thrown.getMessage(), "Exception from metadata lookup must be propagated");
        }
    }

    // They are not used by the active test suite.
    @ParameterizedTest(name = "[{index}] type={0}")
    @MethodSource("nullMetadataProvider")
    void placeholder_nullMetadataTest(Class<?> type) {
        // No implementation – retained for compatibility only.
    }

    @ParameterizedTest(name = "[{index}] type={0} → expected={1}")
    @MethodSource("nonNullMetadataProvider")
    void placeholder_nonNullMetadataTest(Class<?> type, Class<?> expectedGenerated) {
        // No implementation – retained for compatibility only.
    }
}
