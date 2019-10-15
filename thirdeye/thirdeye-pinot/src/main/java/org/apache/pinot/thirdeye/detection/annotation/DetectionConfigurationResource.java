/*
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

package org.apache.pinot.thirdeye.detection.annotation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pinot.thirdeye.detection.annotation.registry.DetectionRegistry;
import io.swagger.annotations.ApiParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;


@Path("/detection/annotation")
public class DetectionConfigurationResource {
  private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static DetectionRegistry detectionRegistry = DetectionRegistry.getInstance();

  @GET
  public Response getConfigurations(@ApiParam("tag") String tag) throws Exception {
    return Response.ok(
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(detectionRegistry.getAllAnnotation()))
        .build();
  }
}
