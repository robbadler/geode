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
package org.apache.geode.connectors.jdbc.internal.cli;

import java.util.List;
import java.util.Set;

import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import org.apache.geode.annotations.Experimental;
import org.apache.geode.connectors.jdbc.internal.configuration.ConnectorService;
import org.apache.geode.distributed.ClusterConfigurationService;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.management.cli.CliMetaData;
import org.apache.geode.management.cli.Result;
import org.apache.geode.management.internal.cli.commands.InternalGfshCommand;
import org.apache.geode.management.internal.cli.functions.CliFunctionResult;
import org.apache.geode.management.internal.cli.i18n.CliStrings;
import org.apache.geode.management.internal.cli.result.CommandResult;
import org.apache.geode.management.internal.cli.result.ResultBuilder;
import org.apache.geode.management.internal.security.ResourceOperation;
import org.apache.geode.security.ResourcePermission;

@Experimental
public class DestroyMappingCommand extends InternalGfshCommand {
  static final String DESTROY_MAPPING = "destroy jdbc-mapping";
  static final String DESTROY_MAPPING__HELP = EXPERIMENTAL + "Destroy the specified mapping.";
  static final String DESTROY_MAPPING__REGION_NAME = "region";
  static final String DESTROY_MAPPING__REGION_NAME__HELP = "Name of the region mapping to destroy.";

  private static final String ERROR_PREFIX = "ERROR: ";

  @CliCommand(value = DESTROY_MAPPING, help = DESTROY_MAPPING__HELP)
  @CliMetaData(relatedTopic = CliStrings.DEFAULT_TOPIC_GEODE)
  @ResourceOperation(resource = ResourcePermission.Resource.CLUSTER,
      operation = ResourcePermission.Operation.MANAGE)
  public Result destroyMapping(@CliOption(key = DESTROY_MAPPING__REGION_NAME, mandatory = true,
      help = DESTROY_MAPPING__REGION_NAME__HELP) String regionName) {

    // input
    Set<DistributedMember> targetMembers = getMembers(null, null);

    // action
    List<CliFunctionResult> results =
        executeAndGetFunctionResult(new DestroyMappingFunction(), regionName, targetMembers);

    // output
    boolean persisted = false;
    ClusterConfigurationService ccService = getConfigurationService();

    if (ccService != null && results.stream().filter(CliFunctionResult::isSuccessful).count() > 0) {
      ConnectorService service =
          ccService.getCustomCacheElement("cluster", "connector-service", ConnectorService.class);
      if (service != null) {
        ccService.removeFromList(service.getRegionMapping(), regionName);
        ccService.saveCustomCacheElement("cluster", service);
        persisted = true;
      }
    }

    CommandResult commandResult = ResultBuilder.buildResult(results, EXPERIMENTAL, null);
    commandResult.setCommandPersisted(persisted);

    return commandResult;
  }
}
