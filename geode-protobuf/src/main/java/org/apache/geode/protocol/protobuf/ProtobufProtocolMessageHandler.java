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
package org.apache.geode.protocol.protobuf;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.geode.annotations.Experimental;
import org.apache.geode.internal.protocol.ClientProtocolMessageHandler;
import org.apache.geode.internal.protocol.MessageExecutionContext;
import org.apache.geode.internal.protocol.protobuf.ClientProtocol;
import org.apache.geode.internal.protocol.security.server.AuthorizationLookupService;
import org.apache.geode.internal.protocol.security.server.Authorizer;
import org.apache.geode.protocol.exception.InvalidProtocolMessageException;
import org.apache.geode.protocol.protobuf.registry.OperationContextRegistry;
import org.apache.geode.protocol.protobuf.serializer.ProtobufProtocolSerializer;
import org.apache.geode.protocol.protobuf.utilities.ProtobufUtilities;
import org.apache.geode.serialization.registry.exception.CodecAlreadyRegisteredForTypeException;

/**
 * This object handles an incoming stream containing protobuf messages. It parses the protobuf
 * messages, hands the requests to an appropriate handler, wraps the response in a protobuf message,
 * and then pushes it to the output stream.
 */
@Experimental
public class ProtobufProtocolMessageHandler implements ClientProtocolMessageHandler {
  private final ProtobufProtocolSerializer protobufProtocolSerializer;
  private final ProtobufOperationsProcessor protobufOperationProcessor;
  private final OperationContextRegistry operationContextRegistry;
  private final ProtobufSerializationService protobufSerializationService;
  private final Authorizer authorizer;

  public ProtobufProtocolMessageHandler() throws CodecAlreadyRegisteredForTypeException {
    protobufProtocolSerializer = new ProtobufProtocolSerializer();
    operationContextRegistry = new OperationContextRegistry();
    protobufSerializationService = new ProtobufSerializationService();
    authorizer = new AuthorizationLookupService().getAuthorizer();
    protobufOperationProcessor = new ProtobufOperationsProcessor(protobufSerializationService,
        operationContextRegistry, authorizer);

  }

  @Override
  public void receiveMessage(InputStream inputStream, OutputStream outputStream,
      MessageExecutionContext executionContext) throws IOException {
    try {
      ClientProtocol.Message incomingMessage = deserializeMessageFromInputStream(inputStream);
      ClientProtocol.Message outgoingMessage = processMessage(incomingMessage, executionContext);
      serializeMessageToOutputStream(outgoingMessage, outputStream);
    } catch (InvalidProtocolMessageException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String getMessageHandlerProtocolName() {
    return "PROTOBUF";
  }

  private ClientProtocol.Message processMessage(ClientProtocol.Message message,
      MessageExecutionContext executionContext) {
    ClientProtocol.Response response =
        protobufOperationProcessor.process(message.getRequest(), executionContext);
    ClientProtocol.MessageHeader responseHeader =
        ProtobufUtilities.createMessageHeaderForRequest(message);
    return ProtobufUtilities.createProtobufResponse(responseHeader, response);
  }

  private void serializeMessageToOutputStream(ClientProtocol.Message responseMessage,
      OutputStream outputStream) throws IOException {
    protobufProtocolSerializer.serialize(responseMessage, outputStream);
  }

  private ClientProtocol.Message deserializeMessageFromInputStream(InputStream inputStream)
      throws InvalidProtocolMessageException, EOFException {
    ClientProtocol.Message message = protobufProtocolSerializer.deserialize(inputStream);
    if (message == null) {
      throw new EOFException("Tried to deserialize protobuf message at EOF");
    }
    return message;
  }
}
