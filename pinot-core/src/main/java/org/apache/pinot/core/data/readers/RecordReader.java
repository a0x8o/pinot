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
package org.apache.pinot.core.data.readers;

import java.io.Closeable;
import java.io.IOException;
import org.apache.pinot.common.data.Schema;
import org.apache.pinot.core.data.GenericRow;
import org.apache.pinot.core.indexsegment.generator.SegmentGeneratorConfig;


/**
 * The <code>RecordReader</code> interface is used to read records from various file formats into {@link GenericRow}s.
 * Pinot segments will be generated from {@link GenericRow}s.
 * <p>NOTE: for time column, record reader should be able to read both incoming and outgoing time (see
 * {@link RecordReaderUtils#extractFieldSpecs(Schema)} for details).
 */
public interface RecordReader extends Closeable {

  /**
   * Initializes the record reader when needed
   */
  void init(SegmentGeneratorConfig segmentGeneratorConfig)
      throws IOException;

  /**
   * Return <code>true</code> if more records remain to be read.
   */
  boolean hasNext();

  /**
   * Get the next record.
   */
  GenericRow next()
      throws IOException;

  /**
   * Get the next record. Re-use the given row if possible to reduce garbage.
   * <p>The passed in row should be returned by previous call to {@link #next()}.
   */
  GenericRow next(GenericRow reuse)
      throws IOException;

  /**
   * Rewind the reader to start reading from the first record again.
   */
  void rewind()
      throws IOException;

  /**
   * Get the Pinot schema.
   */
  Schema getSchema();
}
