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

package org.apache.pinot.thirdeye.alert.feed;

import org.apache.pinot.thirdeye.alert.commons.AnomalyFeedConfig;
import org.apache.pinot.thirdeye.datalayer.dto.MergedAnomalyResultDTO;
import org.apache.pinot.thirdeye.detector.email.filter.AlertFilterFactory;
import java.util.Collection;

/**
 * AnomalyFeed aggregates the output from AnomalyFetchers, and filter the anomalies based on user-input criteria.
 * This anomaly feed can simply union the output, or filter then ongoing anomalies if they are less severe than before.
 */
public interface AnomalyFeed {
  /**
   * Initialize the anomaly feed
   * @param alertFilterFactory
   * @param anomalyFeedConfig
   */
  void init(AlertFilterFactory alertFilterFactory, AnomalyFeedConfig anomalyFeedConfig);

  /**
   * Get the aggregated anomalies
   * @return
   */
  Collection<MergedAnomalyResultDTO> getAnomalyFeed();

  /**
   * Update the snapshots by the alerted anomalies
   * @param alertedAnomalies
   */
  void updateSnapshot(Collection<MergedAnomalyResultDTO> alertedAnomalies);
}
