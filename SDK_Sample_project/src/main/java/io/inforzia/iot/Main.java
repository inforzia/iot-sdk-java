package io.inforzia.iot;


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
            // iot client 생성
            DeviceClient iotClient = new DeviceClient(args[0], clientId, args[1], args[2]);
            while(true) {
                // Data 생성
                double temperature = generateRandomValue(30.0, 33.0);
                double soilMoisture = generateRandomValue(54.0, 55.0);

                // Message 생성
                String payload = String.format("{\"deviceKey\":\"env-B14\",\"temperature\":%.2f,\"soil_moisture\":%.2f}", temperature, soilMoisture);

                System.out.println("Client publish Message.");
                System.out.println(payload);
                
                // 서버로 Data 전송
                iotClient.publish(topic, payload);
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