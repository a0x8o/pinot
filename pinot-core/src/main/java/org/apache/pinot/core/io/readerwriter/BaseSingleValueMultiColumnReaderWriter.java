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
package org.apache.pinot.core.io.readerwriter;

import java.io.IOException;
import org.apache.pinot.core.io.reader.ReaderContext;
import org.apache.pinot.core.io.reader.SingleValueMultiColumnReader;
import org.apache.pinot.core.io.writer.SingleValueMultiColumnWriter;


/**
 * Abstract class for reader and writer interfaces for Single-value Multiple-Columns case.
 * @param <T>
 */
public abstract class BaseSingleValueMultiColumnReaderWriter<T extends ReaderContext> implements SingleValueMultiColumnReader<T>, SingleValueMultiColumnWriter {
  @Override
  public char getChar(int row, int column) {
    throw new UnsupportedOperationException();
  }

  @Override
  public short getShort(int row, int column) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getInt(int row, int column) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getInt(int row, int column, T context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLong(int row, int column) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLong(int row, int column, T context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public float getFloat(int row, int column) {
    throw new UnsupportedOperationException();
  }

  @Override
  public float getFloat(int row, int column, T context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public double getDouble(int row, int column) {
    throw new UnsupportedOperationException();
  }

  @Override
  public double getDouble(int row, int column, T context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getString(int row, int column) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getString(int row, int column, T context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] getBytes(int row, int column) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T createContext() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setInt(int row, int column, int value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setLong(int row, int column, long value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setFloat(int row, int column, float value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setDouble(int row, int column, double value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setString(int row, int column, String value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close()
      throws IOException {
    throw new UnsupportedOperationException();
  }
}
