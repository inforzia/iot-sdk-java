/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
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
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.mqttv5.client.wire;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.paho.mqttv5.client.MqttClientException;
import org.eclipse.paho.mqttv5.client.internal.MqttState;
import org.eclipse.paho.mqttv5.client.logging.Logger;
import org.eclipse.paho.mqttv5.client.logging.LoggerFactory;
import org.eclipse.paho.mqttv5.common.ExceptionHelper;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;


/**
 * An <code>MqttOutputStream</code> lets applications write instances of
 * <code>MqttWireMessage</code>. 
 */
public class MqttOutputStream extends OutputStream {
	private static final String CLASS_NAME = MqttOutputStream.class.getName();
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

	private MqttState clientState = null;
	private BufferedOutputStream out;
	
	public MqttOutputStream(MqttState clientState, OutputStream out, String clientId) {
		this.clientState = clientState;
		this.out = new BufferedOutputStream(out);
		log.setResourceName(clientId);
	}
	
	public void close() throws IOException {
		out.close();
	}
	
	public void flush() throws IOException {
		out.flush();
	}
	
	public void write(byte[] b) throws IOException {
		out.write(b);
		clientState.notifySentBytes(b.length);
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
		clientState.notifySentBytes(len);
	}
	
	public void write(int b) throws IOException {
		out.write(b);
	}

	/**
	 * Writes an <code>MqttWireMessage</code> to the stream.
	 * @param message The {@link MqttWireMessage} to send
	 * @throws IOException if an exception is thrown when writing to the output stream.
	 * @throws MqttException if an exception is thrown when getting the header or payload
	 */
	public void write(MqttWireMessage message) throws IOException, MqttException {
		final String methodName = "write";
		byte[] bytes = message.getHeader();
		byte[] pl = message.getPayload();
		if(this.clientState.getOutgoingMaximumPacketSize() != null && 
				bytes.length+pl.length > this.clientState.getOutgoingMaximumPacketSize() ) {
			// Outgoing packet is too large
			throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_OUTGOING_PACKET_TOO_LARGE);
		}
		out.write(bytes,0,bytes.length);
		clientState.notifySentBytes(bytes.length);
		
        int offset = 0;
        int chunckSize = 1024;
        while (offset < pl.length) {
        	int length = Math.min(chunckSize, pl.length - offset);
        	out.write(pl, offset, length);
        	offset += chunckSize;
        	clientState.notifySentBytes(length);
        }		
		
		// @TRACE 529= sent {0}
    	log.fine(CLASS_NAME, methodName, "529", new Object[]{message});
	}
}

