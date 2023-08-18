/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corp.
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
 *******************************************************************************/

package org.eclipse.paho.client.mqttv3.test.connectionLoss;

import java.net.URI;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.ConnectionManipulationProxyServer;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * These tests verify whether paho can successfully detect a loss of connection with a broker.
 * 
 * >> The tests MUST BE run manually and they WILL FAIL in an automated test environment. <<
 * The pom.xml of the test project, all tests categorized as ManualTests are excluded from the execution.  
 * 
 * The tests will print a messages in the console notifying the operator when to "unplug" the internet connection of the test machine.   
 * 
 * @author mcarrer
 */

public class ConnectionLossTest implements MqttCallback
{
	static final Class<?> cclass = ConnectionLossTest.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);

	private static final MqttDefaultFilePersistence DATA_STORE = new MqttDefaultFilePersistence("/tmp");

	private String  username = "username";
	private char[]  password = "password".toCharArray();
	private String  clientId = "device-client-id";
	private String  message  = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";

	static ConnectionManipulationProxyServer proxy;
	private static URI serverURI;


	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		try {
			String methodName = Utility.getMethodName();
			LoggingUtilities.banner(log, cclass, methodName);
			serverURI = TestProperties.getServerURI();
			// Use 0 for the first time.
			proxy = new ConnectionManipulationProxyServer(serverURI.getHost(), serverURI.getPort(), 0);
			proxy.startProxy();
			while(!proxy.isPortSet()){
				Thread.sleep(0);
			}
			log.log(Level.INFO, "Proxy Started, port set to: " + proxy.getLocalPort());
		} catch (Exception exception) {
		      log.log(Level.SEVERE, "caught exception:", exception);
		      throw exception;
		    }
		
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log.info("Tests finished, stopping proxy");
		proxy.stopProxy();
	}
	
	@After
	public void afterTest() {
		log.info("Disabling Proxy");
		proxy.disableProxy();
	}
	
	/**
	 * Tests whether paho can detect a connection loss with the server even if it has outbound activity by publishing messages with QoS 0.
	 * @throws Exception
	 */
	@Test
	public void testConnectionLossWhilePublishingQos0()
		throws Exception
	{
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		final int keepAlive = 15;

		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setUserName(username);
		options.setPassword(password);
		options.setKeepAliveInterval(keepAlive);

		MqttClient client = new MqttClient("tcp://localhost:" + proxy.getLocalPort(), clientId, DATA_STORE);
		client.setCallback(this);
		proxy.enableProxy();
		client.connect(options);

		log.info((new Date())+" - Connected.");
		for (int i=0; i<10; i++) {    		
			log.info("Disconnect your network in "+(10-i)+" sec...");
			client.publish(username+"/"+clientId+"/abc", message.getBytes(), 0, false);
			Thread.sleep(1000);    		
		}
		proxy.disableProxy();

		final int[] res = new int[1];
		new Timer().schedule( new TimerTask() {
			@Override
			public void run() {
				res[0]++;
				if (res[0] == keepAlive + 1) {
					log.info((new Date())+" - Connection should be lost...");
				}				
			}    		
		}, 0, 1000);

		while (client.isConnected() && res[0] < 2*keepAlive) {
			try {
				client.publish(username+"/"+clientId+"/abc", message.getBytes(), 0, false);
				Thread.sleep(1000);
			}
			catch (MqttException e) {
				// ignore
			}
		}

		Assert.assertFalse("Disconected", client.isConnected());
		if (client.isConnected()) client.disconnect(0);
		client.close();
	}

	
	/**
	 * Tests whether paho can detect a connection loss with the server even if it has outbound activity by publishing messages with QoS 1.
	 * @throws Exception
	 */
	@Test
	public void testConnectionLossWhilePublishingQos1() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);

		final int keepAlive = 15;

		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setUserName(username);
		options.setPassword(password);
		options.setKeepAliveInterval(keepAlive);

		MqttClient client = new MqttClient("tcp://localhost:" + proxy.getLocalPort(), clientId, DATA_STORE);
		client.setCallback(this);
		proxy.enableProxy();
		client.connect(options);

		log.info((new Date()) + " - Connected.");
		for (int i = 0; i < 10; i++) {
			log.info("Disconnect your network in " + (10 - i) + " sec...");
			client.publish(username + "/" + clientId + "/abc", message.getBytes(), 1, false);
			Thread.sleep(1000);
		}
		proxy.disableProxy();

		final int[] res = new int[1];
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				res[0]++;
				if (res[0] == keepAlive + 1) {
					log.info((new Date()) + " - Connection should be lost...");
				}
			}
		}, 0, 1000);

		while (client.isConnected() && res[0] < 2 * keepAlive) {
			try {
				client.publish(username + "/" + clientId + "/abc", message.getBytes(), 1, false);
				Thread.sleep(1000);
			} catch (MqttException e) {
				// ignore
			}
		}
		log.info("Finished publishing...");

		Assert.assertFalse("Disconected", client.isConnected());
		if (client.isConnected())
			client.disconnect(0);
		client.close();
	}

	/**
	 * Tests whether paho can detect a connection loss with the server even if it has outbound activity by publishing messages with QoS 2.
	 * @throws Exception
	 */
	@Test
	public void testConnectionLossWhilePublishingQos2()
		throws Exception
	{
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		final int keepAlive = 15;

		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setUserName(username);
		options.setPassword(password);
		options.setKeepAliveInterval(keepAlive);

		MqttClient client = new MqttClient("tcp://localhost:" + proxy.getLocalPort(), clientId, DATA_STORE);
		client.setCallback(this);
		proxy.enableProxy();
		client.connect(options);

		log.info((new Date())+" - Connected.");
		for (int i=0; i<10; i++) {    		
			log.info("Disconnect your network in "+(10-i)+" sec...");
			client.publish(username+"/"+clientId+"/abc", message.getBytes(), 2, false);
			Thread.sleep(1000);    		
		}
		proxy.disableProxy();

		final int[] res = new int[1];
		new Timer().schedule( new TimerTask() {
			@Override
			public void run() {
				res[0]++;
				if (res[0] == keepAlive + 1) {
					log.info((new Date())+" - Connection should be lost...");
				}				
			}    		
		}, 0, 1000);

		while (client.isConnected() && res[0] < 2*keepAlive) {
			try {
				client.publish(username+"/"+clientId+"/abc", message.getBytes(), 2, false);
				Thread.sleep(1000);
			}
			catch (MqttException e) {
				// ignore
			}    			
		}

		Assert.assertFalse("Disconected", client.isConnected());
		if (client.isConnected()) client.disconnect(0);
		client.close();
	}
	

	/**
	 * Tests whether async paho can detect a connection loss with the server even if it has outbound activity by publishing messages with QoS 1.
	 * @throws Exception
	 */
	@Test
	public void testConnectionLossWhilePublishingQos1Async()
		throws Exception
	{
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		final int keepAlive = 15;

		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setUserName(username);
		options.setPassword(password);
		options.setKeepAliveInterval(keepAlive);

		MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:" + proxy.getLocalPort(), clientId, DATA_STORE);
		client.setCallback(this);
		proxy.enableProxy();
		log.info((new Date())+" - Connecting...");
		client.connect(options);
		while (!client.isConnected()) {
			Thread.sleep(1000);
		}

		log.info((new Date())+" - Connected.");
		for (int i=0; i<10; i++) {    		
			log.info("Disconnect your network in "+(10-i)+" sec...");
			client.publish(username+"/"+clientId+"/abc", message.getBytes(), 1, false);
			Thread.sleep(1000);    		
		}
		proxy.disableProxy();
		final int[] res = new int[1];
		new Timer().schedule( new TimerTask() {
			@Override
			public void run() {
				res[0]++;
				if (res[0] == keepAlive + 1) {
					log.info((new Date())+" - Connection should be lost...");
				}				
			}    		
		}, 0, 1000);

		boolean stopPublishing = false;
		while (client.isConnected() && res[0] < 10*keepAlive) {
			if (!stopPublishing) {
				try {
					client.publish(username+"/"+clientId+"/abc", message.getBytes(), 1, false);
					log.info((new Date())+" - Published...");
					Thread.sleep(1000);
				}
				catch (MqttException e) {
					stopPublishing = true; 
				}
			}
		}

		Assert.assertFalse("Disconected", client.isConnected());
		if (client.isConnected()) client.disconnect(0);
		client.close();
	}
	
	
	/**
	 * Tests whether paho keeps the connection alive for 10 keep alive intervals while publishing messages with QoS 0.
	 * @throws Exception
	 */
	@Test
	public void testKeepConnectionOpenWhilePublishingQos0()
			throws Exception
	{
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		final int keepAlive = 15;

		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setUserName(username);
		options.setPassword(password);
		options.setKeepAliveInterval(keepAlive);

		MqttClient client = new MqttClient("tcp://localhost:" + proxy.getLocalPort(), clientId, DATA_STORE);
		client.setCallback(this);
		proxy.enableProxy();
		client.connect(options);

		log.info((new Date())+" - Connected.");

		final int[] res = new int[1];
		new Timer().schedule( new TimerTask() {
			@Override
			public void run() {
				res[0]++;
				if (res[0] % keepAlive == 0) {
					log.info((new Date())+" - Still running keep alive count: "+res[0]+"...");
				}				
			}    		
		}, 0, 1000);

		while (client.isConnected() && res[0] < 10*keepAlive) {    		
			client.publish(username+"/"+clientId+"/abc", message.getBytes(), 0, false);
			Thread.sleep(1000);    		
		}

		Assert.assertTrue("Connected", client.isConnected());
		if (client.isConnected()) client.disconnect(0);
		client.close();
	}

	
	/**
	 * Tests whether paho keeps the connection alive for 10 keep alive in idle state.
	 * @throws Exception
	 */
	@Test
	public void testKeepConnectionOpenIdle()
			throws Exception
	{
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		final int keepAlive = 15;

		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setUserName(username);
		options.setPassword(password);
		options.setKeepAliveInterval(keepAlive);

		MqttClient client = new MqttClient("tcp://localhost:" + proxy.getLocalPort(), clientId, DATA_STORE);
		client.setCallback(this);
		proxy.enableProxy();
		client.connect(options);

		log.info((new Date())+" - Connected.");

		final int[] res = new int[1];
		new Timer().schedule( new TimerTask() {
			@Override
			public void run() {
				res[0]++;
				if (res[0] % keepAlive == 0) {
					log.info((new Date())+" - Still running keep alive count: "+res[0]+"...");
				}				
			}    		
		}, 0, 1000);

		while (client.isConnected() && res[0] < 10*keepAlive) {    		
			Thread.sleep(1000);    		
		}

		Assert.assertTrue("Connected", client.isConnected());
		if (client.isConnected()) client.disconnect(0);
		client.close();
	}
	
	private boolean connectionLostCalled = false;
	
	public void connectionLost(Throwable cause) {
		log.info((new Date())+" - Connection Lost");
		connectionLostCalled = true;
	}

	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.info("Message Arrived on " + topic + " with " + new String(message.getPayload()));
	}

	public void deliveryComplete(IMqttDeliveryToken token) {
//		log.info("Delivery Complete: " + token.getMessageId());
	}
	
	/**
	 * Tests whether paho can detect a connection loss with the server with no publishing - using ping only
	 * @throws Exception
	 */
	@Test
	public void testConnectionLossUsingPing()
		throws Exception
	{
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		final int keepAlive = 15;

		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setUserName(username);
		options.setPassword(password);
		options.setKeepAliveInterval(keepAlive);

		connectionLostCalled = false;
		MqttClient client = new MqttClient("tcp://localhost:" + proxy.getLocalPort(), clientId, DATA_STORE);
		client.setCallback(this);
		proxy.enableProxy();
		client.connect(options);

		log.info((new Date())+" - Connected.");
		Thread.sleep(1000);    		
		proxy.disableProxy();

		int count = 0;
		while (client.isConnected() && ++count < 2*keepAlive) {
			try {
				Thread.sleep(1000);
			}
			catch (Exception e) {
				// ignore
			}
		}

		Assert.assertTrue("Connection lost was not called", connectionLostCalled);
		Assert.assertFalse("Disconnected", client.isConnected());
		if (client.isConnected()) client.disconnect(0);
		client.close();
	}

}
