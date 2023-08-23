![Flomon](https://img.shields.io/badge/FLOMON-blue)
![JAVA](https://img.shields.io/badge/JAVA-green) 
![SDK](https://img.shields.io/badge/SDK-black)
[![GitHub](https://img.shields.io/badge/license-EPL--1.0-FF0033.svg)](https://github.com/inforzia/mqtt.java-sdk/LICENSE)
# Java Client SDK for the MQTT Protocol

The Flomon Device Client is an MQTT client library written in Java for developing applications that run on the JVM or other Java compatible platforms such as Android.
This code is forked from [![Flomon](https://img.shields.io/badge/Paho-Project-blue)](https://github.com/orgs/eclipse/repositories?q=java+mqtt&type=all&language=&sort=)![Flomon](https://img.shields.io/badge/version-1.2.5-blue).

The Flomon Device Client provides APIs: MqttClient provides a fully asynchronous API where completion of activities is notified via registered callbacks. MqttClient is a synchronous wrapper around MqttAsyncClient where functions appear synchronous to the application.

## Usage
This SDK is used by registering it as an external library. 
Go to the git repository [![SDK Repository](https://img.shields.io/badge/Inforzia%20SDK-Repo-green)](https://github.com/inforzia/mqtt.java-sdk)
```less
$ git clone https://github.com/inforzia/iot-sdk-java.git
```
and clone the IOT-SDK project. And Build it at the root directory.
```less
$ mvn clean package
```
The `IOT_SDK-1.0-SNAPSHOT-jar-with-dependencies.jar` file will be generated in the `{basedir}/target/ directory`. Create a `lib` folder in the root of your project and copy this SDK file into it.
Now add the following dependency to your project's pom.xml:

```xml
<dependency>
    <groupId>io.inforzia</groupId>
    <artifactId>iot-sdk-java</artifactId>
    <version>1.1.1</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/IOT_SDK-1.0-SNAPSHOT-jar-with-dependencies.jar</systemPath>
</dependency>
```

Then, through the build plugin, register the path to your project's Main Class.

```xml
<build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.2.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>your project's Main Class</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

## IOT Device Publish Sample Project
This `sample project` connects to the MQTT Broker of ![Flomon](https://img.shields.io/badge/Flomon-8A2BE2)(Inforzia IoT Platform) and sends data to the environmental sensor resource. To connect to Flomon and send data, the following prerequisites are required:

### Prerequisites

1. Create an environmental sensor resource in Flomon.
2. Create a Flow in Flomon to receive MQTT messages.
3. Obtain the KEY from Flomon for secure MQTT connection.

### Sample Code
```java
/*
 * Copyright 2023 Inforzia Inc. All rights reserved.
 * Inforzia PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

public class Main {
    public static void main(String[] args) {
        String topic = "flomon/telemetry";
        if (args.length < 3) {
            System.out.println("Usage: java -jar xxx.jar [brokerUrl] [username] [password]");
            return;
        }
        String clientId = "iot-client";
        try {
            // iot client create
            DeviceClient iotClient = new DeviceClient(args[0], clientId, args[1], args[2]);
            while(true) {
                // Data generate
                double temperature = generateRandomValue(30.0, 33.0);
                double soilMoisture = generateRandomValue(54.0, 55.0);

                // Message
                String payload = String.format("{\"deviceKey\":\"env-B14\",\"temperature\":%.2f,\"soil_moisture\":%.2f}", temperature, soilMoisture);

                System.out.println("Client publish Message.");
                System.out.println(payload);
                
                // Send the data to server
                iotClient.publish(topic, payload);
                // per second
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private static double generateRandomValue(double min, double max) {
        return min + Math.random() * (max - min);
    }
}
```
### Run the Sample Project

Build the project from the `root directory` of the Sample Project.
```less
$ mvn clean install
```
Now inject the class path and Main class to run (in Windows cmd environment):

```less
$ java -cp "./SDK_Sample_project-1.0-SNAPSHOT.jar;../lib/IOT_SDK-1.0-SNAPSHOT-jar-with-dependencies.jar" io.inforzia.iot.Main [brokerUrl] [username] [password]
```

### Execution Result
Temperature and soil moisture data are sent to the Flomon IoT Platform every second, and this data can be viewed in the Flomon IoT Monitoring Tab.
