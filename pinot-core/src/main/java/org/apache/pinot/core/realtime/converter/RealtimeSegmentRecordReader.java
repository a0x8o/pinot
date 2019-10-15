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
package org.apache.pinot.core.realtime.converter;

import org.apache.pinot.common.data.Schema;
import org.apache.pinot.core.data.GenericRow;
import org.apache.pinot.core.data.readers.RecordReader;
import org.apache.pinot.core.indexsegment.generator.SegmentGeneratorConfig;
import org.apache.pinot.core.indexsegment.mutable.MutableSegmentImpl;


/**
 * Record reader for Pinot realtime segment.
 */
public class RealtimeSegmentRecordReader implements RecordReader {
  private final MutableSegmentImpl _realtimeSegment;
  private final int _numDocs;
  private final Schema _schema;
  private final int[] _sortedDocIdIterationOrder;

  private int _nextDocId = 0;

  public RealtimeSegmentRecordReader(MutableSegmentImpl realtimeSegment, Schema schema) {
    _realtimeSegment = realtimeSegment;
    _numDocs = realtimeSegment.getNumDocsIndexed();
    _schema = schema;
    _sortedDocIdIterationOrder = null;
  }

  public RealtimeSegmentRecordReader(MutableSegmentImpl realtimeSegment, Schema schema, String sortedColumn) {
    _realtimeSegment = realtimeSegment;
    _numDocs = realtimeSegment.getNumDocsIndexed();
    _schema = schema;
    _sortedDocIdIterationOrder = realtimeSegment.getSortedDocIdIterationOrderWithSortedColumn(sortedColumn);
  }

  @Override
  public void init(SegmentGeneratorConfig segmentGeneratorConfig) {

  }

  public int[] getSortedDocIdIterationOrder() {
    return _sortedDocIdIterationOrder;
  }

  @Override
  public boolean hasNext() {
    return _nextDocId < _numDocs;
  }

  @Override
  public GenericRow next() {
    return next(new GenericRow());
  }

  @Override
  public GenericRow next(GenericRow reuse) {
    if (_sortedDocIdIterationOrder == null) {
      return _realtimeSegment.getRecord(_nextDocId++, reuse);
    } else {
      return _realtimeSegment.getRecord(_sortedDocIdIterationOrder[_nextDocId++], reuse);
    }
  }

  @Override
  public void rewind() {
    _nextDocId = 0;
  }

  @Override
  public Schema getSchema() {
    return _schema;
  }

  @Override
  public void close() {
  }
}
