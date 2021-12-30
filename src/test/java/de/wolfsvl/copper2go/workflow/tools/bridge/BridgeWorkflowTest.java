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

import io.github.keymaster65.copper2go.util.Copper2goDependencyInjector;
import io.github.keymaster65.copper2go.util.WorkflowTestRunner;
import io.github.keymaster65.copper2go.workflowapi.EventChannelStore;
import io.github.keymaster65.copper2go.workflowapi.ReplyChannelStore;
import io.github.keymaster65.copper2go.workflowapi.RequestChannelStore;
import io.github.keymaster65.copper2go.workflowapi.WorkflowData;
import org.copperengine.core.Acknowledge;
import org.copperengine.core.CopperException;
import org.copperengine.core.Response;
import org.copperengine.core.tranzient.TransientScottyEngine;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BridgeWorkflowTest {

    public static final String WORKFLOW_DIR = "src/workflow/java";
    public static final String WORKFLOW_NAME = "Bridge";
    public static final String UUID = "uuid";

    @Test
    void replyGoodCaseTest() throws Exception {
        final ReplyChannelStore replyChannelStoreMock = mock(ReplyChannelStore.class);
        final RequestChannelStore requestChannelStoreMock = mock(RequestChannelStore.class);
        final EventChannelStore eventChannelStoreMock = mock(EventChannelStore.class);

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("key", "value");
        runWorkflow(
                replyChannelStoreMock,
                requestChannelStoreMock,
                eventChannelStoreMock,
                "Hello",
                attributes,
                "Response",
                null
        );

        verify(requestChannelStoreMock).request(Mockito.any(),Mockito.eq("Hello"), Mockito.eq(attributes), Mockito.any());
        verify(replyChannelStoreMock).reply(Mockito.any(), Mockito.eq("Response"));
    }

    @Test
    void replyExceptionTest() throws Exception {
        final ReplyChannelStore replyChannelStoreMock = mock(ReplyChannelStore.class);
        final RequestChannelStore requestChannelStoreMock = mock(RequestChannelStore.class);
        final EventChannelStore eventChannelStoreMock = mock(EventChannelStore.class);

        runWorkflow(
                replyChannelStoreMock,
                requestChannelStoreMock,
                eventChannelStoreMock,
                "Hello",
                null,
                null,
                new IllegalArgumentException("Test")
        );

        verify(requestChannelStoreMock).request(Mockito.any(),Mockito.eq("Hello"), Mockito.any(), Mockito.any());
        verify(replyChannelStoreMock, times(0)).reply(Mockito.any(), Mockito.any());
        verify(replyChannelStoreMock).replyError(Mockito.any(), Mockito.eq("BridgeRuntimeException: Could call RequestChannel."));
    }

    private void runWorkflow(
            final ReplyChannelStore replyChannelStoreMock,
            final RequestChannelStore requestChannelStoreMock,
            final EventChannelStore eventChannelStoreMock,
            final String payload,
            final Map<String,String> attributes,
            final String response,
            Exception exception
    ) throws CopperException {
        TransientScottyEngine engine = WorkflowTestRunner.createTestEngine(
                WORKFLOW_DIR,
                new Copper2goDependencyInjector(
                        replyChannelStoreMock,
                        eventChannelStoreMock,
                        requestChannelStoreMock
                )
        );

        Mockito.doAnswer(invocation -> {
            String responseRorrelationId = invocation.getArgument(3);
            Response<String> copperResponse = new Response<>(responseRorrelationId, response, exception);
            engine.notify(copperResponse, new Acknowledge.BestEffortAcknowledge());
            return null;
        }).when(requestChannelStoreMock).request(Mockito.eq("RequestChannel"), Mockito.eq(payload), Mockito.any(), Mockito.any());

        WorkflowTestRunner.runTest(
                new WorkflowData(UUID, payload, attributes),
                new WorkflowTestRunner.WorkflowDefinition(WORKFLOW_NAME, 1L, 0L),
                engine
        );
    }
}

