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
package org.apache.geode.protocol.protobuf.security.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.geode.internal.protocol.protobuf.AuthenticationAPI;
import org.apache.geode.internal.protocol.security.server.Authenticator;
import org.apache.geode.management.internal.security.ResourceConstants;
import org.apache.geode.security.AuthenticationFailedException;
import org.apache.geode.security.SecurityManager;

public class ProtobufSimpleAuthenticator implements Authenticator {
  private boolean authenticationSuccessfull = false;
  private Object principal;

  @Override
  public Object authenticate(InputStream inputStream, OutputStream outputStream,
      SecurityManager securityManager) throws IOException {
    if (authenticationSuccessfull) {
      return principal;
    } else {
      AuthenticationAPI.SimpleAuthenticationRequest authenticationRequest =
          AuthenticationAPI.SimpleAuthenticationRequest.parseDelimitedFrom(inputStream);
      if (authenticationRequest == null) {
        // TODO Why are we returning and EOF here?
        throw new EOFException();
      }

      Properties properties = new Properties();
      properties.setProperty(ResourceConstants.USER_NAME, authenticationRequest.getUsername());
      properties.setProperty(ResourceConstants.PASSWORD, authenticationRequest.getPassword());

      try {
        principal = securityManager.authenticate(properties);
        authenticationSuccessfull = true;
        AuthenticationAPI.SimpleAuthenticationResponse.newBuilder()
            .setAuthenticated(isAuthenticated()).build().writeDelimitedTo(outputStream);
        return principal;
      } catch (AuthenticationFailedException e) {
        // We need to add some logging here;
        // logger.error(e);
        AuthenticationAPI.SimpleAuthenticationResponse.newBuilder().setAuthenticated(false).build()
            .writeDelimitedTo(outputStream);
        throw e;
      }
    }
  }

  @Override
  public boolean isAuthenticated() {
    return authenticationSuccessfull;
  }

  @Override
  public String getImplementationID() {
    return "SIMPLE";
  }
}
