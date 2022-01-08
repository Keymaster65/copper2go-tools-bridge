/*
 * Copyright 2021 Wolf Sluyterman van Langeweyde
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.wolfsvl.copper2go.workflow.tools.bridge;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;

class HttpKafkaBridgeTest {

    private static final Logger log = LoggerFactory.getLogger(HttpKafkaBridgeTest.class);

    @Rule
    public static final Network network = Network.newNetwork();

    @Test
    void systemTest() throws URISyntaxException, IOException, InterruptedException {
        String payload = "{\"name\" = \"Wolf\"}";
        sendViaHttpToKafka(payload);
        receiveFromKafkaViaHttp(payload);
    }

    @BeforeAll
    static void startKafkaContainer() {
        KafkaContainerControl.start(network);
    }

    @AfterAll
    static void stopKafkaContainer() {
        KafkaContainerControl.stop();
    }

    private void sendViaHttpToKafka(final String payload) throws IOException, InterruptedException, URISyntaxException {
        try (final GenericContainer<?> copper2GoContainerHttpKafkaBridge = Copper2GoContainerControl.start("configHttpKafkaBridge", network)) {
            HttpResponse<String> response = TestHttpClient.post(
                    UriFactory.create("/copper2go/3/api/twoway/1.0/Bridge?key=value", copper2GoContainerHttpKafkaBridge),
                    payload);
            final String body = response.body();
            log.info("Response body: {}", body);
            log.info("configHttpKafkaBridge.log:\n{}", copper2GoContainerHttpKafkaBridge.getLogs());
            Assertions.assertThat(body).startsWith("{");
        }
    }

    private void receiveFromKafkaViaHttp(final String payload) throws IOException {
        final ClientAndServer clientAndServer = HttpServerSimulator.start();

        try (GenericContainer<?> copper2GoContainerKafkaHttpBridge = Copper2GoContainerControl.start("configKafkaHttpBridge", network)) {
            HttpServerSimulator.waitForRequest(clientAndServer, payload);
            log.info("configKafkaHttpBridge.log:\n{}", copper2GoContainerKafkaHttpBridge.getLogs());
        } finally {
            clientAndServer.stop();
        }
    }
}
