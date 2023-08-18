/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at 
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 * Contributors:
 * 	  Dave Locke   - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.common.packet;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.Assert;
import org.junit.Test;

public class MqttPubCompTest {
	private static final int returnCode = MqttReturnCode.RETURN_CODE_PACKET_ID_NOT_FOUND;
	private static final String reasonString = "Reason String 123.";
	private static final String userKey1 = "userKey1";
	private static final String userKey2 = "userKey2";
	private static final String userKey3 = "userKey3";
	private static final String userValue1 = "userValue1";
	private static final String userValue2 = "userValue2";
	private static final String userValue3 = "userValue3";
	
	@Test
	public void testEncodingMqttPubComp() throws MqttException {
		MqttPubComp mqttPubCompPacket = generateMqttPubCompPacket();
		mqttPubCompPacket.getHeader();
		mqttPubCompPacket.getPayload();
	}
	
	@Test
	public void testDecodingMqttPubComp() throws MqttException, IOException {
		MqttPubComp mqttPubCompPacket = generateMqttPubCompPacket();
		byte[] header = mqttPubCompPacket.getHeader();
		byte[] payload = mqttPubCompPacket.getPayload();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		
		MqttPubComp decodedPubCompPacket = (MqttPubComp) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		MqttProperties properties = decodedPubCompPacket.getProperties();
		
		Assert.assertEquals(returnCode, decodedPubCompPacket.getReturnCode());
		Assert.assertEquals(reasonString, properties.getReasonString());
		Assert.assertTrue(new UserProperty(userKey1, userValue1).equals(properties.getUserProperties().get(0)));
		Assert.assertTrue(new UserProperty(userKey2, userValue2).equals(properties.getUserProperties().get(1)));
		Assert.assertTrue(new UserProperty(userKey3, userValue3).equals(properties.getUserProperties().get(2)));
	}
	
	@Test
	public void testDecodingSmallMqttPubcomp() throws MqttException, IOException {
		MqttPubComp mqttPubackPacket = new MqttPubComp(MqttReturnCode.RETURN_CODE_PACKET_ID_NOT_FOUND, 1, new MqttProperties());
		byte[] header = mqttPubackPacket.getHeader();
		byte[] payload = mqttPubackPacket.getPayload();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		// Total Packet should be 5 bytes long
		Assert.assertEquals(5, outputStream.size());
		MqttPubComp decodedPubackPacket = (MqttPubComp) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		Assert.assertEquals(MqttReturnCode.RETURN_CODE_PACKET_ID_NOT_FOUND, decodedPubackPacket.getReturnCode());
	
	}
	
	@Test
	public void testDecodingVerySmallMqttPubcomp() throws MqttException, IOException {
		MqttPubComp mqttPubackPacket = new MqttPubComp(MqttReturnCode.RETURN_CODE_SUCCESS, 1, new MqttProperties());
		byte[] header = mqttPubackPacket.getHeader();
		byte[] payload = mqttPubackPacket.getPayload();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(header);
		outputStream.write(payload);
		// Total Packet should be 4 bytes long
		Assert.assertEquals(4, outputStream.size());
		MqttPubComp decodedPubackPacket = (MqttPubComp) MqttWireMessage.createWireMessage(outputStream.toByteArray());
		Assert.assertEquals(MqttReturnCode.RETURN_CODE_SUCCESS, decodedPubackPacket.getReturnCode());
	}
	
	public MqttPubComp generateMqttPubCompPacket() throws MqttException{
		MqttProperties properties = new MqttProperties();
		
		properties.setReasonString(reasonString);
		ArrayList<UserProperty> userDefinedProperties = new ArrayList<UserProperty>();
		userDefinedProperties.add(new UserProperty(userKey1, userValue1));
		userDefinedProperties.add(new UserProperty(userKey2, userValue2));
		userDefinedProperties.add(new UserProperty(userKey3, userValue3));
		properties.setUserProperties(userDefinedProperties);
		
		MqttPubComp mqttPubCompPacket = new MqttPubComp(returnCode,1, properties);
		
		return mqttPubCompPacket;
	}

}
