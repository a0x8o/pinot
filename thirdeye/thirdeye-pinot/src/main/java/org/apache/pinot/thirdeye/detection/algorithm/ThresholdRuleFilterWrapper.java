/*
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

package org.apache.pinot.thirdeye.detection.algorithm;

import org.apache.pinot.thirdeye.dataframe.DataFrame;
import org.apache.pinot.thirdeye.dataframe.util.MetricSlice;
import org.apache.pinot.thirdeye.datalayer.dto.DetectionConfigDTO;
import org.apache.pinot.thirdeye.datalayer.dto.MergedAnomalyResultDTO;
import org.apache.pinot.thirdeye.detection.DataProvider;
import org.apache.pinot.thirdeye.rootcause.impl.MetricEntity;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;

import static org.apache.pinot.thirdeye.dataframe.util.DataFrameUtils.*;


/**
 * This filter wrapper filters the anomalies if either the min or max thresholds do not pass.
 */
public class ThresholdRuleFilterWrapper extends RuleBasedFilterWrapper {
  private static final String PROP_MIN = "min";
  private static final double PROP_MIN_DEFAULT = Double.NaN;

  private static final String PROP_MAX = "max";
  private static final double PROP_MAX_DEFAULT = Double.NaN;

  private final double min;
  private final double max;

  public ThresholdRuleFilterWrapper(DataProvider provider, DetectionConfigDTO config, long startTime, long endTime) {
    super(provider, config, startTime, endTime);
    this.min = MapUtils.getDoubleValue(config.getProperties(), PROP_MIN, PROP_MIN_DEFAULT);
    this.max = MapUtils.getDoubleValue(config.getProperties(), PROP_MAX, PROP_MAX_DEFAULT);
  }

  @Override
  boolean isQualified(MergedAnomalyResultDTO anomaly) {
    MetricEntity me = MetricEntity.fromURN(anomaly.getMetricUrn());
    MetricSlice currentSlice = MetricSlice.from(me.getId(), anomaly.getStartTime(), anomaly.getEndTime(), me.getFilters());

    Map<MetricSlice, DataFrame> aggregates = this.provider.fetchAggregates(Collections.singleton(currentSlice), Collections.<String>emptyList());
    double currentValue = getValueFromAggregates(currentSlice, aggregates);
    if (!Double.isNaN(this.min) && currentValue < this.min) {
      return false;
    }
    if (!Double.isNaN(this.max) && currentValue > this.max) {
      return false;
    }
    return true;
  }

  double getValueFromAggregates(MetricSlice slice, Map<MetricSlice, DataFrame> aggregates) {
    return aggregates.get(slice).getDouble(COL_VALUE, 0);
  }
}
