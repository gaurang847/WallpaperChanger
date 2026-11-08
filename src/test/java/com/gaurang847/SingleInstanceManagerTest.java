package com.gaurang847;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class SingleInstanceManagerTest {

    @After
    public void tearDown() {
        SingleInstanceManager.cleanup();
    }

    @Test
    public void testAcquireLockAndSecondInstanceRejection() {
        // First acquire should succeed
        boolean first = SingleInstanceManager.checkAndAcquireLock(() -> {});
        assertTrue("First instance must acquire lock successfully", first);

        // Second acquire from same or duplicate process context should fail (lock already held)
        boolean second = SingleInstanceManager.checkAndAcquireLock(() -> {});
        assertFalse("Second instance must be rejected when lock is held", second);

        // After cleanup, lock can be acquired again
        SingleInstanceManager.cleanup();
        boolean third = SingleInstanceManager.checkAndAcquireLock(() -> {});
        assertTrue("After cleanup, new instance should be able to acquire lock", third);
    }
}
