# Eclipse Paho Java MQTTv3 Client

The Paho Java Client provides two APIs: MqttAsyncClient provides a fully asychronous API where completion of activities is notified via registered callbacks. MqttClient is a synchronous wrapper around MqttAsyncClient where functions appear synchronous to the application.

## Using the Paho Java MQTTv3 Client

### Downloading

Eclipse hosts a Nexus repository for those who want to use Maven to manage their dependencies. The released libraries are also available in the Maven Central repository.

Add the repository definition and the dependency definition shown below to your pom.xml.

Replace %REPOURL% with either ``` https://repo.eclipse.org/content/repositories/paho-releases/ ``` for the official releases, or ``` https://repo.eclipse.org/content/repositories/paho-snapshots/  ``` for the nightly snapshots. Replace %VERSION% with the level required .

The latest release version is ```1.2.5``` and the current snapshot version is ```1.2.6-SNAPSHOT```.

```
<project ...>
<repositories>
    <repository>
        <id>Eclipse Paho Repo</id>
        <url>%REPOURL%</url>
    </repository>
</repositories>
...
<dependencies>
    <dependency>
        <groupId>org.eclipse.paho</groupId>
        <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
        <version>%VERSION%</version>
    </dependency>
</dependencies>
</project>

```

If you find that there is functionality missing or bugs in the release version, you may want to try using the snapshot version to see if this helps before raising a feature request or an issue.

### Building from source

There are two active branches on the Paho Java git repository, ```master``` which is used to produce stable releases, and ```develop``` where active development is carried out. By default cloning the git repository will download the ```master``` branch, to build from ```develop``` make sure you switch to the remote branch: ``` git checkout -b develop remotes/origin/develop ```

To then build the library run the following maven command: ```mvn package -DskipTests```

This will build the client library without running the tests. The jars for the library, source and javadoc can be found in the ```org.eclipse.paho.client.mqttv3/target``` directory.

## Documentation
Reference documentation is online at: [http://www.eclipse.org/paho/files/javadoc/index.html](http://www.eclipse.org/paho/files/javadoc/index.html)

Log and Debug in the Java Client: [https://wiki.eclipse.org/Paho/Log_and_Debug_in_the_Java_client](https://wiki.eclipse.org/Paho/Log_and_Debug_in_the_Java_client)

## Getting Started

The included code below is a very basic sample that connects to a server and publishes a message using the MqttClient synchronous API. More extensive samples demonstrating the use of the Asynchronous API can be found in the ```org.eclipse.paho.sample.mqttv3app``` directory of the source.


```
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttPublishSample {

    public static void main(String[] args) {

        String topic        = "MQTT Examples";
        String content      = "Message from MqttPublishSample";
        int qos             = 2;
        String broker       = "tcp://iot.eclipse.org:1883";
        String clientId     = "JavaSample";
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
            System.out.println("Publishing message: "+content);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            sampleClient.publish(topic, message);
            System.out.println("Message published");
            sampleClient.disconnect();
            System.out.println("Disconnected");
            System.exit(0);
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }
}
```

## Adding custom headers for Websocket connection

The included code below is a extended basic sample that connects to a server with custom headers.

```
MqttClient client = new MqttClient("wss://<BROKER_URI>", "MyClient");

MqttConnectOptions connectOptions = new MqttConnectOptions();
Properties properties = new Properties();
properties.setProperty("X-Amz-CustomAuthorizer-Name", <SOME_VALUE>);
properties.setProperty("X-Amz-CustomAuthorizer-Signature", <SOME_VALUE>);
properties.setProperty(<SOME_VALUE>, <SOME_VALUE>);
connectOptions.setCustomWebSocketHeaders(properties);

client.connect(connectOptions);

```
