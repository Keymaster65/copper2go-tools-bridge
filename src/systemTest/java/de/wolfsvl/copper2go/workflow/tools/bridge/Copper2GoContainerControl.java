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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Copper2GoContainerControl {

    private static final Logger log = LoggerFactory.getLogger(Copper2GoContainerControl.class);

    private Copper2GoContainerControl() {
    }

    static GenericContainer<?> start(final String configName, final Network network) throws IOException {
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

}
