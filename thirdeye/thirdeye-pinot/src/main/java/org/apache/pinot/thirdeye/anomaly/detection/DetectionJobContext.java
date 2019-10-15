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

package org.apache.pinot.thirdeye.anomaly.detection;

import java.util.List;

import org.apache.pinot.thirdeye.anomaly.job.JobContext;
import org.apache.pinot.thirdeye.datalayer.dto.AnomalyFunctionDTO;

public class DetectionJobContext extends JobContext {

  public enum DetectionJobType {
    DEFAULT, BACKFILL, OFFLINE
  }


  private Long anomalyFunctionId;
  private AnomalyFunctionDTO anomalyFunctionSpec;
  private List<Long> startTimes;
  private List<Long> endTimes;
  private DetectionJobType detectionJobType;


  public List<Long> getStartTimes() {
    return startTimes;
  }

  public void setStartTimes(List<Long> startTimes) {
    this.startTimes = startTimes;
  }

  public List<Long> getEndTimes() {
    return endTimes;
  }

  public void setEndTimes(List<Long> endTimes) {
    this.endTimes = endTimes;
  }

  public Long getAnomalyFunctionId() {
    return anomalyFunctionId;
  }

  public void setAnomalyFunctionId(Long anomalyFunctionId) {
    this.anomalyFunctionId = anomalyFunctionId;
  }

  public AnomalyFunctionDTO getAnomalyFunctionSpec() {
    return anomalyFunctionSpec;
  }

  public void setAnomalyFunctionSpec(AnomalyFunctionDTO anomalyFunctionSpec) {
    this.anomalyFunctionSpec = anomalyFunctionSpec;
  }

  public DetectionJobType getDetectionJobType() {
    return detectionJobType;
  }

  public void setDetectionJobType(DetectionJobType detectionJobType) {
    this.detectionJobType = detectionJobType;
  }
}
