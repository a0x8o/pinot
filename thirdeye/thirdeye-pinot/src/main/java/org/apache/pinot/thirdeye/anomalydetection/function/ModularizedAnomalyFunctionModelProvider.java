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

package org.apache.pinot.thirdeye.anomalydetection.function;

import org.apache.pinot.thirdeye.anomalydetection.model.data.DataModel;
import org.apache.pinot.thirdeye.anomalydetection.model.detection.DetectionModel;
import org.apache.pinot.thirdeye.anomalydetection.model.merge.MergeModel;
import org.apache.pinot.thirdeye.anomalydetection.model.prediction.PredictionModel;
import org.apache.pinot.thirdeye.anomalydetection.model.transform.TransformationFunction;
import java.util.List;

public interface ModularizedAnomalyFunctionModelProvider {

  /**
   * Returns the data model for retrieve current and training time series.
   * @return a data model for retrieve current and training time series.
   */
  DataModel getDataModel();

  /**
   * Returns the chain of transformation functions for current time series.
   * @return a chain of transformation functions.
   */
  List<TransformationFunction> getCurrentTimeSeriesTransformationChain();

  /**
   * Returns the chain of transformation functions for training time series (baselines).
   * @return a chain of transformation functions.
   */
  List<TransformationFunction> getBaselineTimeSeriesTransformationChain();

  /**
   * Returns the prediction model to be trained and used for forecasting.
   * @return a prediction model to be trained and used for forecasting.
   */
  PredictionModel getPredictionModel();

  /**
   * Returns the detection model for testing and detecting the anomalies in observerd time series.
   * @return a detection model for testing and detecting the anomalies in observerd time series.
   */
  DetectionModel getDetectionModel();

  /**
   * Returns the merge model for calculating score, weight, etc. of merged anomalies.
   * @return a merge model for calculating score, weight, etc. of merged anomalies.
   */
  MergeModel getMergeModel();
}
