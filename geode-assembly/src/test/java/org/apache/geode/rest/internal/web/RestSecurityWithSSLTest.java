/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.geode.rest.internal.web;

import static org.apache.geode.distributed.ConfigurationProperties.SECURITY_MANAGER;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_ENABLED_COMPONENTS;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_KEYSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_KEYSTORE_PASSWORD;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_KEYSTORE_TYPE;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_PROTOCOLS;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_TRUSTSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.SSL_TRUSTSTORE_PASSWORD;
import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.apache.http.HttpResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.internal.security.SecurableCommunicationChannel;
import org.apache.geode.security.SimpleTestSecurityManager;
import org.apache.geode.test.junit.categories.IntegrationTest;
import org.apache.geode.test.junit.categories.RestAPITest;
import org.apache.geode.test.junit.categories.SecurityTest;
import org.apache.geode.test.junit.rules.RequiresGeodeHome;
import org.apache.geode.test.junit.rules.ServerStarterRule;

@Category({IntegrationTest.class, SecurityTest.class, RestAPITest.class})
public class RestSecurityWithSSLTest {

  private static final URL KEYSTORE_URL =
      RestSecurityWithSSLTest.class.getClassLoader().getResource("ssl/trusted.keystore");

  @Rule
  public RequiresGeodeHome requiresGeodeHome = new RequiresGeodeHome();

  @Rule
  public ServerStarterRule serverStarter = new ServerStarterRule().withRestService()
      .withProperty(SECURITY_MANAGER, SimpleTestSecurityManager.class.getName())
      .withProperty(SSL_ENABLED_COMPONENTS, SecurableCommunicationChannel.WEB.getConstant())
      .withProperty(SSL_KEYSTORE, KEYSTORE_URL.getPath())
      .withProperty(SSL_KEYSTORE_PASSWORD, "password").withProperty(SSL_KEYSTORE_TYPE, "JKS")
      .withProperty(SSL_TRUSTSTORE, KEYSTORE_URL.getPath())
      .withProperty(SSL_TRUSTSTORE_PASSWORD, "password")
      .withProperty(SSL_PROTOCOLS, "TLSv1.2,TLSv1.1").withAutoStart();

  @Test
  public void testRestSecurityWithSSL() throws Exception {
    GeodeRestClient restClient =
        new GeodeRestClient("localhost", serverStarter.getHttpPort(), true);
    HttpResponse response = restClient.doGet("/servers", "cluster", "cluster");

    assertEquals(200, GeodeRestClient.getCode(response));
  }
}
