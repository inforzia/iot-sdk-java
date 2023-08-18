package org.eclipse.paho.mqttv5.client.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.test.utilities.TestMemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.Assert;
import org.junit.Test;

public class ServerAssignedClientIdentifierTest {
	private static URI serverURI;
	private static final String className = ServerAssignedClientIdentifierTest.class.getName();
	private static final Logger log = Logger.getLogger(className);

	@Test
	/**
	 * Tests that an MqttV5 client can connect to a broker with a null client
	 * identifier. The broker should then return a new client identifier in the
	 * CONNACK packet.
	 */
	public void connectWithNullClientIdentifier() throws URISyntaxException, MqttException, InterruptedException {
		serverURI = TestProperties.getServerURI();

		TestMemoryPersistence memoryPersistence = new TestMemoryPersistence();

		// Create an MqttAsyncClient with a null Client ID.
		MqttAsyncClient client = new MqttAsyncClient(serverURI.toString(), null, memoryPersistence, null, null);

		IMqttToken connectToken = client.connect();
		connectToken.waitForCompletion(1000);
		Assert.assertTrue("The client should be connected.", client.isConnected());

		MqttProperties connectProperties = connectToken.getResponse().getProperties();
		String assignedClientIdentifier = connectProperties.getAssignedClientIdentifier();
		log.info("Server assigned client identifer: " + assignedClientIdentifier);

		// First, validate that we actually received an Assigned Client Identifier from
		// the Server
		Assert.assertNotNull("AssignedClilent Identifier should not be null.", assignedClientIdentifier);

		// Validate that the Client now knows about it's own Client ID;
		Assert.assertEquals("The client should have the correct ID set.", assignedClientIdentifier,
				client.getClientId());

		// Cleanup
		IMqttToken disconnectToken = client.disconnect();
		disconnectToken.waitForCompletion(1000);
		Assert.assertFalse("The client should now be disconnected.", client.isConnected());
		client.close();
	}

	
}
