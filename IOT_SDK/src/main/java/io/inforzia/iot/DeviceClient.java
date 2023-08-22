package io.inforzia.iot;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/*
 * Copyright 2023 Inforzia Inc. All rights reserved.
 * Inforzia PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
public class DeviceClient {
    private MqttClient mqttClient;

    public DeviceClient(String brokerUrl, String clientId) throws MqttException {
        mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
    }
    public DeviceClient(String brokerUrl, String clientId, String username, String password) throws MqttException {
        mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setUserName(username);
        connectOptions.setPassword(password.toCharArray());
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("Connection Lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                System.out.println("MessageArrived.");
                System.out.println("Topic : " + topic);
                System.out.println("MqttMessage : " + message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                System.out.println("Message delivery Complete.");
            }
        });
        mqttClient.connect(connectOptions);
    }

    public void connect() throws MqttException {
        mqttClient.connect();
    }

    public void publish(String topic, String message) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        mqttClient.publish(topic, mqttMessage);
    }

    public void disconnect() throws MqttException {
        mqttClient.disconnect();
    }
}

