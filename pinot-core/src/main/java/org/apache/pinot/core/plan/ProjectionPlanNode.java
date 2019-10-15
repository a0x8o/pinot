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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.pinot.core.common.DataSource;
import org.apache.pinot.core.indexsegment.IndexSegment;
import org.apache.pinot.core.operator.ProjectionOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>ProjectionPlanNode</code> class provides the execution plan for fetching projection columns' data source
 * on a single segment.
 */
public class ProjectionPlanNode implements PlanNode {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProjectionPlanNode.class);

  private final IndexSegment _indexSegment;
  private final Set<String> _projectionColumns;
  private final DocIdSetPlanNode _docIdSetPlanNode;

  public ProjectionPlanNode(@Nonnull IndexSegment indexSegment, @Nonnull Set<String> projectionColumns,
      @Nonnull DocIdSetPlanNode docIdSetPlanNode) {
    _indexSegment = indexSegment;
    _projectionColumns = projectionColumns;
    _docIdSetPlanNode = docIdSetPlanNode;
  }

  @Override
  public ProjectionOperator run() {
    Map<String, DataSource> dataSourceMap = new HashMap<>(_projectionColumns.size());
    for (String column : _projectionColumns) {
      dataSourceMap.put(column, _indexSegment.getDataSource(column));
    }
    return new ProjectionOperator(dataSourceMap, _docIdSetPlanNode.run());
  }

  @Override
  public void showTree(String prefix) {
    LOGGER.debug(prefix + "Segment Level Inner-Segment Plan Node:");
    LOGGER.debug(prefix + "Operator: ProjectionOperator");
    LOGGER.debug(prefix + "Argument 0: IndexSegment - " + _indexSegment.getSegmentName());
    LOGGER.debug(prefix + "Argument 1: Projection Columns - " + _projectionColumns);
    LOGGER.debug(prefix + "Argument 2: DocIdSet - ");
    _docIdSetPlanNode.showTree(prefix + "    ");
  }
}
