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

import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.LockSupport;

class HttpServerSimulator {

    private static final Logger log = LoggerFactory.getLogger(HttpServerSimulator.class);

    private HttpServerSimulator() {
    }


    static ClientAndServer start() {
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

    static void waitForRequest(final ClientAndServer clientAndServer, final String payload) {
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
