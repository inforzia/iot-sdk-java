package io.inforzia.iot;


/*
 * Copyright 2023 Inforzia Inc. All rights reserved.
 * Inforzia PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

public class Main {
    public static void main(String[] args) {
//        if (args.length < 3) {
//            System.out.println("Usage: java -jar xxx.jar [brokerUrl] [username] [password]");
//            return;
//        }
        String brokerUrl = "tcp://223.130.133.182:1883";
        String clientId = "sdk-client";
        try {
//            DeviceClient mqttSdkClient = new DeviceClient(args[0], clientId, args[1], args[2]);
            DeviceClient mqttSdkClient = new DeviceClient(brokerUrl, clientId, "mdbfvbbyn7fz0kd8", "b83a5ae77cc9cc5ba77cb2e6ec2b3602");
            String topic = "flomon/sdk/test";
            String message = "Hello, Flomon MQTT!";
            mqttSdkClient.publish(topic, message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}