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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.locks.LockSupport;

class KafkaContainerControl {

    static KafkaContainer kafka;

    private static final Logger log = LoggerFactory.getLogger(KafkaContainerControl.class);

    private KafkaContainerControl() {
    }

    static void start(@SuppressWarnings("SameParameterValue") final Network network) {
        // SonarLint: Use try-with-resources or close this "KafkaContainer" in a "finally" clause.
        // In stop() only
        KafkaContainerControl.kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.5.6")) // NOSONAR
                .withNetwork(network)
                .withNetworkAliases("kafka");
        KafkaContainerControl.kafka.start();
        while (!KafkaContainerControl.kafka.isRunning()) {
            log.info("Wait for kafka running.");
            LockSupport.parkNanos(50L * 1000 * 1000);
        }
        log.info("Kafka server: {} with port {}. Exposed: {}", KafkaContainerControl.kafka.getBootstrapServers(), KafkaContainerControl.kafka.getFirstMappedPort(), KafkaContainerControl.kafka.getExposedPorts());
    }

    static void stop() {
        log.info("Stopping KafkaContainer.");
        KafkaContainerControl.kafka.stop();
        log.info("KafkaContainer stopped.");
    }
}
