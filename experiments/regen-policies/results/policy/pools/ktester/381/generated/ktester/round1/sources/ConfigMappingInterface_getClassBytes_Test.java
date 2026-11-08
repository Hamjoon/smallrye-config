package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigMappingInterface_getClassBytes_Test {

    /**
     * Instance under test.
     */
    private ConfigMappingInterface configMappingInterface;

    /**
     * Minimal configuration interface used only to obtain a {@code Class<?>} token.
     */
    @ConfigMapping(prefix = "dummy")
    interface DummyConfig {
    }

    @BeforeEach
    void setupBeforeEach() {
        // The constructor is package‑private, which is accessible from this test
        // because it lives in the same package (io.smallrye.config).
        configMappingInterface = new ConfigMappingInterface(DummyConfig.class, ConfigMappingInterface.NO_TYPES, ConfigMappingInterface.NO_PROPERTIES);
    }

    @Test
    void getClassBytes_returnsGeneratedBytes() {
        // Act
        byte[] bytes = configMappingInterface.getClassBytes();
        // Assert
        assertNotNull(bytes, "Generated class bytes should not be null");
        assertTrue(bytes.length > 0, "Generated class bytes should have positive length");
    }
}
