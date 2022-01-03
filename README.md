# copper2go-tools-bridge

Simple workflow that submits the workflow data to a RequestChannel and delivers the RequestChannel's response as workflow
reply. In no reply is supported be the (workflow) receiver, it will be skipped.

It can be deployed in a copper2go container. You will find more in https://github.com/Keymaster65/copper2go

## Example

This is an example Bridge, set submits HTTP body into Kafka topic.

### Configuration

#### Kafka Container
May be you want to start a Kafka service first? This one was tested on Windows:
https://raw.githubusercontent.com/Keymaster65/copper2go-tools-bridge/master/docker-compose.yml
It can be start with 

`docker-compose up`

#### Docker Windows Host
Pay attention to the "host.docker.internal". On Windows you can use that as kafkaHost in the following configuration steps.

#### copper2go Configuration
Download the test configuration
https://raw.githubusercontent.com/Keymaster65/copper2go-tools-bridge/master/src/systemTest/resources/de/wolfsvl/copper2go/workflow/tools/bridge/configHttpKafkaBridge.json
and change it to your needs.

#### Start copper2go Container
Now 

`docker run -d -e C2G_CONFIG="$(cat configHttpKafkaBridge.json)" --pull always  --name copper2go -p 59665:59665 --rm registry.hub.docker.com/keymaster65/copper2go:3`

#### Test Payload
Send data to copper2go using `curl`

`curl --data Wolf http://localhost:59665/copper2go/3/api/twoway/1.0/Bridge`

If should return something like

{"checksum":3897875225,"offset":0,"partition":0,"timestamp":1641063641561,"topic":"targetTopic"}

This is the result in Kafka, where the payload was sent to a topic (targetTopic).

#### Trouble Shouting
If the return is unexpected, like

BridgeRuntimeException: Timeout, could call RequestChannel.

then check your containers and their configurations. You can get deeper, if you inspect the container logs.