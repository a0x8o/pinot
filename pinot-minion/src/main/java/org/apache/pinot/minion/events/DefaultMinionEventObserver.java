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
package org.apache.pinot.minion.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.pinot.common.config.PinotTaskConfig;


/**
 * Default no-op minion event observer which can be extended.
 */
public class DefaultMinionEventObserver implements MinionEventObserver {

  @Override
  public void notifyTaskStart(@Nonnull PinotTaskConfig pinotTaskConfig) {
  }

  @Override
  public void notifyTaskSuccess(@Nonnull PinotTaskConfig pinotTaskConfig, @Nullable Object executionResult) {
  }

  @Override
  public void notifyTaskCancelled(@Nonnull PinotTaskConfig pinotTaskConfig) {
  }

  @Override
  public void notifyTaskError(@Nonnull PinotTaskConfig pinotTaskConfig, @Nonnull Exception exception) {
  }
}
