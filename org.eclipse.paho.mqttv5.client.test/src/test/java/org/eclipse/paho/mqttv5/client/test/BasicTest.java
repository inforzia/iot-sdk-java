package org.eclipse.paho.mqttv5.client.test;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.test.utilities.MqttV5Receiver;
import org.eclipse.paho.mqttv5.client.test.utilities.TestClientUtilities;
import org.eclipse.paho.mqttv5.client.test.utilities.Utility;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A series of basic connectivity tests to validate that basic functions work:
 * <ul>
 * <li>Connect</li>
 * <li>Subscribe</li>
 * <li>Publish</li>
 * <li>Unsubscribe</li>
 * <li>Disconnect</li>
 * </ul>
 *
 */
public class BasicTest {
	
	static final Class<?> cclass = BasicTest.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);

	private static URI serverURI;
	private static String topicPrefix;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			String methodName = Utility.getMethodName();
			LoggingUtilities.banner(log, cclass, methodName);

			serverURI = TestProperties.getServerURI();
			topicPrefix = "BasicTest-" + UUID.randomUUID().toString() + "-";

		} catch (Exception exception) {
			log.log(Level.SEVERE, "caught exception:", exception);
			throw exception;
		}
	}

	/**
	 * Very simple test case that validates that the client can connect and then
	 * disconnect cleanly.
	 * 
	 * @throws MqttException
	 */
	@Test
	public void testConnectAndDisconnect() throws MqttException {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		// This utility method already asserts that we are connected, so good so far
		MqttAsyncClient asyncClient = TestClientUtilities.connectAndGetClient(serverURI.toString(), methodName, null,
				null, 5000);
		// Client Should now be disconnected.
		TestClientUtilities.disconnectAndCloseClient(asyncClient, 5000);
	}

	/**
	 * Simple test case that validates that the client can Publish and Receive
	 * messages at QoS 0, 1 and 2
	 * 
	 * @throws MqttException,
	 *             InterruptedException
	 * 
	 */
	@Test
	public void testPublishAndReceive() throws MqttException, InterruptedException {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		String topic = topicPrefix + methodName;

		int timeout = 5000;

		MqttV5Receiver mqttV5Receiver = new MqttV5Receiver(methodName, LoggingUtilities.getPrintStream());
		MqttAsyncClient asyncClient = TestClientUtilities.connectAndGetClient(serverURI.toString(), methodName,
				mqttV5Receiver, null, timeout);

		for (int qos = 0; qos <= 2; qos++) {
			log.info("Testing Publish and Receive at QoS: " + qos);
			// Subscribe to a topic
			log.info(String.format("Subscribing to: %s at QoS %d", topic, qos));
			MqttSubscription subscription = new MqttSubscription(topic, qos);
			IMqttToken subscribeToken = asyncClient.subscribe(subscription);
			subscribeToken.waitForCompletion(timeout);

			// Publish a message to the topic
			String messagePayload = "Test Payload at QoS : " + qos;
			MqttMessage testMessage = new MqttMessage(messagePayload.getBytes(), qos, false, null);
			log.info(String.format("Publishing Message %s to: %s at QoS: %d", testMessage.toDebugString(), topic, qos));
			IMqttToken deliveryToken = asyncClient.publish(topic, testMessage);
			deliveryToken.waitForCompletion(timeout);

			log.info("Waiting for delivery and validating message.");
			boolean received = mqttV5Receiver.validateReceipt(topic, qos, testMessage);
			Assert.assertTrue(received);

			// Unsubscribe from the topic
			log.info("Unsubscribing from : " + topic);
			IMqttToken unsubscribeToken = asyncClient.unsubscribe(topic);
			unsubscribeToken.waitForCompletion(timeout);
		}
		TestClientUtilities.disconnectAndCloseClient(asyncClient, timeout);
	}

	/**
	 * @throws MqttException,
	 *             InterruptedException
	 * 
	 */
	@Test
	public void testQoS2DuplicateIssue() throws MqttException, InterruptedException {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		String subTopic = "a/+/c";

		int timeout = 5000;

		MqttV5Receiver mqttV5Receiver = new MqttV5Receiver(methodName, LoggingUtilities.getPrintStream());
		MqttAsyncClient asyncClient = TestClientUtilities.connectAndGetClient(serverURI.toString(), methodName,
				mqttV5Receiver, null, timeout);

		int qos = 2;
		log.info("Testing Publish and Receive at QoS: " + qos);
		// Subscribe to a topic
		log.info(String.format("Subscribing to: %s at QoS %d", subTopic, qos));
		MqttSubscription subscription = new MqttSubscription(subTopic, qos);
		IMqttToken subscribeToken = asyncClient.subscribe(subscription);
		subscribeToken.waitForCompletion(timeout);
		
		String samplePayload = "Hello World";

		log.info("Publishing messages on topics.");
		publishMessage(samplePayload, 2, "a/2/c", asyncClient, timeout);
		
		
		log.info("Waiting for delivery and validating message.");
		Assert.assertTrue(mqttV5Receiver.validateReceipt("a/2/c", 2, samplePayload.getBytes()));
		log.info("Number of received message: " + mqttV5Receiver.receivedMessageCount());
				
		

		TestClientUtilities.disconnectAndCloseClient(asyncClient, timeout);
	}

	private void publishMessage(String payload, int qos, String topic, MqttAsyncClient client, int timeout) throws MqttException {
		MqttMessage testMessage = new MqttMessage(payload.getBytes(), qos, false, null);
		log.info(String.format("Publishing Message %s to: %s at QoS: %d", testMessage.toDebugString(), topic,
				qos));
		IMqttToken deliveryToken = client.publish(topic, testMessage);
		deliveryToken.waitForCompletion(timeout);
	}
	
	  @Test
	  public void test330() throws Exception {
	    String methodName = Utility.getMethodName();
	    LoggingUtilities.banner(log, cclass, methodName);

	    int before_thread_count = Thread.activeCount();
		MqttAsyncClient client = new MqttAsyncClient("tcp://iot.eclipse.org:1882", methodName);

	    MqttConnectionOptions options = new MqttConnectionOptions();
	    options.setAutomaticReconnect(true);
	    options.setUserName("foo");
	    options.setPassword("bar".getBytes());
	    options.setConnectionTimeout(2);
	    client.connect(options);

	    Thread.sleep(1000);

	    try {
	    	  // this would deadlock before fix
	      client.disconnect(0).waitForCompletion();
	    } finally {
	      client.close();
	    }
	    
	    int after_count = Thread.activeCount();
	    Thread[] tarray = new Thread[after_count];
	    while (after_count > before_thread_count) {
	      after_count = Thread.enumerate(tarray);
	      for (int i = 0; i < after_count; ++i) {
	    	    log.info("Thread "+ i + ": " + tarray[i].getName());
	      }
	      Thread.sleep(300);
	    }
	    Assert.assertEquals(before_thread_count, after_count);
	  }

}
