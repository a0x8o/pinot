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
package org.apache.pinot.common.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;


@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public class StarTreeIndexConfig {
  @ConfigKey("dimensionsSplitOrder")
  private List<String> _dimensionsSplitOrder;

  @ConfigKey("skipStarNodeCreationForDimensions")
  private List<String> _skipStarNodeCreationForDimensions;

  @ConfigKey("functionColumnPairs")
  private List<String> _functionColumnPairs;

  @ConfigKey("maxLeafRecords")
  private int _maxLeafRecords;

  public List<String> getDimensionsSplitOrder() {
    return _dimensionsSplitOrder;
  }

  public void setDimensionsSplitOrder(List<String> dimensionsSplitOrder) {
    _dimensionsSplitOrder = dimensionsSplitOrder;
  }

  public List<String> getSkipStarNodeCreationForDimensions() {
    return _skipStarNodeCreationForDimensions;
  }

  public void setSkipStarNodeCreationForDimensions(List<String> skipStarNodeCreationForDimensions) {
    _skipStarNodeCreationForDimensions = skipStarNodeCreationForDimensions;
  }

  public List<String> getFunctionColumnPairs() {
    return _functionColumnPairs;
  }

  public void setFunctionColumnPairs(List<String> functionColumnPairs) {
    _functionColumnPairs = functionColumnPairs;
  }

  public int getMaxLeafRecords() {
    return _maxLeafRecords;
  }

  public void setMaxLeafRecords(int maxLeafRecords) {
    _maxLeafRecords = maxLeafRecords;
  }
}
