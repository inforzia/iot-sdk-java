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
 *******************************************************************************/

package org.eclipse.paho.mqttv5.client.test.client;

import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;


/**
 *
 */
public class MqttAsyncClientPaho extends MqttAsyncClient {

  /**
   * @param serverURI
   * @param clientId
   * @throws MqttException
   */
  public MqttAsyncClientPaho(String serverURI, String clientId) throws MqttException {
    super(serverURI, clientId);
  }

  /**
   * @param serverURI
   * @param clientId
   * @param persistence
   * @throws MqttException 
   */
  public MqttAsyncClientPaho(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
    super(serverURI, clientId, persistence);
  }

  /**
   * @throws Exception 
   */
  public void startTrace() throws Exception {
    // not implemented
  }

  /**
   * @throws Exception 
   */
  public void stopTrace() throws Exception {
    // not implemented
  }

  /**
   * @return trace buffer
   * @throws Exception 
   */
  public String getTraceLog() throws Exception {
    return null;
  }
}
