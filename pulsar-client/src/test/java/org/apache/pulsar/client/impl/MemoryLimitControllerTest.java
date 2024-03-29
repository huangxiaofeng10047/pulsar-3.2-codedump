/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.client.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MemoryLimitControllerTest {

    private ExecutorService executor;

    @BeforeClass
    void setup() {
        executor = Executors.newCachedThreadPool();
    }

    @AfterClass(alwaysRun = true)
    void teardown() {
        executor.shutdownNow();
    }

    @Test
    public void testLimit() throws Exception {
        MemoryLimitController mlc = new MemoryLimitController(100);

        for (int i = 0; i < 101; i++) {
            mlc.reserveMemory(1);
        }

        assertEquals(mlc.currentUsage(), 101);
        assertEquals(mlc.currentUsagePercent(), 1.01);
        assertFalse(mlc.tryReserveMemory(1));
        mlc.releaseMemory(1);
        assertEquals(mlc.currentUsage(), 100);
        assertEquals(mlc.currentUsagePercent(), 1.0);

        assertTrue(mlc.tryReserveMemory(1));
        assertEquals(mlc.currentUsage(), 101);

        mlc.forceReserveMemory(99);
        assertFalse(mlc.tryReserveMemory(1));
        assertEquals(mlc.currentUsagePercent(), 2.0);

        mlc.releaseMemory(50);
        assertFalse(mlc.tryReserveMemory(1));
        assertEquals(mlc.currentUsagePercent(), 1.5);

        mlc.releaseMemory(50);
        assertTrue(mlc.tryReserveMemory(1));
        assertEquals(mlc.currentUsagePercent(), 1.01);
    }

    @Test
    public void testTrigger() {
        AtomicBoolean trigger = new AtomicBoolean(false);
        MemoryLimitController mlc = new MemoryLimitController(100, 95, () -> trigger.set(true));

        mlc.forceReserveMemory(94);
        Assert.assertFalse(trigger.get());
        mlc.forceReserveMemory(1);
        Assert.assertTrue(trigger.get());

        trigger.set(false);
        for (int i = 0; i < 5; i++) {
            mlc.forceReserveMemory(1);
            Assert.assertFalse(trigger.get());
        }

        mlc.releaseMemory(50);
        Assert.assertTrue(mlc.tryReserveMemory(50));
        Assert.assertTrue(trigger.get());
    }

    @Test
    public void testBlocking() throws Exception {
        MemoryLimitController mlc = new MemoryLimitController(100);

        for (int i = 0; i < 101; i++) {
            mlc.reserveMemory(1);
        }

        CountDownLatch l1 = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                mlc.reserveMemory(1);
                l1.countDown();
            } catch (InterruptedException e) {
            }
        });

        CountDownLatch l2 = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                mlc.reserveMemory(1);
                l2.countDown();
            } catch (InterruptedException e) {
            }
        });

        CountDownLatch l3 = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                mlc.reserveMemory(1);
                l3.countDown();
            } catch (InterruptedException e) {
            }
        });

        // The threads are blocked since the quota is full
        assertFalse(l1.await(100, TimeUnit.MILLISECONDS));
        assertFalse(l2.await(100, TimeUnit.MILLISECONDS));
        assertFalse(l3.await(100, TimeUnit.MILLISECONDS));

        assertEquals(mlc.currentUsage(), 101);
        mlc.releaseMemory(3);

        assertTrue(l1.await(1, TimeUnit.SECONDS));
        assertTrue(l2.await(1, TimeUnit.SECONDS));
        assertTrue(l3.await(1, TimeUnit.SECONDS));
        assertEquals(mlc.currentUsage(), 101);
    }

    @Test
    public void testStepRelease() throws Exception {
        MemoryLimitController mlc = new MemoryLimitController(100);

        for (int i = 0; i < 101; i++) {
            mlc.reserveMemory(1);
        }

        CountDownLatch l1 = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                mlc.reserveMemory(1);
                l1.countDown();
            } catch (InterruptedException e) {
            }
        });

        CountDownLatch l2 = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                mlc.reserveMemory(1);
                l2.countDown();
            } catch (InterruptedException e) {
            }
        });

        CountDownLatch l3 = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                mlc.reserveMemory(1);
                l3.countDown();
            } catch (InterruptedException e) {
            }
        });

        // The threads are blocked since the quota is full
        assertFalse(l1.await(100, TimeUnit.MILLISECONDS));
        assertFalse(l2.await(100, TimeUnit.MILLISECONDS));
        assertFalse(l3.await(100, TimeUnit.MILLISECONDS));

        assertEquals(mlc.currentUsage(), 101);

        mlc.releaseMemory(1);
        mlc.releaseMemory(1);
        mlc.releaseMemory(1);

        assertTrue(l1.await(1, TimeUnit.SECONDS));
        assertTrue(l2.await(1, TimeUnit.SECONDS));
        assertTrue(l3.await(1, TimeUnit.SECONDS));
        assertEquals(mlc.currentUsage(), 101);
    }

    @Test
    public void testModifyMemoryFailedDueToNegativeParam() throws Exception {
        MemoryLimitController mlc = new MemoryLimitController(100);

        try {
            mlc.tryReserveMemory(-1);
            fail("The test should fail due to calling tryReserveMemory with a negative value.");
        } catch (IllegalArgumentException e) {
            // Expected ex.
        }

        try {
            mlc.reserveMemory(-1);
            fail("The test should fail due to calling reserveMemory with a negative value.");
        } catch (IllegalArgumentException e) {
            // Expected ex.
        }

        try {
            mlc.forceReserveMemory(-1);
            fail("The test should fail due to calling forceReserveMemory with a negative value.");
        } catch (IllegalArgumentException e) {
            // Expected ex.
        }

        try {
            mlc.releaseMemory(-1);
            fail("The test should fail due to calling releaseMemory with a negative value.");
        } catch (IllegalArgumentException e) {
            // Expected ex.
        }

        assertEquals(mlc.currentUsage(), 0);
    }
}
