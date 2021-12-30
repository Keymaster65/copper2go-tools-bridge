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

import com.google.common.io.CharStreams;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

public class HttpKafkaBridgeTest {

    private static final Logger log = LoggerFactory.getLogger(HttpKafkaBridgeTest.class);
    static KafkaContainer kafka;

    @Rule
    public static final Network network = Network.newNetwork();

    @Test
    void systemTest() throws URISyntaxException, IOException, InterruptedException {
        String payload = "{\"name\" = \"Wolf\"}";
        try (final GenericContainer<?> copper2GoContainerHttpKafkaBridge = startCopper2GoContainer("configHttpKafkaBridge")) {
            HttpResponse<String> response = TestHttpClient.post(
                    UriFactory.create("/copper2go/2/api/request/1.0/Bridge?key=value", copper2GoContainerHttpKafkaBridge),
                    payload);
            Assertions.assertThat(response.body()).startsWith("{");
            log.info("configHttpKafkaBridge.log:\n{}", copper2GoContainerHttpKafkaBridge.getLogs());
        }

        final ClientAndServer clientAndServer = startHttpServerSimulator();

        try (GenericContainer<?> copper2GoContainerKafkaHttpBridge = startCopper2GoContainer("configKafkaHttpBridge")) {
            waitForRequest(clientAndServer, payload);
            log.info("configKafkaHttpBridge.log:\n{}", copper2GoContainerKafkaHttpBridge.getLogs());
        } finally {
            clientAndServer.stop();
        }
    }

    @AfterAll
    static void stopKafkaContainer() {
        log.info("Stopping TestContainer.");
        kafka.stop();
        log.info("TestContainer stopped.");
    }

    @BeforeAll
    static void startKafkaContainer() {
        // SonarLint: Use try-with-resources or close this "KafkaContainer" in a "finally" clause.
        // In stop() only
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.5.6")) // NOSONAR
                .withNetwork(network)
                .withNetworkAliases("kafka");
        kafka.start();
        while (!kafka.isRunning()) {
            log.info("Wait for kafka running.");
            LockSupport.parkNanos(50L * 1000 * 1000);
        }
        log.info("Kafka server: {} with port {}. Exposed: {}", kafka.getBootstrapServers(), kafka.getFirstMappedPort(), kafka.getExposedPorts());
    }

    private static GenericContainer<?> startCopper2GoContainer(final String configName) throws IOException {
        String config = CharStreams.toString(
                new InputStreamReader(
                        Objects.requireNonNull(HttpKafkaBridgeTest.class.getResourceAsStream(configName + ".json")),
                        StandardCharsets.UTF_8
                )
        );
        GenericContainer<?> copper2GoContainer = new GenericContainer<>(DockerImageName.parse("keymaster65/copper2go:latest")) // NOSONAR
                .withExposedPorts(59665)
                .withImagePullPolicy(imageName -> true)
                .withNetworkAliases("copper2go")
                .withNetwork(network)
                .withEnv("C2G_CONFIG", config);
        copper2GoContainer.start();

        log.info("copper2go server started with port {}. Exposed: {}", copper2GoContainer.getFirstMappedPort(), copper2GoContainer.getExposedPorts());
        return copper2GoContainer;
    }

    private ClientAndServer startHttpServerSimulator() {
        final ClientAndServer clientAndServer = new ClientAndServer(59999);
        clientAndServer
                .when(
                        HttpRequest.request()
                                .withMethod("GET")
                )
                .respond(
                        org.mockserver.model.HttpResponse.response()
                                .withBody("yes")

                );
        return clientAndServer;
    }

    private void waitForRequest(final ClientAndServer clientAndServer, final String payload) {
        boolean waitForRequest = true;
        do {
            try {
                clientAndServer
                        .verify(
                                HttpRequest.request()
                                        .withPath("/")
                                        .withBody(payload)
                        );
                waitForRequest = false;
                log.info("Received request {}.", payload);
            } catch (AssertionError assertionError) {
                log.info("Wait for request.");
                LockSupport.parkNanos(1000L * 1000L * 1000L);
            }
        } while (waitForRequest);
    }
}
