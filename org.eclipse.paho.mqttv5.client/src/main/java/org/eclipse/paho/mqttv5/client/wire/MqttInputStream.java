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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

import org.eclipse.paho.mqttv5.client.MqttClientException;
import org.eclipse.paho.mqttv5.client.internal.MqttState;
import org.eclipse.paho.mqttv5.client.logging.Logger;
import org.eclipse.paho.mqttv5.client.logging.LoggerFactory;
import org.eclipse.paho.mqttv5.common.ExceptionHelper;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttDataTypes;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;


/**
 * An <code>MqttInputStream</code> lets applications read instances of
 * <code>MqttWireMessage</code>. 
 */
public class MqttInputStream extends InputStream {
	private static final String CLASS_NAME = MqttInputStream.class.getName();
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

	private MqttState clientState = null;
	private DataInputStream in;	
	private ByteArrayOutputStream bais;
	private int remLen;
	private int packetLen;
	private byte[] packet;

	public MqttInputStream(MqttState clientState, InputStream in, String clientId) {
		this.clientState = clientState;
		this.in = new DataInputStream(in);		
		this.bais = new ByteArrayOutputStream();
		this.remLen = -1;
		log.setResourceName(clientId);
	}
	
	public int read() throws IOException {
		return in.read();
	}
	
	public int available() throws IOException {
		return in.available();
	}
	
	public void close() throws IOException {
		in.close();
	}
	
	/**
	 * Reads an <code>MqttWireMessage</code> from the stream.
	 * If the message cannot be fully read within the socket read timeout,
	 * a null message is returned and the method can be called again until
	 * the message is fully read.
	 * @return The {@link MqttWireMessage}
	 * @throws IOException if an exception is thrown when reading from the stream
	 * @throws MqttException if the message is invalid 
	 */
	public MqttWireMessage readMqttWireMessage() throws IOException, MqttException {
		final String methodName ="readMqttWireMessage";
		
		MqttWireMessage message = null;
		try {
			// read header
			if (remLen < 0) {
				// Assume we can read the whole header at once.
				// The header is very small so it's likely we
				// are able to read it fully or not at all.
				// This keeps the parser lean since we don't
				// need to cope with a partial header.
				// Should we lose synch with the stream,
				// the keepalive mechanism would kick in
				// closing the connection.
				bais.reset();
				
				byte first = in.readByte();
				clientState.notifyReceivedBytes(1);

				byte type = (byte) ((first >>> 4) & 0x0F);
				if ((type < MqttWireMessage.MESSAGE_TYPE_CONNECT) ||
						(type > MqttWireMessage.MESSAGE_TYPE_AUTH)) {
					// Invalid MQTT message type...
					throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_INVALID_MESSAGE);
				}
				
				byte reserved = (byte) (first & 0x0F);
				MqttWireMessage.validateReservedBits(type, reserved);
				
				remLen = MqttDataTypes.readVariableByteInteger(in).getValue();
				bais.write(first);
				bais.write(MqttWireMessage.encodeVariableByteInteger((int)remLen));
				packet = new byte[(int)(bais.size()+remLen)];
				if(this.clientState.getIncomingMaximumPacketSize() != null && 
						bais.size()+remLen > this.clientState.getIncomingMaximumPacketSize() ) {
					// Incoming packet is too large
					throw ExceptionHelper.createMqttException(MqttClientException.REASON_CODE_INCOMING_PACKET_TOO_LARGE);
				}
				packetLen = 0;
			}
			
			// read remaining packet
			if (remLen >= 0) {
				// the remaining packet can be read with timeouts
				readFully();

				// reset packet parsing state 
				remLen = -1;
				
				byte[] header = bais.toByteArray();
				System.arraycopy(header,0,packet,0, header.length);
				message = MqttWireMessage.createWireMessage(packet);
				// @TRACE 530= Received {0} 
				log.fine(CLASS_NAME, methodName, "530",new Object[] {message});
			}
		} catch (SocketTimeoutException e) {
			// ignore socket read timeout
		}
		
		return message;
	}
	
    private void readFully() throws IOException {
    	int off = bais.size() + (int) packetLen;
    	int len = (int) (remLen - packetLen);
    	if (len < 0)
    		throw new IndexOutOfBoundsException();
    	int n = 0;
    	while (n < len) {
    		int count = -1;
    		try {
    			count = in.read(packet, off + n, len - n);
    		} catch (SocketTimeoutException e) {
    			// remember the packet read so far 
    			packetLen += n;
    			throw e;
    		}
    		

    		if (count < 0) {
    			throw new EOFException();
    		}
    		clientState.notifyReceivedBytes(count);
    		n += count;
    	}
    }
}
