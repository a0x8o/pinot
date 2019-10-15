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

package org.apache.pinot.thirdeye.anomalydetection.model.detection;

import org.apache.pinot.thirdeye.anomalydetection.context.AnomalyDetectionContext;
import org.apache.pinot.thirdeye.anomalydetection.context.AnomalyResult;
import java.util.List;
import java.util.Properties;

public interface DetectionModel {
  /**
   * Initializes this model with the given properties.
   * @param properties the given properties.
   */
  void init(Properties properties);

  /**
   * Returns the properties of this model.
   */
  Properties getProperties();

  /**
   * Detects anomalies on the observed (current) time series and returns a list of raw anomalies.
   *
   * @param metricName the name of the metric on which this detection model should detect anomalies
   * @param anomalyDetectionContext the context that contains the observed time series and
   *                                prediction model, which could provide an expected time series or
   *                                additional parameters (e.g., sigma) for anomaly detection.
   * @return list of raw anomalies.
   */
  List<AnomalyResult> detect(String metricName, AnomalyDetectionContext anomalyDetectionContext);
}
