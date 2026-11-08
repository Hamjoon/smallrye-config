package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.eclipse.microprofile.config.spi.Converter;
import org.eclipse.microprofile.config.spi.ConfigSource;
import io.smallrye.config.ConfigMapping.NamingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_getNameBuilder_Test {

    /**
     * Mocked configuration used only to satisfy the constructor requirements.
     */
//     @Mock
    SmallRyeConfig mockConfig;

    /**
     * The instance under test.
     */
    ConfigMappingContext context;

    /**
     * Empty roots map – the constructor needs it but the test does not depend on any roots.
     */
    private static final Map<Class<?>, Set<String>> EMPTY_ROOTS = Collections.emptyMap();

    interface DummyGroup {
    }

    /**
     * Dummy group interface used for the {@code constructGroup} test.
     */
//     interface DummyGroup {
    }

    /**
     * Minimal {@link SmallRyeConfig} implementation used only to satisfy the
     * {@link ConfigMappingContext} constructor. The real implementation is not
     * needed for these tests.
     */
    private static class DummyConfig extends SmallRyeConfig {

        DummyConfig() {
            super(new SmallRyeConfigBuilder());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The constructor of ConfigMappingContext expects a non‑null SmallRyeConfig
        // and a map of roots.  We provide a dummy implementation and an empty map.
        SmallRyeConfig dummyConfig = new DummyConfig();
        context = new ConfigMappingContext(dummyConfig, EMPTY_ROOTS);
    }

    @Test
    void getNameBuilder_returnsSameInstanceAndIsNotNull() {
        StringBuilder first = context.getNameBuilder();
        StringBuilder second = context.getNameBuilder();
        assertNotNull(first, "The name builder should never be null");
        assertSame(first, second, "Subsequent calls must return the same StringBuilder instance");
    }

    @Test
    void getNameBuilder_canBeUsedToAppendAndReadBack() {
        StringBuilder builder = context.getNameBuilder();
        builder.append("prefix");
        builder.append('.');
        builder.append("suffix");
        assertEquals("prefix.suffix", builder.toString(), "The builder should retain the characters that were appended");
    }

    @Test
    void getNameBuilder_isInitiallyEmpty() {
        StringBuilder builder = context.getNameBuilder();
        assertEquals(0, builder.length(), "A fresh ConfigMappingContext must start with an empty builder");
    }

    @Test
    void getNameBuilder_modificationsAreVisibleOnSubsequentCalls() {
        StringBuilder builder = context.getNameBuilder();
        builder.append("test");
        StringBuilder again = context.getNameBuilder();
        assertEquals("test", again.toString(), "Mutations on the returned builder must be visible later");
    }

    @Test
    void eachContextOwnsIndependentBuilder() {
        ConfigMappingContext other = new ConfigMappingContext(new DummyConfig(), EMPTY_ROOTS);
        context.getNameBuilder().append("first");
        assertNotSame(context.getNameBuilder(), other.getNameBuilder(), "Different ConfigMappingContext instances must not share the same builder");
        assertEquals(0, other.getNameBuilder().length(), "The second context's builder should be empty");
    }

    @Test
    void applyRootPathDoesNotReplaceOrAlterBuilder() {
        StringBuilder before = context.getNameBuilder();
        context.applyRootPath("my.path");
        StringBuilder after = context.getNameBuilder();
        assertSame(before, after, "applyRootPath must not replace the builder");
        assertEquals(0, after.length(), "Builder content must stay unchanged after applyRootPath");
    }

    @Test
    void applyNamingStrategyDoesNotReplaceOrAlterBuilder() {
        StringBuilder before = context.getNameBuilder();
        context.applyNamingStrategy(NamingStrategy.KEBAB_CASE);
        StringBuilder after = context.getNameBuilder();
        assertSame(before, after, "applyNamingStrategy must not replace the builder");
        assertEquals(0, after.length(), "Builder content must stay unchanged after applyNamingStrategy");
    }

    @Test
    void constructGroupDoesNotReplaceOrPrepopulateBuilder() {
        StringBuilder before = context.getNameBuilder();
        context.constructGroup(DummyGroup.class);
        StringBuilder after = context.getNameBuilder();
        assertSame(before, after, "constructGroup must not replace the builder");
        assertEquals(0, after.length(), "Builder must remain empty after constructGroup");
    }

    @Test
    void setLengthZeroEmptiesBuilderAndIsReflectedLater() {
        StringBuilder builder = context.getNameBuilder();
        builder.append("something");
        assertTrue(builder.length() > 0, "Pre‑condition: builder must contain data");
        builder.setLength(0);
        assertEquals(0, context.getNameBuilder().length(), "After setLength(0) the builder must be empty on subsequent accesses");
    }

    @Test
    void appendAndDeleteOperationsPersist() {
        StringBuilder builder = context.getNameBuilder();
        builder.append("abcde");
        builder.delete(1, 4);
        assertEquals("ae", context.getNameBuilder().toString(), "After append and delete the builder should contain \"ae\"");
    }

    @Test
    void largeStringAppendsDoNotReplaceBuilder() {
        StringBuilder builder = context.getNameBuilder();
        String large = "x".repeat(10_000);
        builder.append(large);
        assertEquals(10_000, builder.length(), "Builder length must reflect the large appended content");
        assertSame(builder, context.getNameBuilder(), "The same builder instance must be returned after large modifications");
        assertEquals('x', builder.charAt(0));
        assertEquals('x', builder.charAt(9_999));
    }

    @Test
    void concurrentCallsReturnIdenticalBuilderInstance() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            Callable<StringBuilder> task = context::getNameBuilder;
            var futures = IntStream.range(0, threadCount).mapToObj(i -> executor.submit(task)).toList();
            StringBuilder first = futures.get(0).get();
            for (Future<StringBuilder> f : futures) {
                assertSame(first, f.get(), "All concurrent calls must return the same StringBuilder instance");
            }
        } finally {
            executor.shutdownNow();
        }
    }
}