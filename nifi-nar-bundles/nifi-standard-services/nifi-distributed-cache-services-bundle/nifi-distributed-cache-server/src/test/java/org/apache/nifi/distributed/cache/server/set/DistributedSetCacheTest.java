/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.distributed.cache.server.set;

import org.apache.commons.lang3.SerializationException;
import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.distributed.cache.client.DistributedSetCacheClientService;
import org.apache.nifi.distributed.cache.server.DistributedSetCacheServer;
import org.apache.nifi.processor.Processor;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verify basic functionality of {@link DistributedSetCacheClientService}.
 * <p>
 * This test instantiates both the server and client {@link org.apache.nifi.controller.ControllerService} objects
 * implementing the distributed cache protocol.  It assumes that the default distributed cache port (4557)
 * is available.
 */
public class DistributedSetCacheTest {

    private static TestRunner runner = null;
    private static DistributedSetCacheServer server = null;
    private static DistributedSetCacheClientService client = null;
    private static final Serializer<String> serializer = new StringSerializer();

    @BeforeClass
    public static void beforeClass() throws Exception {
        final String port = DistributedSetCacheServer.PORT.getDefaultValue();
        runner = TestRunners.newTestRunner(Mockito.mock(Processor.class));

        server = new DistributedSetCacheServer();
        runner.addControllerService(server.getClass().getName(), server);
        runner.setProperty(server, DistributedSetCacheServer.PORT, port);
        runner.enableControllerService(server);

        client = new DistributedSetCacheClientService();
        runner.addControllerService(client.getClass().getName(), client);
        runner.setProperty(client, DistributedSetCacheClientService.HOSTNAME, "localhost");
        runner.setProperty(client, DistributedSetCacheClientService.PORT, port);
        runner.enableControllerService(client);
    }

    @AfterClass
    public static void afterClass() {
        runner.disableControllerService(client);
        runner.removeControllerService(client);

        runner.disableControllerService(server);
        runner.removeControllerService(server);
    }

    @Test
    public void testSetOperations() throws IOException {
        final String value = "value";
        assertFalse(client.contains(value, serializer));
        assertTrue(client.addIfAbsent(value, serializer));
        assertFalse(client.addIfAbsent(value, serializer));
        assertTrue(client.contains(value, serializer));
        assertTrue(client.remove(value, serializer));
        assertFalse(client.contains(value, serializer));
    }

    private static class StringSerializer implements Serializer<String> {
        @Override
        public void serialize(final String value, final OutputStream output) throws SerializationException, IOException {
            output.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
