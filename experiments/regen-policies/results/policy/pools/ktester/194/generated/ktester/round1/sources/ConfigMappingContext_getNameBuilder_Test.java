package io.smallrye.config;

import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
class ConfigMappingContext_getNameBuilder_Test {

    /**
     * Mocked configuration used only to satisfy the constructor – its behaviour is not needed for these tests.
     */
//     @Mock
    SmallRyeConfig mockConfig;

    /**
     * The instance under test.
     */
    ConfigMappingContext context;

    /**
     * The configuration object required by {@link ConfigMappingContext}'s constructor.
     * It is not used by the methods under test, therefore a {@code null} reference is sufficient.
     */
    private SmallRyeConfig config = null;

    /**
     * Create a fresh {@link ConfigMappingContext} before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The constructor expects a map of mappings; an empty map is sufficient for the tests.
        Map<Class<?>, Set<String>> emptyMappings = Collections.emptyMap();
        context = new ConfigMappingContext(config, emptyMappings);
    }

    /**
     * No special teardown required for this test class.
     */
    @AfterEach
    void teardownAfterEach() {
        context = null;
    }

    /**
     * Verify that {@code getNameBuilder()} never returns {@code null}.
     */
    @Test
    void getNameBuilder_isNotNull() {
        assertNotNull(context.getNameBuilder(), "The name builder should never be null");
    }

    /**
     * Verify that successive calls return the same {@link StringBuilder} instance.
     */
    @Test
    void getNameBuilder_returnsSameInstance() {
        StringBuilder first = context.getNameBuilder();
        StringBuilder second = context.getNameBuilder();
        assertSame(first, second, "Subsequent calls must return the same StringBuilder instance");
    }

    /**
     * Verify that modifications performed on the returned builder are visible on subsequent
     * accesses, confirming that the same mutable object is shared.
     */
    @Test
    void getNameBuilder_mutationsAreVisible() {
        StringBuilder sb = context.getNameBuilder();
        sb.append("prefix");
        // Obtain the builder again and check its content.
        StringBuilder sbAgain = context.getNameBuilder();
        assertEquals("prefix", sbAgain.toString(), "Changes made through one reference must be reflected in subsequent references");
    }

    /**
     * Verify that the builder is initially empty (length == 0).
     */
    @Test
    void getNameBuilder_initiallyEmpty() {
        StringBuilder sb = context.getNameBuilder();
        assertEquals(0, sb.length(), "A newly created builder should be empty");
    }

    /**
     * Ensure that two distinct {@link ConfigMappingContext} objects do not share a builder.
     */
    @Test
    void getNameBuilder_distinctInstancesForDifferentContexts() {
        ConfigMappingContext anotherContext = new ConfigMappingContext(config, Collections.emptyMap());
        StringBuilder sb1 = context.getNameBuilder();
        StringBuilder sb2 = anotherContext.getNameBuilder();
        assertNotSame(sb1, sb2, "Builders of different ConfigMappingContext instances must be different objects");
    }

    /**
     * Verify that calling other public methods does not replace or clear the builder.
     * The builder should retain any previously appended content.
     */
    @Test
    void getNameBuilder_unchangedAfterOtherPublicMethods() {
        // Append a known prefix.
        StringBuilder sb = context.getNameBuilder();
        sb.append("base.");
        // The other methods accept nullable arguments; passing {@code null} is sufficient for the test.
        context.applyPrefix("my.prefix");
        context.applyNamingStrategy(null);
        context.applyBeanStyleGetters(Boolean.TRUE);
        // Retrieve the builder again and verify content is unchanged.
        StringBuilder sbAgain = context.getNameBuilder();
        assertSame(sb, sbAgain, "Builder instance must stay the same after other method calls");
        assertEquals("base.", sbAgain.toString(), "Content previously added to the builder must persist after other method calls");
    }

    /**
     * Verify that clearing the builder externally (via {@code setLength(0)}) is reflected on later accesses.
     */
    @Test
    void getNameBuilder_clearingReflectsOnSubsequentCalls() {
        StringBuilder sb = context.getNameBuilder();
        sb.append("temporary");
        // Explicitly clear the builder.
        sb.setLength(0);
        assertEquals(0, sb.length(), "Builder should be empty after setLength(0)");
        // Retrieve again and ensure we have the same instance and it remains empty.
        StringBuilder sbAgain = context.getNameBuilder();
        assertSame(sb, sbAgain, "Builder instance must stay the same after clearing");
        assertEquals(0, sbAgain.length(), "Builder must remain empty after clearing");
    }

    /**
     * Verify that modifications performed by concurrent threads are visible to all threads.
     * Each thread obtains the builder via {@code getNameBuilder()} and appends a distinct string.
     */
    @Test
    void getNameBuilder_concurrentModificationsAreVisible() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        Runnable appendA = () -> {
            try {
                startLatch.await();
                context.getNameBuilder().append("A");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };
        Runnable appendB = () -> {
            try {
                startLatch.await();
                context.getNameBuilder().append("B");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };
        executor.submit(appendA);
        executor.submit(appendB);
        // Release both threads simultaneously.
        startLatch.countDown();
        // Wait for both threads to finish (with a timeout to avoid hanging tests).
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Threads did not finish in time");
        // Shutdown executor.
        executor.shutdownNow();
        String result = context.getNameBuilder().toString();
        // The result must contain both "A" and "B" in any order.
        assertTrue(result.contains("A") && result.contains("B"), "Builder must contain modifications from both threads. Actual: " + result);
        assertEquals(2, result.length(), "Builder length should be exactly 2 after both single‑character appends");
    }
}