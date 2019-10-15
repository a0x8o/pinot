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

package org.apache.pinot.thirdeye.anomalydetection.model.merge;

import org.apache.pinot.thirdeye.anomalydetection.context.AnomalyDetectionContext;
import org.apache.pinot.thirdeye.anomalydetection.context.TimeSeries;
import org.apache.pinot.thirdeye.anomalydetection.model.detection.MinMaxThresholdDetectionModel;
import org.apache.pinot.thirdeye.datalayer.dto.MergedAnomalyResultDTO;
import java.util.Properties;
import org.joda.time.Interval;

public class MinMaxThresholdMergeModel extends AbstractMergeModel {
  public static final String DEFAULT_MESSAGE_TEMPLATE = "change : %.2f %%, currentVal : %.2f, min : %.2f, max : %.2f";
  public static final String MIN_VAL = "min";
  public static final String MAX_VAL = "max";

  @Override public void update(AnomalyDetectionContext anomalyDetectionContext,
      MergedAnomalyResultDTO anomalyToUpdated) {
    // Get min / max props
    Properties props = getProperties();
    Double min = null;
    if (props.containsKey(MIN_VAL)) {
      min = Double.valueOf(props.getProperty(MIN_VAL));
    }
    Double max = null;
    if (props.containsKey(MAX_VAL)) {
      max = Double.valueOf(props.getProperty(MAX_VAL));
    }

    String metricName =
        anomalyDetectionContext.getAnomalyDetectionFunction().getSpec().getTopicMetric();
    TimeSeries timeSeries = anomalyDetectionContext.getTransformedCurrent(metricName);
    Interval timeSeriesInterval = timeSeries.getTimeSeriesInterval();

    long windowStartInMillis = timeSeriesInterval.getStartMillis();
    long windowEndInMillis = timeSeriesInterval.getEndMillis();

    double currentAverageValue = 0d;
    int currentBucketCount = 0;
    double deviationFromThreshold = 0d;
    long anomalyStartTime = anomalyToUpdated.getStartTime();
    long anomalyEndTime = anomalyToUpdated.getEndTime();
    Interval anomalyInterval = new Interval(anomalyStartTime, anomalyEndTime);
    for (long time : timeSeries.timestampSet()) {
      if (anomalyInterval.contains(time)) {
        double value = timeSeries.get(time);
        if (value != 0d) {
          if (windowStartInMillis <= time && time <= windowEndInMillis) {
            currentAverageValue += value;
            ++currentBucketCount;
            deviationFromThreshold += MinMaxThresholdDetectionModel.getDeviationFromThreshold(value, min, max);
          } // else ignore unknown time key
        }
      }
    }

    if (currentBucketCount > 0) {
      currentAverageValue /= currentBucketCount;
      deviationFromThreshold /= currentBucketCount;
    }
    anomalyToUpdated.setScore(currentAverageValue);
    anomalyToUpdated.setWeight(deviationFromThreshold);
    anomalyToUpdated.setAvgCurrentVal(currentAverageValue);
    double value = 0d;
    if (min != null) {
      value = min;
    } else if (max != null) {
      value = max;
    }
    anomalyToUpdated.setAvgBaselineVal(value);

    String message =
        String.format(DEFAULT_MESSAGE_TEMPLATE, deviationFromThreshold, currentAverageValue, min, max);
    anomalyToUpdated.setMessage(message);
  }
}
