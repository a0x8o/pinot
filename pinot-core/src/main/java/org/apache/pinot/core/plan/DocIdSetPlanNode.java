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

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.apache.pinot.common.request.BrokerRequest;
import org.apache.pinot.core.indexsegment.IndexSegment;
import org.apache.pinot.core.operator.DocIdSetOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DocIdSetPlanNode implements PlanNode {
  public static int MAX_DOC_PER_CALL = 10000;

  private static final Logger LOGGER = LoggerFactory.getLogger(DocIdSetPlanNode.class);

  private final IndexSegment _indexSegment;
  private final FilterPlanNode _filterPlanNode;
  private final int _maxDocPerCall;

  public DocIdSetPlanNode(@Nonnull IndexSegment indexSegment, @Nonnull BrokerRequest brokerRequest, int maxDocPerCall) {
    Preconditions.checkState(maxDocPerCall > 0 && maxDocPerCall <= MAX_DOC_PER_CALL);
    _indexSegment = indexSegment;
    _filterPlanNode = new FilterPlanNode(_indexSegment, brokerRequest);
    _maxDocPerCall = maxDocPerCall;
  }

  public DocIdSetPlanNode(@Nonnull IndexSegment indexSegment, @Nonnull BrokerRequest brokerRequest) {
    this(indexSegment, brokerRequest, MAX_DOC_PER_CALL);
  }

  @Override
  public DocIdSetOperator run() {
    return new DocIdSetOperator(_filterPlanNode.run(), _maxDocPerCall);
  }

  @Override
  public void showTree(String prefix) {
    LOGGER.debug(prefix + "DocIdSetPlanNode Plan Node :");
    LOGGER.debug(prefix + "Operator: DocIdSetOperator");
    LOGGER.debug(prefix + "Argument 0: IndexSegment - " + _indexSegment.getSegmentName());
    LOGGER.debug(prefix + "Argument 1: FilterPlanNode:");
    _filterPlanNode.showTree(prefix + "    ");
  }
}
