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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.pinot.common.data.Schema;
import org.apache.pinot.common.utils.EqualityUtils;
import org.apache.pinot.common.utils.JsonUtils;

import static org.apache.pinot.common.utils.EqualityUtils.hashCodeOf;
import static org.apache.pinot.common.utils.EqualityUtils.isEqual;
import static org.apache.pinot.common.utils.EqualityUtils.isNullOrNotSameClass;


/**
 * Combined configuration object, which contains an offline table configuration, a realtime table configuration, and a
 * schema for the table.
 */
@UseChildKeyTransformers({AdjustTableNameChildKeyTransformer.class, RemapTableTypesChildKeyTransformer.class, CombinedConfigSeparatorChildKeyTransformer.class})
public class CombinedConfig {
  @ConfigKey("offline")
  private TableConfig _offline;

  @ConfigKey("realtime")
  private TableConfig _realtime;

  @ConfigKey("schema")
  private Schema _schema;

  @Override
  public String toString() {
    try {
      return JsonUtils.objectToPrettyString(this);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public TableConfig getOfflineTableConfig() {
    return _offline;
  }

  public TableConfig getRealtimeTableConfig() {
    return _realtime;
  }

  public Schema getSchema() {
    return _schema;
  }

  public void setOffline(TableConfig offline) {
    _offline = offline;
  }

  public void setRealtime(TableConfig realtime) {
    _realtime = realtime;
  }

  public void setSchema(Schema schema) {
    _schema = schema;
  }

  public CombinedConfig(TableConfig offline, TableConfig realtime, Schema schema) {
    _offline = offline;
    _realtime = realtime;
    _schema = schema;
  }

  public CombinedConfig() {
  }

  @Override
  public boolean equals(Object o) {
    if (EqualityUtils.isSameReference(this, o)) {
      return true;
    }

    if (isNullOrNotSameClass(this, o)) {
      return false;
    }

    CombinedConfig that = (CombinedConfig) o;

    return isEqual(_offline, that._offline) && isEqual(_realtime, that._realtime) && isEqual(_schema, that._schema);
  }

  @Override
  public int hashCode() {
    int result = hashCodeOf(_offline);
    result = hashCodeOf(result, _realtime);
    result = hashCodeOf(result, _schema);
    return result;
  }
}
