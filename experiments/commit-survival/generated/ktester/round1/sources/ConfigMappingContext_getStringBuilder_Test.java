package io.smallrye.config;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.eclipse.microprofile.config.spi.Converter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigMappingContext_getStringBuilder_Test {

    // Mocked dependency required by the ConfigMappingContext constructor
    @Mock
    SmallRyeConfig mockConfig;

    // Instance under test
    ConfigMappingContext context;

    interface DummyRoot {
    }

    interface DummyRoot {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Minimal, valid arguments for the constructor
        Map<String, List<Class<?>>> roots = Collections.emptyMap();
        Map<String, Map<String, Set<String>>> names = Collections.emptyMap();
        // Create the context using the mocked SmallRyeConfig
        context = new ConfigMappingContext(mockConfig, roots, names);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test
        context = null;
    }

    @Test
    void getStringBuilder_shouldNeverReturnNull() {
        StringBuilder sb = context.getStringBuilder();
        assertNotNull(sb, "getStringBuilder() must not return null");
    }

    @Test
    void getStringBuilder_shouldReturnSameInstanceOnRepeatedCalls() {
        StringBuilder first = context.getStringBuilder();
        StringBuilder second = context.getStringBuilder();
        assertSame(first, second, "Repeated calls should return the same StringBuilder instance");
    }

    @Test
    void getStringBuilder_shouldAllowMutationsToBeObserved() {
        StringBuilder sb = context.getStringBuilder();
        sb.append("test");
        // Retrieve again and verify the mutation is present
        StringBuilder sbAgain = context.getStringBuilder();
        assertEquals("test", sbAgain.toString(), "Mutations performed on the returned StringBuilder must be visible on subsequent calls");
    }

    @Test
    void getStringBuilder_initiallyEmpty_lengthZero() {
        StringBuilder sb = context.getStringBuilder();
        assertEquals(0, sb.length(), "A newly created StringBuilder must have length 0");
    }

    @Test
    void getStringBuilder_initiallyEmpty_toStringIsEmpty() {
        StringBuilder sb = context.getStringBuilder();
        assertEquals("", sb.toString(), "toString() of a fresh StringBuilder must be the empty string");
    }

    @Test
    void getStringBuilder_defaultCapacityAtLeast16() {
        StringBuilder sb = context.getStringBuilder();
        assertTrue(sb.capacity() >= 16, "The default capacity of a new StringBuilder should be at least 16 characters");
    }

    @Test
    void getStringBuilder_instanceUnchangedAfterOtherPublicMethods() {
        // Capture the original instance
        StringBuilder original = context.getStringBuilder();
        // 1. applyNamingStrategy – use a mock NamingStrategy that simply returns the input unchanged
        ConfigMapping.NamingStrategy mockStrategy = mock(ConfigMapping.NamingStrategy.class);
        when(mockStrategy.apply(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        // method returns a NamingStrategy, we ignore it
        context.applyNamingStrategy(mockStrategy);
        // 2. constructRoot – use a dummy interface; the method may return null or throw,
        //    but we are only interested in the side‑effect on the StringBuilder.
        //    To avoid side effects we wrap the call in a try/catch and ignore any exception.
        interface DummyRoot {
        }
        try {
            context.<DummyRoot>constructRoot(DummyRoot.class);
        } catch (Throwable ignored) {
            // The implementation is not relevant for this test; any exception is ignored.
        }
        // Verify the StringBuilder instance is still the same
        StringBuilder after = context.getStringBuilder();
        assertSame(original, after, "Calling unrelated public methods must not replace the StringBuilder");
    }

    @Test
    void getStringBuilder_concurrentModificationsAreVisible() throws Exception {
        final StringBuilder sb = context.getStringBuilder();
        // Tokens each thread will append
        final List<String> tokens = List.of("A", "B", "C", "D", "E");
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(tokens.size());
        ExecutorService executor = Executors.newFixedThreadPool(tokens.size());
        for (String token : tokens) {
            executor.submit(() -> {
                try {
                    // wait for the go‑signal
                    startLatch.await();
                    sb.append(token);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Release all threads at once
        startLatch.countDown();
        // Wait for all threads to finish
        doneLatch.await();
        executor.shutdownNow();
        // Verify that each token appears at least once in the final string.
        String result = sb.toString();
        for (String token : tokens) {
            assertTrue(result.contains(token), "Resulting StringBuilder must contain token \"" + token + "\" after concurrent appends");
        }
    }

    @Test
    void getStringBuilder_mutationsDeleteInsertAreObserved() {
        StringBuilder sb = context.getStringBuilder();
        // Build initial content
        // => "hello"
        sb.append("hello");
        // Delete first three characters ("hel")
        // => "lo"
        sb.delete(0, 3);
        // Insert "hey" at the beginning
        // => "heylo"
        sb.insert(0, "hey");
        // Retrieve again and verify the final content
        StringBuilder again = context.getStringBuilder();
        assertEquals("heylo", again.toString(), "After delete and insert operations the StringBuilder must reflect the changes");
    }
}
