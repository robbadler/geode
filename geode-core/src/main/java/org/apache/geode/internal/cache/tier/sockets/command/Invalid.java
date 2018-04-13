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
package org.apache.geode.internal.cache.tier.sockets.command;

import java.io.IOException;

import org.apache.geode.internal.cache.tier.Command;
import org.apache.geode.internal.cache.tier.MessageType;
import org.apache.geode.internal.cache.tier.sockets.*;
import org.apache.geode.internal.i18n.LocalizedStrings;
import org.apache.geode.internal.logging.log4j.LocalizedMessage;
import org.apache.geode.internal.security.SecurityService;

public class Invalid extends BaseCommand {

  private static final Invalid singleton = new Invalid();

  public static Command getCommand() {
    return singleton;
  }

  private Invalid() {}

  @Override
  public void cmdExecute(final Message clientMessage, final ServerConnection serverConnection,
      final SecurityService securityService, long start) throws IOException {
    logger.error(
        LocalizedMessage.create(LocalizedStrings.Invalid_0_INVALID_MESSAGE_TYPE_WITH_TX_1_FROM_2,
            new Object[] {serverConnection.getName(),
                Integer.valueOf(clientMessage.getTransactionId()),
                serverConnection.getSocketString()}));
    writeErrorResponse(clientMessage, MessageType.INVALID, serverConnection);

  }

}
