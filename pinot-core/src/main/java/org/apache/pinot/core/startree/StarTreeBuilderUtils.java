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
package org.apache.pinot.core.startree;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.apache.pinot.common.utils.StringUtil;
import org.apache.pinot.core.segment.memory.PinotDataBuffer;


/**
 * The {@code StarTreeBuilderUtils} class contains utility methods for star-tree builders.
 */
public class StarTreeBuilderUtils {
  private StarTreeBuilderUtils() {
  }

  public static final int INVALID_ID = -1;

  public static class TreeNode {
    public int _dimensionId = INVALID_ID;
    public int _dimensionValue = INVALID_ID;
    public int _startDocId = INVALID_ID;
    public int _endDocId = INVALID_ID;
    public int _aggregatedDocId = INVALID_ID;
    public int _childDimensionId = INVALID_ID;
    public Map<Integer, TreeNode> _children;
  }

  /**
   * Serialize the star-tree structure into a file.
   */
  public static void serializeTree(File starTreeFile, TreeNode rootNode, String[] dimensions, int numNodes)
      throws IOException {
    int headerSizeInBytes = computeHeaderByteSize(dimensions);
    long totalSizeInBytes = headerSizeInBytes + (long) numNodes * OffHeapStarTreeNode.SERIALIZABLE_SIZE_IN_BYTES;

    // Backward-compatible: star-tree file is always little-endian
    try (PinotDataBuffer buffer = PinotDataBuffer
        .mapFile(starTreeFile, false, 0, totalSizeInBytes, ByteOrder.LITTLE_ENDIAN,
            "StarTreeBuilderUtils#serializeTree: star-tree buffer")) {
      long offset = writeHeader(buffer, headerSizeInBytes, dimensions, numNodes);
      writeNodes(buffer, offset, rootNode);
    }
  }

  /**
   * Helper method to compute size of the star-tree header in bytes.
   * <p>The header contains the following fields:
   * <ul>
   *   <li>Magic marker (long)</li>
   *   <li>Size of the header (int)</li>
   *   <li>Version (int)</li>
   *   <li>Number of dimensions (int)</li>
   *   <li>For each dimension, index of the dimension (int), number of bytes in the dimension string (int), and the byte
   *   array for the string</li>
   *   <li>Number of nodes in the tree (int)</li>
   * </ul>
   */
  private static int computeHeaderByteSize(String[] dimensions) {
    // Magic marker (8), version (4), size of header (4) and number of dimensions (4)
    int headerSizeInBytes = 20;

    for (String dimension : dimensions) {
      headerSizeInBytes += Integer.BYTES; // For dimension index
      headerSizeInBytes += Integer.BYTES; // For length of dimension name
      headerSizeInBytes += StringUtil.encodeUtf8(dimension).length; // For dimension name
    }

    headerSizeInBytes += Integer.BYTES; // For number of nodes.
    return headerSizeInBytes;
  }

  /**
   * Helper method to write the header into the data buffer.
   */
  private static int writeHeader(PinotDataBuffer dataBuffer, int headerSizeInBytes, String[] dimensions, int numNodes) {
    int offset = 0;

    dataBuffer.putLong(offset, OffHeapStarTree.MAGIC_MARKER);
    offset += Long.BYTES;

    dataBuffer.putInt(offset, OffHeapStarTree.VERSION);
    offset += Integer.BYTES;

    dataBuffer.putInt(offset, headerSizeInBytes);
    offset += Integer.BYTES;

    int numDimensions = dimensions.length;
    dataBuffer.putInt(offset, numDimensions);
    offset += Integer.BYTES;

    for (int i = 0; i < numDimensions; i++) {
      dataBuffer.putInt(offset, i);
      offset += Integer.BYTES;

      String dimension = dimensions[i];
      byte[] dimensionBytes = StringUtil.encodeUtf8(dimension);
      int dimensionLength = dimensionBytes.length;
      dataBuffer.putInt(offset, dimensionLength);
      offset += Integer.BYTES;

      dataBuffer.readFrom(offset, dimensionBytes, 0, dimensionLength);
      offset += dimensionLength;
    }

    dataBuffer.putInt(offset, numNodes);
    offset += Integer.BYTES;

    return offset;
  }

  /**
   * Helper method to traverse star-tree using BFS and write nodes into the data buffer.
   */
  private static void writeNodes(PinotDataBuffer dataBuffer, long offset, TreeNode rootNode) {
    Queue<TreeNode> queue = new LinkedList<>();
    queue.add(rootNode);

    int currentNodeId = 0;
    while (!queue.isEmpty()) {
      TreeNode node = queue.remove();

      if (node._children == null) {
        offset = writeNode(dataBuffer, offset, node, INVALID_ID, INVALID_ID);
      } else {
        // Sort all children nodes based on dimension value
        List<TreeNode> sortedChildren = new ArrayList<>(node._children.values());
        sortedChildren.sort((o1, o2) -> Integer.compare(o1._dimensionValue, o2._dimensionValue));

        int firstChildId = currentNodeId + queue.size() + 1;
        int lastChildId = firstChildId + sortedChildren.size() - 1;
        offset = writeNode(dataBuffer, offset, node, firstChildId, lastChildId);

        queue.addAll(sortedChildren);
      }

      currentNodeId++;
    }
  }

  /**
   * Helper method to write one node into the data buffer.
   */
  private static long writeNode(PinotDataBuffer dataBuffer, long offset, TreeNode node, int firstChildId,
      int lastChildId) {
    dataBuffer.putInt(offset, node._dimensionId);
    offset += Integer.BYTES;

    dataBuffer.putInt(offset, node._dimensionValue);
    offset += Integer.BYTES;

    dataBuffer.putInt(offset, node._startDocId);
    offset += Integer.BYTES;

    dataBuffer.putInt(offset, node._endDocId);
    offset += Integer.BYTES;

    dataBuffer.putInt(offset, node._aggregatedDocId);
    offset += Integer.BYTES;

    dataBuffer.putInt(offset, firstChildId);
    offset += Integer.BYTES;

    dataBuffer.putInt(offset, lastChildId);
    offset += Integer.BYTES;

    return offset;
  }
}
