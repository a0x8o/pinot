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

package org.apache.pinot.thirdeye.completeness.checker;


import java.util.List;

import org.apache.pinot.thirdeye.anomaly.job.JobContext;

/**
 * job context for data completeness jobs
 */
public class DataCompletenessJobContext extends JobContext {

   private long checkDurationStartTime;
   private long checkDurationEndTime;
   private List<String> datasetsToCheck;

  public long getCheckDurationStartTime() {
    return checkDurationStartTime;
  }
  public void setCheckDurationStartTime(long checkDurationStartTime) {
    this.checkDurationStartTime = checkDurationStartTime;
  }
  public long getCheckDurationEndTime() {
    return checkDurationEndTime;
  }
  public void setCheckDurationEndTime(long checkDurationEndTime) {
    this.checkDurationEndTime = checkDurationEndTime;
  }
  public List<String> getDatasetsToCheck() {
    return datasetsToCheck;
  }
  public void setDatasetsToCheck(List<String> datasetsToCheck) {
    this.datasetsToCheck = datasetsToCheck;
  }

}
