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

package org.apache.pinot.thirdeye.anomaly.alert;

import com.google.common.base.Preconditions;
import org.apache.pinot.thirdeye.anomaly.alert.v2.AlertJobSchedulerV2;
import org.apache.pinot.thirdeye.datalayer.bao.AlertConfigManager;
import org.apache.pinot.thirdeye.datalayer.dto.AlertConfigDTO;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.quartz.SchedulerException;

@Path("/alert-job")
@Produces(MediaType.APPLICATION_JSON)
@Deprecated  // not used
public class AlertJobResource {
  private final AlertJobSchedulerV2 alertJobScheduler;
  private final AlertConfigManager alertConfigurationDAO;

  public AlertJobResource(AlertJobSchedulerV2 alertJobScheduler,
      AlertConfigManager alertConfigurationDAO) {
    this.alertJobScheduler = alertJobScheduler;
    this.alertConfigurationDAO = alertConfigurationDAO;
  }

  @GET
  public List<String> showActiveJobs() throws SchedulerException {
    return alertJobScheduler.getScheduledJobs();
  }

  @POST
  @Path("/{id}")
  public Response enable(@PathParam("id") Long id) throws Exception {
    toggleActive(id, true);
    alertJobScheduler.startJob(id);
    return Response.ok().build();
  }

  @DELETE
  @Path("/{id}")
  public Response disable(@PathParam("id") Long id) throws Exception {
    toggleActive(id, false);
    alertJobScheduler.stopJob(id);
    return Response.ok().build();
  }

  private void toggleActive(Long id, boolean state) {
    AlertConfigDTO alertConfig = alertConfigurationDAO.findById(id);
    Preconditions.checkNotNull(alertConfig, "Alert config not found");
    alertConfig.setActive(state);
    alertConfigurationDAO.update(alertConfig);
  }

  @POST
  @Path("/{id}/restart")
  public Response restart(@PathParam("id") Long id) throws Exception {
    alertJobScheduler.stopJob(id);
    alertJobScheduler.startJob(id);
    return Response.ok().build();
  }
}
