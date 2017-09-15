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
package org.apache.geode.protocol.protobuf.operations;

import org.apache.geode.annotations.Experimental;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.PartitionedRegionStorageException;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.TimeoutException;
import org.apache.geode.internal.protocol.MessageExecutionContext;
import org.apache.geode.internal.exception.InvalidExecutionContextException;
import org.apache.geode.internal.protocol.protobuf.BasicTypes;
import org.apache.geode.internal.protocol.protobuf.RegionAPI;
import org.apache.geode.protocol.operations.OperationHandler;
import org.apache.geode.protocol.responses.Failure;
import org.apache.geode.protocol.protobuf.ProtocolErrorCode;
import org.apache.geode.protocol.responses.Result;
import org.apache.geode.protocol.responses.Success;
import org.apache.geode.protocol.protobuf.utilities.ProtobufResponseUtilities;
import org.apache.geode.protocol.protobuf.utilities.ProtobufUtilities;
import org.apache.geode.serialization.SerializationService;
import org.apache.geode.serialization.exception.UnsupportedEncodingTypeException;
import org.apache.geode.serialization.registry.exception.CodecNotRegisteredForTypeException;

@Experimental
public class GetAllRequestOperationHandler
    implements OperationHandler<RegionAPI.GetAllRequest, RegionAPI.GetAllResponse> {

  @Override
  public Result<RegionAPI.GetAllResponse> process(SerializationService serializationService,
      RegionAPI.GetAllRequest request, MessageExecutionContext executionContext)
      throws InvalidExecutionContextException {
    String regionName = request.getRegionName();
    Region region = executionContext.getCache().getRegion(regionName);
    if (region == null) {
      return Failure.of(ProtobufResponseUtilities
          .makeErrorResponse(ProtocolErrorCode.REGION_NOT_FOUND.codeValue, "Region not found"));
    }

    RegionAPI.GetAllResponse.Builder responseBuilder = RegionAPI.GetAllResponse.newBuilder();

    request.getKeyList().stream().map((key) -> processOneMessage(serializationService, region, key))
        .forEach(entry -> {
          if (entry instanceof BasicTypes.Entry) {
            responseBuilder.addEntries((BasicTypes.Entry) entry);
          } else if (entry instanceof BasicTypes.KeyedError) {
            responseBuilder.addFailures((BasicTypes.KeyedError) entry);
          }
        });

    return Success.of(responseBuilder.build());
  }

  private Object processOneMessage(SerializationService serializationService, Region region,
      BasicTypes.EncodedValue key) {
    try {
      Object decodedKey = ProtobufUtilities.decodeValue(serializationService, key);
      Object value = region.get(decodedKey);
      return ProtobufUtilities.createEntry(serializationService, decodedKey, value);
    } catch (CodecNotRegisteredForTypeException | UnsupportedEncodingTypeException ex) {
      return BasicTypes.KeyedError.newBuilder().setKey(key)
          .setError(BasicTypes.Error.newBuilder()
              .setErrorCode(ProtocolErrorCode.VALUE_ENCODING_ERROR.codeValue)
              .setMessage("Encoding not supported."))
          .build();
    } catch (org.apache.geode.distributed.LeaseExpiredException | TimeoutException e) {
      return BasicTypes.KeyedError.newBuilder().setKey(key)
          .setError(BasicTypes.Error.newBuilder()
              .setErrorCode(ProtocolErrorCode.OPERATION_TIMEOUT.codeValue)
              .setMessage("Operation timed out: " + e.getMessage()))
          .build();
    } catch (CacheLoaderException | PartitionedRegionStorageException e) {
      return BasicTypes.KeyedError.newBuilder().setKey(key)
          .setError(BasicTypes.Error.newBuilder()
              .setErrorCode(ProtocolErrorCode.DATA_UNREACHABLE.codeValue)
              .setMessage("Data unreachable: " + e.getMessage()))
          .build();
    } catch (NullPointerException | IllegalArgumentException e) {
      return BasicTypes.KeyedError.newBuilder().setKey(key)
          .setError(BasicTypes.Error.newBuilder()
              .setErrorCode(ProtocolErrorCode.CONSTRAINT_VIOLATION.codeValue)
              .setMessage("Invalid input: " + e.getMessage()))
          .build();
    }
  }
}
