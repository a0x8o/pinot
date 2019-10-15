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
package org.apache.pinot.server.api.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.pinot.core.segment.memory.PinotDataBuffer;


/**
 * Debug endpoint to check memory allocation.
 */
@Api(value = "debug", description = "Debug information", tags = "Debug")
@Path("debug")
public class MmapDebugResource {

  @GET
  @Path("memory/offheap")
  @ApiOperation(value = "View current off-heap allocations", notes = "Lists all off-heap allocations and their associated sizes")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Success")})
  @Produces(MediaType.APPLICATION_JSON)
  public List<String> getOffHeapSizes() {
    return PinotDataBuffer.getBufferInfo();
  }
}
