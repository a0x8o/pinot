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
package org.apache.pinot.core.startree.v2.store;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.pinot.common.segment.ReadMode;
import org.apache.pinot.core.segment.creator.impl.V1Constants;
import org.apache.pinot.core.segment.index.SegmentMetadataImpl;
import org.apache.pinot.core.segment.index.column.ColumnIndexContainer;
import org.apache.pinot.core.segment.memory.PinotDataBuffer;
import org.apache.pinot.core.startree.v2.StarTreeV2;
import org.apache.pinot.core.startree.v2.StarTreeV2Constants;
import org.apache.pinot.core.startree.v2.StarTreeV2Metadata;

import static org.apache.pinot.core.startree.v2.store.StarTreeIndexMapUtils.IndexKey;
import static org.apache.pinot.core.startree.v2.store.StarTreeIndexMapUtils.IndexValue;


/**
 * The {@code StarTreeIndexContainer} class contains the indexes for multiple star-trees.
 */
public class StarTreeIndexContainer implements Closeable {
  private final PinotDataBuffer _dataBuffer;
  private final List<StarTreeV2> _starTrees;

  public StarTreeIndexContainer(File segmentDirectory, SegmentMetadataImpl segmentMetadata,
      Map<String, ColumnIndexContainer> indexContainerMap, ReadMode readMode)
      throws ConfigurationException, IOException {
    List<StarTreeV2Metadata> starTreeMetadataList = segmentMetadata.getStarTreeV2MetadataList();
    if (starTreeMetadataList != null) {
      // Star-tree V2 exists, load it
      File indexFile = new File(segmentDirectory, StarTreeV2Constants.INDEX_FILE_NAME);
      if (readMode == ReadMode.heap) {
        _dataBuffer = PinotDataBuffer
            .loadFile(indexFile, 0, indexFile.length(), ByteOrder.LITTLE_ENDIAN, "Star-tree V2 data buffer");
      } else {
        _dataBuffer = PinotDataBuffer
            .mapFile(indexFile, true, 0, indexFile.length(), ByteOrder.LITTLE_ENDIAN, "Star-tree V2 data buffer");
      }
      File indexMapFile = new File(segmentDirectory, StarTreeV2Constants.INDEX_MAP_FILE_NAME);
      List<Map<IndexKey, IndexValue>> indexMapList =
          StarTreeIndexMapUtils.loadFromFile(indexMapFile, starTreeMetadataList.size());
      _starTrees = StarTreeLoaderUtils.loadStarTreeV2(_dataBuffer, indexMapList, segmentMetadata, indexContainerMap);
    } else {
      // Backward-compatible: star-tree V2 does not exist, convert star-tree V1 to star-tree V2
      File indexFile = new File(segmentDirectory, V1Constants.STAR_TREE_INDEX_FILE);
      if (readMode == ReadMode.heap) {
        _dataBuffer = PinotDataBuffer
            .loadFile(indexFile, 0, indexFile.length(), ByteOrder.LITTLE_ENDIAN, "Star-tree V1 data buffer");
      } else {
        _dataBuffer = PinotDataBuffer
            .mapFile(indexFile, true, 0, indexFile.length(), ByteOrder.LITTLE_ENDIAN, "Star-tree V1 data buffer");
      }
      _starTrees = StarTreeLoaderUtils.convertFromStarTreeV1(_dataBuffer, segmentMetadata, indexContainerMap);
    }
  }

  public List<StarTreeV2> getStarTrees() {
    return _starTrees;
  }

  @Override
  public void close()
      throws IOException {
    _dataBuffer.close();
  }
}
