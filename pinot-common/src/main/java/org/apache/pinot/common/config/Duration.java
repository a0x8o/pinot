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

import java.util.concurrent.TimeUnit;
import org.apache.pinot.common.utils.EqualityUtils;


/**
 * Duration, which is a combination of a time unit (minutes, seconds, hours, etc.) and a unit count.
 */
public class Duration {
  @ConfigKey("unit")
  private TimeUnit unit;

  @ConfigKey("unitCount")
  private int unitCount;

  public Duration(TimeUnit unit, int unitCount) {
    this.unit = unit;
    this.unitCount = unitCount;
  }

  public Duration() {
  }

  public TimeUnit getUnit() {
    return unit;
  }

  public int getUnitCount() {
    return unitCount;
  }

  @Override
  public boolean equals(Object o) {
    if (EqualityUtils.isSameReference(this, o)) {
      return true;
    }

    if (EqualityUtils.isNullOrNotSameClass(this, o)) {
      return false;
    }

    Duration duration = (Duration) o;

    return EqualityUtils.isEqual(unitCount, duration.unitCount) && EqualityUtils.isEqual(unit, duration.unit);
  }

  @Override
  public int hashCode() {
    int result = EqualityUtils.hashCodeOf(unit);
    result = EqualityUtils.hashCodeOf(result, unitCount);
    return result;
  }

  @Override
  public String toString() {
    return "Duration{" + "unit=" + unit + ", unitCount=" + unitCount + '}';
  }
}
