/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.core.plan;

import org.apache.pinot.core.operator.InstanceResponseOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InstanceResponsePlanNode implements PlanNode {
  private static final Logger LOGGER = LoggerFactory.getLogger(InstanceResponsePlanNode.class);

  private final CombinePlanNode _combinePlanNode;

  public InstanceResponsePlanNode(CombinePlanNode combinePlanNode) {
    _combinePlanNode = combinePlanNode;
  }

  @Override
  public InstanceResponseOperator run() {
    long start = System.currentTimeMillis();
    InstanceResponseOperator instanceResponseOperator = new InstanceResponseOperator(_combinePlanNode.run());
    long end = System.currentTimeMillis();
    LOGGER.debug("InstanceResponsePlanNode.run took: {}ms", end - start);
    return instanceResponseOperator;
  }

  @Override
  public void showTree(String prefix) {
    LOGGER.debug(prefix + "Instance Level Inter-Segments Query Plan Node:");
    LOGGER.debug(prefix + "Operator: InstanceResponseOperator");
    LOGGER.debug(prefix + "Argument 0: Combine -");
    _combinePlanNode.showTree(prefix + "    ");
  }
}
