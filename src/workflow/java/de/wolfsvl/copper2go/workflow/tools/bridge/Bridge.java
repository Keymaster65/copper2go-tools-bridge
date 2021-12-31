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

import io.github.keymaster65.copper2go.api.workflow.ReplyChannelStore;
import io.github.keymaster65.copper2go.api.workflow.RequestChannelStore;
import io.github.keymaster65.copper2go.api.workflow.WorkflowData;
import org.copperengine.core.AutoWire;
import org.copperengine.core.Interrupt;
import org.copperengine.core.Response;
import org.copperengine.core.WaitMode;
import org.copperengine.core.Workflow;
import org.copperengine.core.WorkflowDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.util.Map;

@WorkflowDescription(alias = "Bridge", majorVersion = 1, minorVersion = 0, patchLevelVersion = 0)
public class Bridge extends Workflow<WorkflowData> {
    @Serial
    private static final long serialVersionUID = 1;

    private static final Logger logger = LoggerFactory.getLogger(Bridge.class);
    private transient ReplyChannelStore replyChannelStore;

    @AutoWire
    public void setReplyChannelStore(ReplyChannelStore replyChannelStore) {
        this.replyChannelStore = replyChannelStore;
    }

    private transient RequestChannelStore requestChannelStore;

    @AutoWire
    public void setRequestChannelStore(RequestChannelStore requestChannelStore) {
        this.requestChannelStore = requestChannelStore;
    }

    public void replyError(final String message) {
        if (getData().getUUID() != null) {
            replyChannelStore.replyError(getData().getUUID(), message);
        }
    }

    public void reply(final String message) {
        if (getData().getUUID() != null) {
            replyChannelStore.reply(getData().getUUID(), message);
        }
    }

    @Override
    public void main() throws Interrupt {
        try {
            logger.info("Begin workflow {} 1.0 with UUID {}.", this.getClass().getSimpleName(), getData().getUUID());
            final String response = callRequestChannel(getData().getPayload(), getData().getAttributes());
            reply(response);
        } catch (Exception e) {
            replyError(e.getClass().getSimpleName() + ": " + e.getMessage());
            throw new BridgeRuntimeException("Could not process payload: " + getData().getPayload(), e);
        }
        logger.info("Finish workflow {} 1.0.", this.getClass().getSimpleName());
    }

    private String callRequestChannel(final String payload, final Map<String, String> attributes) throws Interrupt {
        String correlationId = getEngine().createUUID();
        requestChannelStore.request("RequestChannel", payload, attributes, correlationId);
        wait(WaitMode.FIRST, 3000, correlationId);
        Response<String> response = getAndRemoveResponse(correlationId);
        if (response == null) {
            throw new BridgeRuntimeException("Response is null, could not call RequestChannel.");
        }
        if (response.isTimeout()) {
            throw new BridgeRuntimeException("Timeout, could call RequestChannel.");
        } else if (null != response.getException()) {
            throw new BridgeRuntimeException("Could call RequestChannel.", response.getException());
        }
        return response.getResponse();
    }
}
