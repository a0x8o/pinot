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
package org.apache.pinot.index.reader;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.Random;
import org.apache.pinot.core.io.reader.ReaderContext;
import org.apache.pinot.core.io.reader.SingleColumnMultiValueReader;
import org.apache.pinot.core.io.writer.SingleColumnMultiValueWriter;
import org.apache.pinot.core.segment.memory.PinotDataBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;


public class FixedBitMultiValueTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(FixedBitMultiValueTest.class);

  @Test
  public void testSingleColMultiValue()
      throws Exception {
    testSingleColMultiValue(org.apache.pinot.core.io.writer.impl.v1.FixedBitMultiValueWriter.class,
        org.apache.pinot.core.io.reader.impl.v1.FixedBitMultiValueReader.class);
    testSingleColMultiValueWithContext(org.apache.pinot.core.io.writer.impl.v1.FixedBitMultiValueWriter.class,
        org.apache.pinot.core.io.reader.impl.v1.FixedBitMultiValueReader.class);
  }

  public void testSingleColMultiValue(Class<? extends SingleColumnMultiValueWriter> writerClazz,
      Class<? extends SingleColumnMultiValueReader> readerClazz)
      throws Exception {
    LOGGER.info("Testing for writerClazz:{} readerClass:{}", writerClazz.getName(), readerClazz.getName());
    Constructor<? extends SingleColumnMultiValueWriter> writerClazzConstructor =
        writerClazz.getConstructor(new Class[]{File.class, int.class, int.class, int.class});
    Constructor<? extends SingleColumnMultiValueReader> readerClazzConstructor =
        readerClazz.getConstructor(new Class[]{PinotDataBuffer.class, int.class, int.class, int.class});
    int maxBits = 1;
    while (maxBits < 32) {
      final String fileName = getClass().getName() + "_test_single_col_mv_fixed_bit.dat";
      final File f = new File(fileName);
      f.delete();
      int numDocs = 10;
      int maxNumValues = 100;
      final int[][] data = new int[numDocs][];
      final Random r = new Random();
      final int maxValue = (int) Math.pow(2, maxBits);
      int totalNumValues = 0;
      int[] startOffsets = new int[numDocs];
      int[] lengths = new int[numDocs];
      for (int i = 0; i < data.length; i++) {
        final int numValues = r.nextInt(maxNumValues) + 1;
        data[i] = new int[numValues];
        for (int j = 0; j < numValues; j++) {
          data[i][j] = r.nextInt(maxValue);
        }
        startOffsets[i] = totalNumValues;
        lengths[i] = numValues;
        totalNumValues = totalNumValues + numValues;
      }

      SingleColumnMultiValueWriter writer =
          writerClazzConstructor.newInstance(new Object[]{f, numDocs, totalNumValues, maxBits});

      for (int i = 0; i < data.length; i++) {
        writer.setIntArray(i, data[i]);
      }
      writer.close();

      final RandomAccessFile raf = new RandomAccessFile(f, "rw");
      raf.close();

      int[] readValues = new int[maxNumValues];

      // Test heap mode
      try (SingleColumnMultiValueReader<? extends ReaderContext> heapReader = readerClazzConstructor
          .newInstance(new Object[]{PinotDataBuffer.loadBigEndianFile(f), numDocs, totalNumValues, maxBits})) {
        for (int i = 0; i < data.length; i++) {
          final int numValues = heapReader.getIntArray(i, readValues);
          Assert.assertEquals(numValues, data[i].length);
          for (int j = 0; j < numValues; j++) {
            Assert.assertEquals(readValues[j], data[i][j]);
          }
        }
      }

      // Test mmap mode
      try (SingleColumnMultiValueReader<? extends ReaderContext> mmapReader = readerClazzConstructor
          .newInstance(new Object[]{PinotDataBuffer.mapReadOnlyBigEndianFile(f), numDocs, totalNumValues, maxBits})) {
        for (int i = 0; i < data.length; i++) {
          final int numValues = mmapReader.getIntArray(i, readValues);
          Assert.assertEquals(numValues, data[i].length);
          for (int j = 0; j < numValues; j++) {
            Assert.assertEquals(readValues[j], data[i][j]);
          }
        }
      }

      f.delete();
      maxBits = maxBits + 1;
    }
    LOGGER.info("DONE: Testing for writerClazz:{} readerClass:{}", writerClazz.getName(), readerClazz.getName());
  }

  public void testSingleColMultiValueWithContext(Class<? extends SingleColumnMultiValueWriter> writerClazz,
      Class<? extends SingleColumnMultiValueReader<? extends ReaderContext>> readerClazz)
      throws Exception {
    LOGGER.info("Testing for writerClazz:{} readerClass:{}", writerClazz.getName(), readerClazz.getName());
    Constructor<? extends SingleColumnMultiValueWriter> writerClazzConstructor =
        writerClazz.getConstructor(new Class[]{File.class, int.class, int.class, int.class});
    Constructor<? extends SingleColumnMultiValueReader<? extends ReaderContext>> readerClazzConstructor =
        readerClazz.getConstructor(new Class[]{PinotDataBuffer.class, int.class, int.class, int.class});
    int maxBits = 1;
    while (maxBits < 32) {
      final String fileName = getClass().getName() + "_test_single_col_mv_fixed_bit.dat";
      final File f = new File(fileName);
      f.delete();
      int numDocs = 10;
      int maxNumValues = 100;
      final int[][] data = new int[numDocs][];
      final Random r = new Random();
      final int maxValue = (int) Math.pow(2, maxBits);
      int totalNumValues = 0;
      int[] startOffsets = new int[numDocs];
      int[] lengths = new int[numDocs];
      for (int i = 0; i < data.length; i++) {
        final int numValues = r.nextInt(maxNumValues) + 1;
        data[i] = new int[numValues];
        for (int j = 0; j < numValues; j++) {
          data[i][j] = r.nextInt(maxValue);
        }
        startOffsets[i] = totalNumValues;
        lengths[i] = numValues;
        totalNumValues = totalNumValues + numValues;
      }

      SingleColumnMultiValueWriter writer =
          writerClazzConstructor.newInstance(new Object[]{f, numDocs, totalNumValues, maxBits});

      for (int i = 0; i < data.length; i++) {
        writer.setIntArray(i, data[i]);
      }
      writer.close();

      final RandomAccessFile raf = new RandomAccessFile(f, "rw");
      raf.close();

      int[] readValues = new int[maxNumValues];

      // Test heap mode
      try (SingleColumnMultiValueReader heapReader = readerClazzConstructor
          .newInstance(new Object[]{PinotDataBuffer.loadBigEndianFile(f), numDocs, totalNumValues, maxBits})) {
        ReaderContext context = heapReader.createContext();
        for (int i = 0; i < data.length; i++) {
          final int numValues = heapReader.getIntArray(i, readValues, context);
          if (numValues != data[i].length) {
            System.err.println("Failed Expected:" + data[i].length + " Actual:" + numValues);
            int length = heapReader.getIntArray(i, readValues, context);
          }
          Assert.assertEquals(numValues, data[i].length);
          for (int j = 0; j < numValues; j++) {
            Assert.assertEquals(readValues[j], data[i][j]);
          }
        }
      }

      // Test mmap mode
      try (SingleColumnMultiValueReader<? extends ReaderContext> mmapReader = readerClazzConstructor
          .newInstance(new Object[]{PinotDataBuffer.mapReadOnlyBigEndianFile(f), numDocs, totalNumValues, maxBits})) {
        for (int i = 0; i < data.length; i++) {
          final int numValues = mmapReader.getIntArray(i, readValues);
          Assert.assertEquals(numValues, data[i].length);
          for (int j = 0; j < numValues; j++) {
            Assert.assertEquals(readValues[j], data[i][j]);
          }
        }
      }

      f.delete();
      maxBits = maxBits + 1;
    }
    LOGGER.info("DONE: Testing for writerClazz:{} readerClass:{}", writerClazz.getName(), readerClazz.getName());
  }
}
