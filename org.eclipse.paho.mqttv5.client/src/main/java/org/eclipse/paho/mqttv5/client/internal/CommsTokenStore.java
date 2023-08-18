/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corp.
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
package org.eclipse.paho.mqttv5.client.internal;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.client.logging.Logger;
import org.eclipse.paho.mqttv5.client.logging.LoggerFactory;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttPublish;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;


/**
 * Provides a "token" based system for storing and tracking actions across 
 * multiple threads. 
 * When a message is sent, a token is associated with the message
 * and saved using the {@link CommsTokenStore#saveToken(MqttToken, MqttWireMessage)} method. Anyone interested
 * in tacking the state can call one of the wait methods on the token or using 
 * the asynchronous listener callback method on the operation. 
 * The {@link CommsReceiver} class, on another thread, reads responses back from 
 * the network. It uses the response to find the relevant token, which it can then 
 * notify. 
 * 
 * Note:
 *   Ping, connect and disconnect do not have a unique message id as
 *   only one outstanding request of each type is allowed to be outstanding
 */
public class CommsTokenStore {
	private static final String CLASS_NAME = CommsTokenStore.class.getName();
	private Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

	// Maps message-specific data (usually message IDs) to tokens
	private final Hashtable<String, MqttToken> tokens;
	private String logContext;
	private MqttException closedResponse = null;

	public CommsTokenStore(String logContext) {
		final String methodName = "<Init>";

		log.setResourceName(logContext);
		this.tokens = new Hashtable<String, MqttToken>();
		this.logContext = logContext;
		//@TRACE 308=<>
		log.fine(CLASS_NAME,methodName,"308");//,new Object[]{message});

	}

	/**
	 * Based on the message type that has just been received return the associated
	 * token from the token store or null if one does not exist.
	 * @param message whose token is to be returned 
	 * @return token for the requested message
	 */
	public MqttToken getToken(MqttWireMessage message) {
		String key = message.getKey(); 
		return (MqttToken)tokens.get(key);
	}

	public MqttToken getToken(String key) {
		return (MqttToken)tokens.get(key);
	}

	
	public MqttToken removeToken(MqttWireMessage message) {
		if (message != null) {
			return removeToken(message.getKey());
		}
		return null;
	}
	
	public MqttToken removeToken(String key) {
		final String methodName = "removeToken";
		//@TRACE 306=key={0}
		log.fine(CLASS_NAME,methodName,"306",new Object[]{key});
		
		if ( null != key ){
		    return (MqttToken) tokens.remove(key);
		}
		
		return null;
	}
		
	/**
	 * Restores a token after a client restart.  This method could be called
	 * for a SEND of CONFIRM, but either way, the original SEND is what's 
	 * needed to re-build the token.
	 * @param message The {@link MqttPublish} message to restore
	 * @return {@link MqttToken}
	 */
	protected MqttToken restoreToken(MqttPublish message) {
		final String methodName = "restoreToken";
		MqttToken token;
		synchronized(tokens) {
			String key = Integer.valueOf(message.getMessageId()).toString();
			if (this.tokens.containsKey(key)) {
				token = this.tokens.get(key);
				//@TRACE 302=existing key={0} message={1} token={2}
				log.fine(CLASS_NAME,methodName, "302",new Object[]{key, message,token});
			} else {
				token = new MqttToken(logContext);
                                token.internalTok.setDeliveryToken(true);
				token.internalTok.setKey(key);
				this.tokens.put(key, token);
				//@TRACE 303=creating new token key={0} message={1} token={2}
				log.fine(CLASS_NAME,methodName,"303",new Object[]{key, message, token});
			}
		}
		return token;
	}
	
	// For outbound messages store the token in the token store 
	// For pubrel use the existing publish token 
	protected void saveToken(MqttToken token, MqttWireMessage message) throws MqttException {
		final String methodName = "saveToken";

		synchronized(tokens) {
			if (closedResponse == null) {
				String key = message.getKey();
				//@TRACE 300=key={0} message={1}
				log.fine(CLASS_NAME,methodName,"300",new Object[]{key, message});
				
				saveToken(token,key);
			} else {
				throw closedResponse;
			}
		}
	}
	
	protected void saveToken(MqttToken token, String key) {
		final String methodName = "saveToken";

		synchronized(tokens) {
			//@TRACE 307=key={0} token={1}
			log.fine(CLASS_NAME,methodName,"307",new Object[]{key,token.toString()});
			token.internalTok.setKey(key);
			this.tokens.put(key, token);
		}
	}

	protected void quiesce(MqttException quiesceResponse) {
		final String methodName = "quiesce";

		synchronized(tokens) {
			//@TRACE 309=resp={0}
			log.fine(CLASS_NAME,methodName,"309",new Object[]{quiesceResponse});

			closedResponse = quiesceResponse;
		}
	}
	
	public void open() {
		final String methodName = "open";

		synchronized(tokens) {
			//@TRACE 310=>
			log.fine(CLASS_NAME,methodName,"310");

			closedResponse = null;
		}
	}

	public MqttToken[] getOutstandingDelTokens() {
		final String methodName = "getOutstandingDelTokens";

		synchronized(tokens) {
			//@TRACE 311=>
			log.fine(CLASS_NAME,methodName,"311");

			Vector<MqttToken> list = new Vector<MqttToken>();
			Enumeration<MqttToken> enumeration = tokens.elements();
			MqttToken token;
			while(enumeration.hasMoreElements()) {
				token = (MqttToken)enumeration.nextElement();
				if (token != null 
					&& token.internalTok.isDeliveryToken() == true
					&& !token.internalTok.isNotified()) {
					
					list.addElement(token);
				}
			}
	
			MqttToken[] result = new MqttToken[list.size()];
			return (MqttToken[]) list.toArray(result);
		}
	}
	
	public Vector<MqttToken> getOutstandingTokens() {
		final String methodName = "getOutstandingTokens";

		synchronized(tokens) {
			//@TRACE 312=>
			log.fine(CLASS_NAME,methodName,"312");

			Vector<MqttToken> list = new Vector<MqttToken>();
			Enumeration<MqttToken> enumeration = tokens.elements();
			MqttToken token;
			while(enumeration.hasMoreElements()) {
				token = (MqttToken)enumeration.nextElement();
				if (token != null) {
						list.addElement(token);
				}
			}
			return list;
		}
	}

	/**
	 * Empties the token store without notifying any of the tokens.
	 */
	public void clear() {
		final String methodName = "clear";
		//@TRACE 305=> {0} tokens
		log.fine(CLASS_NAME, methodName, "305", new Object[] { Integer.valueOf(tokens.size())});
		synchronized(tokens) {
			tokens.clear();
		}
	}
	
	public int count() {
		synchronized(tokens) {
			return tokens.size();
		}
	}
	public String toString() {
		String lineSep = System.getProperty("line.separator","\n");
		StringBuffer toks = new StringBuffer();
		synchronized(tokens) {
			Enumeration<MqttToken> enumeration = tokens.elements();
			MqttToken token;
			while(enumeration.hasMoreElements()) {
				token = (MqttToken)enumeration.nextElement();
					toks.append("{"+token.internalTok+"}"+lineSep);
			}
			return toks.toString();
		}
	}
}
