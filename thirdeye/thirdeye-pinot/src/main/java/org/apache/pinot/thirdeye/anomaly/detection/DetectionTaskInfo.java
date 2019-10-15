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
import java.util.Objects;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.MoreObjects;
import org.apache.pinot.thirdeye.anomaly.task.TaskInfo;
import org.apache.pinot.thirdeye.datalayer.dto.AnomalyFunctionDTO;
import org.apache.pinot.thirdeye.util.CustomListDateDeserializer;
import org.apache.pinot.thirdeye.util.CustomListDateSerializer;
import org.apache.pinot.thirdeye.anomaly.detection.DetectionJobContext.DetectionJobType;

public class DetectionTaskInfo implements TaskInfo {

  private long jobExecutionId;

  @JsonSerialize(using = CustomListDateSerializer.class)
  @JsonDeserialize(using = CustomListDateDeserializer.class)
  private List<DateTime> windowStartTime;

  @JsonSerialize(using = CustomListDateSerializer.class)
  @JsonDeserialize(using = CustomListDateDeserializer.class)
  private List<DateTime> windowEndTime;
  private AnomalyFunctionDTO anomalyFunctionSpec;
  private String groupByDimension;
  private DetectionJobType detectionJobType = DetectionJobType.DEFAULT;

  public DetectionTaskInfo(long jobExecutionId, List<DateTime> windowStartTime,
      List<DateTime> windowEndTime, AnomalyFunctionDTO anomalyFunctionSpec, String groupByDimension,
      DetectionJobType detectionJobType) {
    this.jobExecutionId = jobExecutionId;
    this.windowStartTime = windowStartTime;
    this.windowEndTime = windowEndTime;
    this.anomalyFunctionSpec = anomalyFunctionSpec;
    this.groupByDimension = groupByDimension;
    this.detectionJobType = detectionJobType;
  }

  public DetectionTaskInfo() {
    this.detectionJobType = DetectionJobType.DEFAULT;
  }

  public long getJobExecutionId() {
    return jobExecutionId;
  }

  public void setJobExecutionId(long jobExecutionId) {
    this.jobExecutionId = jobExecutionId;
  }

  public List<DateTime> getWindowStartTime() {
    return windowStartTime;
  }

  public void setWindowStartTime(List<DateTime> windowStartTime) {
    this.windowStartTime = windowStartTime;
  }

  public List<DateTime> getWindowEndTime() {
    return windowEndTime;
  }

  public void setWindowEndTime(List<DateTime> windowEndTime) {
    this.windowEndTime = windowEndTime;
  }

  public AnomalyFunctionDTO getAnomalyFunctionSpec() {
    return anomalyFunctionSpec;
  }

  public void setAnomalyFunctionSpec(AnomalyFunctionDTO anomalyFunctionSpec) {
    this.anomalyFunctionSpec = anomalyFunctionSpec;
  }

  public String getGroupByDimension() {
    return groupByDimension;
  }

  public void setGroupByDimension(String groupByDimension) {
    this.groupByDimension = groupByDimension;
  }

  public DetectionJobType getDetectionJobType() {
    return detectionJobType;
  }

  public void setDetectionJobType(DetectionJobType detectionJobType) {
    this.detectionJobType = detectionJobType;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DetectionTaskInfo)) {
      return false;
    }
    DetectionTaskInfo dt = (DetectionTaskInfo) o;
    return Objects.equals(jobExecutionId, dt.getJobExecutionId())
        && Objects.equals(windowStartTime, dt.getWindowStartTime())
        && Objects.equals(windowEndTime, dt.getWindowEndTime())
        && Objects.equals(anomalyFunctionSpec, dt.getAnomalyFunctionSpec())
        && Objects.equals(groupByDimension, dt.getGroupByDimension());
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobExecutionId, windowStartTime, windowEndTime, anomalyFunctionSpec, groupByDimension);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("jobExecutionId", jobExecutionId).add("windowStartTime", windowStartTime)
        .add("windowEndTime", windowEndTime).add("anomalyFunctionSpec", anomalyFunctionSpec)
        .add("groupByDimension", groupByDimension).toString();
  }
}
