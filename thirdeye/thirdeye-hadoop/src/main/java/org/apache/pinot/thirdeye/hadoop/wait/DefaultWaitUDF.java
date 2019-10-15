/**
 * Copyright (C) 2014-2018 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pinot.thirdeye.hadoop.wait;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultWaitUDF implements WaitUDF {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWaitUDF.class);

  private Properties inputConfig;

  public DefaultWaitUDF() {

  }

  @Override
  public void init(Properties inputConfig) {
    this.inputConfig = inputConfig;
  }

  @Override
  // default implementation always returns complete
  public boolean checkCompleteness() {
    return true;
  }

}
