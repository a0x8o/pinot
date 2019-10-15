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

package org.apache.pinot.thirdeye.detection.algorithm;

import org.apache.pinot.thirdeye.dataframe.BooleanSeries;
import org.apache.pinot.thirdeye.dataframe.DataFrame;
import org.apache.pinot.thirdeye.dataframe.util.MetricSlice;
import org.apache.pinot.thirdeye.datalayer.dto.DetectionConfigDTO;
import org.apache.pinot.thirdeye.datalayer.dto.MergedAnomalyResultDTO;
import org.apache.pinot.thirdeye.detection.DataProvider;
import org.apache.pinot.thirdeye.detection.DetectionPipelineResult;
import org.apache.pinot.thirdeye.detection.StaticDetectionPipeline;
import org.apache.pinot.thirdeye.detection.spi.model.InputData;
import org.apache.pinot.thirdeye.detection.spi.model.InputDataSpec;
import org.apache.pinot.thirdeye.rootcause.impl.MetricEntity;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.MapUtils;

import static org.apache.pinot.thirdeye.dataframe.util.DataFrameUtils.*;


/**
 * Simple threshold rule algorithm with (optional) upper and lower bounds on a metric value.
 */
public class ThresholdAlgorithm extends StaticDetectionPipeline {
  private final String COL_TOO_HIGH = "tooHigh";
  private final String COL_TOO_LOW = "tooLow";
  private final String COL_ANOMALY = "anomaly";

  private final double min;
  private final double max;
  private final MetricSlice slice;

  public ThresholdAlgorithm(DataProvider provider, DetectionConfigDTO config, long startTime, long endTime) {
    super(provider, config, startTime, endTime);
    this.min = MapUtils.getDoubleValue(config.getProperties(), "min", Double.NaN);
    this.max = MapUtils.getDoubleValue(config.getProperties(), "max", Double.NaN);

    String metricUrn = MapUtils.getString(config.getProperties(), "metricUrn");
    MetricEntity me = MetricEntity.fromURN(metricUrn);
    this.slice = MetricSlice.from(me.getId(), this.startTime, this.endTime, me.getFilters());
  }

  @Override
  public InputDataSpec getInputDataSpec() {
    return new InputDataSpec()
        .withTimeseriesSlices(Collections.singletonList(this.slice));
  }

  @Override
  public DetectionPipelineResult run(InputData data) {
    DataFrame df = data.getTimeseries().get(this.slice);

    // defaults
    df.addSeries(COL_TOO_HIGH, BooleanSeries.fillValues(df.size(), false));
    df.addSeries(COL_TOO_LOW, BooleanSeries.fillValues(df.size(), false));

    // max
    if (!Double.isNaN(this.max)) {
      df.addSeries(COL_TOO_HIGH, df.getDoubles(COL_VALUE).gt(this.max));
    }

    // min
    if (!Double.isNaN(this.min)) {
      df.addSeries(COL_TOO_LOW, df.getDoubles(COL_VALUE).lt(this.min));
    }

    df.mapInPlace(BooleanSeries.HAS_TRUE, COL_ANOMALY, COL_TOO_HIGH, COL_TOO_LOW);

    List<MergedAnomalyResultDTO> anomalies = this.makeAnomalies(this.slice, df, COL_ANOMALY);

    return new DetectionPipelineResult(anomalies)
        .setDiagnostics(Collections.singletonMap(DetectionPipelineResult.DIAGNOSTICS_DATA, (Object) df.dropAllNullColumns()));
  }
}
