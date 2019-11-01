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
package org.apache.pinot.core.data.recordtransformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pinot.common.data.FieldSpec;
import org.apache.pinot.common.data.Schema;
import org.apache.pinot.core.data.GenericRow;


/**
 * The {@code DataTypeTransformer} class will convert the values to follow the data types in {@link FieldSpec}.
 * <p>NOTE: should put this after all the values has been generated by other transformers (such as
 * {@link TimeTransformer} and {@link ExpressionTransformer}). After this, all values should be of the desired data
 * types.
 */
public class DataTypeTransformer implements RecordTransformer {
  private static final Map<Class, PinotDataType> SINGLE_VALUE_TYPE_MAP = new HashMap<>();
  private static final Map<Class, PinotDataType> MULTI_VALUE_TYPE_MAP = new HashMap<>();

  static {
    SINGLE_VALUE_TYPE_MAP.put(Boolean.class, PinotDataType.BOOLEAN);
    SINGLE_VALUE_TYPE_MAP.put(Byte.class, PinotDataType.BYTE);
    SINGLE_VALUE_TYPE_MAP.put(Character.class, PinotDataType.CHARACTER);
    SINGLE_VALUE_TYPE_MAP.put(Short.class, PinotDataType.SHORT);
    SINGLE_VALUE_TYPE_MAP.put(Integer.class, PinotDataType.INTEGER);
    SINGLE_VALUE_TYPE_MAP.put(Long.class, PinotDataType.LONG);
    SINGLE_VALUE_TYPE_MAP.put(Float.class, PinotDataType.FLOAT);
    SINGLE_VALUE_TYPE_MAP.put(Double.class, PinotDataType.DOUBLE);
    SINGLE_VALUE_TYPE_MAP.put(String.class, PinotDataType.STRING);
    SINGLE_VALUE_TYPE_MAP.put(byte[].class, PinotDataType.BYTES);

    MULTI_VALUE_TYPE_MAP.put(Byte.class, PinotDataType.BYTE_ARRAY);
    MULTI_VALUE_TYPE_MAP.put(Character.class, PinotDataType.CHARACTER_ARRAY);
    MULTI_VALUE_TYPE_MAP.put(Short.class, PinotDataType.SHORT_ARRAY);
    MULTI_VALUE_TYPE_MAP.put(Integer.class, PinotDataType.INTEGER_ARRAY);
    MULTI_VALUE_TYPE_MAP.put(Long.class, PinotDataType.LONG_ARRAY);
    MULTI_VALUE_TYPE_MAP.put(Float.class, PinotDataType.FLOAT_ARRAY);
    MULTI_VALUE_TYPE_MAP.put(Double.class, PinotDataType.DOUBLE_ARRAY);
    MULTI_VALUE_TYPE_MAP.put(String.class, PinotDataType.STRING_ARRAY);
  }

  private final Map<String, PinotDataType> _dataTypes = new HashMap<>();

  public DataTypeTransformer(Schema schema) {
    for (FieldSpec fieldSpec : schema.getAllFieldSpecs()) {
      if (!fieldSpec.isVirtualColumn()) {
        _dataTypes.put(fieldSpec.getName(), PinotDataType.getPinotDataType(fieldSpec));
      }
    }
  }

  @Override
  public GenericRow transform(GenericRow record) {
    for (Map.Entry<String, PinotDataType> entry : _dataTypes.entrySet()) {
      String column = entry.getKey();
      Object value = record.getValue(column);

      // Convert List value to Object[]
      if (value instanceof List) {
        value = ((List) value).toArray();
      }

      // Convert data type if necessary
      PinotDataType source;
      if (value instanceof Object[]) {
        // Multi-value column
        Object[] values = (Object[]) value;
        source = MULTI_VALUE_TYPE_MAP.get(values[0].getClass());
        if (source == null) {
          source = PinotDataType.OBJECT_ARRAY;
        }
      } else {
        // Single-value column
        source = SINGLE_VALUE_TYPE_MAP.get(value.getClass());
        if (source == null) {
          source = PinotDataType.OBJECT;
        }
      }
      PinotDataType dest = entry.getValue();
      if (source != dest) {
        value = dest.convert(value, source);
      }

      record.putValue(column, value);
    }
    return record;
  }
}