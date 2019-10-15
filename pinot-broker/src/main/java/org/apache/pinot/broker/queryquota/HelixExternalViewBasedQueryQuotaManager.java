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
package org.apache.pinot.broker.queryquota;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.RateLimiter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.helix.HelixConstants;
import org.apache.helix.HelixManager;
import org.apache.helix.ZNRecord;
import org.apache.helix.model.ExternalView;
import org.apache.helix.store.zk.ZkHelixPropertyStore;
import org.apache.pinot.broker.broker.helix.ClusterChangeHandler;
import org.apache.pinot.common.config.QuotaConfig;
import org.apache.pinot.common.config.TableConfig;
import org.apache.pinot.common.config.TableNameBuilder;
import org.apache.pinot.common.metadata.ZKMetadataProvider;
import org.apache.pinot.common.metrics.BrokerGauge;
import org.apache.pinot.common.metrics.BrokerMetrics;
import org.apache.pinot.common.utils.CommonConstants;
import org.apache.pinot.common.utils.helix.HelixHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.pinot.common.utils.CommonConstants.Helix.BROKER_RESOURCE_INSTANCE;
import static org.apache.pinot.common.utils.CommonConstants.Helix.TableType;


public class HelixExternalViewBasedQueryQuotaManager implements ClusterChangeHandler, QueryQuotaManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(HelixExternalViewBasedQueryQuotaManager.class);
  private static final int TIME_RANGE_IN_SECOND = 1;

  private final AtomicInteger _lastKnownBrokerResourceVersion = new AtomicInteger(-1);
  private final Map<String, QueryQuotaConfig> _rateLimiterMap = new ConcurrentHashMap<>();

  private HelixManager _helixManager;
  private BrokerMetrics _brokerMetrics;

  @Override
  public void init(HelixManager helixManager) {
    Preconditions.checkState(_helixManager == null, "HelixExternalViewBasedQueryQuotaManager is already initialized");
    _helixManager = helixManager;
  }

  @Override
  public void processClusterChange(HelixConstants.ChangeType changeType) {
    Preconditions
        .checkState(changeType == HelixConstants.ChangeType.EXTERNAL_VIEW, "Illegal change type: " + changeType);
    processQueryQuotaChange();
  }

  /**
   * Initialize dynamic rate limiter with table query quota.
   * @param tableConfig table config.
   * @param brokerResource broker resource which stores all the broker states of each table.
   */
  public void initTableQueryQuota(TableConfig tableConfig, ExternalView brokerResource) {
    String tableNameWithType = tableConfig.getTableName();
    String rawTableName = TableNameBuilder.extractRawTableName(tableNameWithType);
    LOGGER.info("Initializing rate limiter for table {}", tableNameWithType);

    // Check whether qps quotas from both tables are the same.
    QuotaConfig offlineQuotaConfig;
    QuotaConfig realtimeQuotaConfig;
    CommonConstants.Helix.TableType tableType = tableConfig.getTableType();
    if (tableType == CommonConstants.Helix.TableType.OFFLINE) {
      offlineQuotaConfig = tableConfig.getQuotaConfig();
      realtimeQuotaConfig = getQuotaConfigFromPropertyStore(rawTableName, CommonConstants.Helix.TableType.REALTIME);
    } else {
      realtimeQuotaConfig = tableConfig.getQuotaConfig();
      offlineQuotaConfig = getQuotaConfigFromPropertyStore(rawTableName, CommonConstants.Helix.TableType.OFFLINE);
    }
    // Log a warning if MaxQueriesPerSecond are set different.
    if ((offlineQuotaConfig != null && !Strings.isNullOrEmpty(offlineQuotaConfig.getMaxQueriesPerSecond())) && (
        realtimeQuotaConfig != null && !Strings.isNullOrEmpty(realtimeQuotaConfig.getMaxQueriesPerSecond()))) {
      if (!offlineQuotaConfig.getMaxQueriesPerSecond().equals(realtimeQuotaConfig.getMaxQueriesPerSecond())) {
        LOGGER.warn(
            "Attention! The values of MaxQueriesPerSecond for table {} are set different! Offline table qps quota: {}, Real-time table qps quota: {}",
            rawTableName, offlineQuotaConfig.getMaxQueriesPerSecond(), realtimeQuotaConfig.getMaxQueriesPerSecond());
      }
    }

    // Create rate limiter
    createRateLimiter(tableNameWithType, brokerResource, tableConfig.getQuotaConfig());
  }

  /**
   * Drop table query quota.
   * @param tableNameWithType table name with type.
   */
  public void dropTableQueryQuota(String tableNameWithType) {
    LOGGER.info("Dropping rate limiter for table {}", tableNameWithType);
    removeRateLimiter(tableNameWithType);
  }

  /** Remove or update rate limiter if another table with the same raw table name but different type is still using the quota config.
   * @param tableNameWithType table name with type
   */
  private void removeRateLimiter(String tableNameWithType) {
    _rateLimiterMap.remove(tableNameWithType);
  }

  /**
   * Get QuotaConfig from property store.
   * @param rawTableName table name without table type.
   * @param tableType table type: offline or real-time.
   * @return QuotaConfig, which could be null.
   */
  private QuotaConfig getQuotaConfigFromPropertyStore(String rawTableName, CommonConstants.Helix.TableType tableType) {
    ZkHelixPropertyStore<ZNRecord> propertyStore = _helixManager.getHelixPropertyStore();

    String tableNameWithType = TableNameBuilder.forType(tableType).tableNameWithType(rawTableName);
    TableConfig tableConfig = ZKMetadataProvider.getTableConfig(propertyStore, tableNameWithType);
    if (tableConfig == null) {
      return null;
    }
    return tableConfig.getQuotaConfig();
  }

  /**
   * Create a rate limiter for a table.
   * @param tableNameWithType table name with table type.
   * @param brokerResource broker resource which stores all the broker states of each table.
   * @param quotaConfig quota config of the table.
   */
  private void createRateLimiter(String tableNameWithType, ExternalView brokerResource, QuotaConfig quotaConfig) {
    if (quotaConfig == null || Strings.isNullOrEmpty(quotaConfig.getMaxQueriesPerSecond())) {
      LOGGER.info("No qps config specified for table: {}", tableNameWithType);
      return;
    }

    if (brokerResource == null) {
      LOGGER.warn("Failed to init qps quota for table {}. No broker resource connected!", tableNameWithType);
      return;
    }

    Map<String, String> stateMap = brokerResource.getStateMap(tableNameWithType);
    int otherOnlineBrokerCount = 0;

    // If stateMap is null, that means this broker is the first broker for this table.
    if (stateMap != null) {
      for (Map.Entry<String, String> state : stateMap.entrySet()) {
        if (!_helixManager.getInstanceName().equals(state.getKey()) && state.getValue()
            .equals(CommonConstants.Helix.StateModel.SegmentOnlineOfflineStateModel.ONLINE)) {
          otherOnlineBrokerCount++;
        }
      }
    }

    int onlineCount = otherOnlineBrokerCount + 1;
    LOGGER.info("The number of online brokers for table {} is {}", tableNameWithType, onlineCount);

    // Get the dynamic rate
    double overallRate;
    if (quotaConfig.isMaxQueriesPerSecondValid()) {
      overallRate = Double.parseDouble(quotaConfig.getMaxQueriesPerSecond());
    } else {
      LOGGER.error("Failed to init qps quota: error when parsing qps quota: {} for table: {}",
          quotaConfig.getMaxQueriesPerSecond(), tableNameWithType);
      return;
    }

    double perBrokerRate = overallRate / onlineCount;
    QueryQuotaConfig queryQuotaConfig =
        new QueryQuotaConfig(RateLimiter.create(perBrokerRate), new HitCounter(TIME_RANGE_IN_SECOND));
    _rateLimiterMap.put(tableNameWithType, queryQuotaConfig);
    LOGGER.info(
        "Rate limiter for table: {} has been initialized. Overall rate: {}. Per-broker rate: {}. Number of online broker instances: {}",
        tableNameWithType, overallRate, perBrokerRate, onlineCount);
  }

  /**
   * {@inheritDoc}
   * <p>Acquires a token from rate limiter based on the table name.
   *
   * @return true if there is no query quota specified for the table or a token can be acquired, otherwise return false.
   */
  @Override
  public boolean acquire(String tableName) {
    LOGGER.debug("Trying to acquire token for table: {}", tableName);
    String offlineTableName = null;
    String realtimeTableName = null;
    QueryQuotaConfig offlineTableQueryQuotaConfig = null;
    QueryQuotaConfig realtimeTableQueryQuotaConfig = null;

    CommonConstants.Helix.TableType tableType = TableNameBuilder.getTableTypeFromTableName(tableName);
    if (tableType == CommonConstants.Helix.TableType.OFFLINE) {
      offlineTableName = tableName;
      offlineTableQueryQuotaConfig = _rateLimiterMap.get(tableName);
    } else if (tableType == CommonConstants.Helix.TableType.REALTIME) {
      realtimeTableName = tableName;
      realtimeTableQueryQuotaConfig = _rateLimiterMap.get(tableName);
    } else {
      offlineTableName = TableNameBuilder.OFFLINE.tableNameWithType(tableName);
      realtimeTableName = TableNameBuilder.REALTIME.tableNameWithType(tableName);
      offlineTableQueryQuotaConfig = _rateLimiterMap.get(offlineTableName);
      realtimeTableQueryQuotaConfig = _rateLimiterMap.get(realtimeTableName);
    }

    boolean offlineQuotaOk =
        offlineTableQueryQuotaConfig == null || tryAcquireToken(offlineTableName, offlineTableQueryQuotaConfig);
    boolean realtimeQuotaOk =
        realtimeTableQueryQuotaConfig == null || tryAcquireToken(realtimeTableName, realtimeTableQueryQuotaConfig);

    return offlineQuotaOk && realtimeQuotaOk;
  }

  /**
   * Try to acquire token from rate limiter. Emit the utilization of the qps quota if broker metric isn't null.
   * @param tableNameWithType table name with type.
   * @param queryQuotaConfig query quota config for type-specific table.
   * @return true if there's no qps quota for that table, or a token is acquired successfully.
   */
  private boolean tryAcquireToken(String tableNameWithType, QueryQuotaConfig queryQuotaConfig) {
    // Use hit counter to count the number of hits.
    queryQuotaConfig.getHitCounter().hit();

    RateLimiter rateLimiter = queryQuotaConfig.getRateLimiter();
    double perBrokerRate = rateLimiter.getRate();

    // Emit the qps capacity utilization rate.
    int numHits = queryQuotaConfig.getHitCounter().getHitCount();
    if (_brokerMetrics != null) {
      int percentageOfCapacityUtilization = (int) (numHits * 100 / perBrokerRate);
      LOGGER.debug("The percentage of rate limit capacity utilization is {}", percentageOfCapacityUtilization);
      _brokerMetrics.setValueOfTableGauge(tableNameWithType, BrokerGauge.QUERY_QUOTA_CAPACITY_UTILIZATION_RATE,
          percentageOfCapacityUtilization);
    }

    if (!rateLimiter.tryAcquire()) {
      LOGGER.info("Quota is exceeded for table: {}. Per-broker rate: {}. Current qps: {}", tableNameWithType,
          perBrokerRate, numHits);
      return false;
    }
    // Token is successfully acquired.
    return true;
  }

  public void setBrokerMetrics(BrokerMetrics brokerMetrics) {
    _brokerMetrics = brokerMetrics;
  }

  @VisibleForTesting
  public int getRateLimiterMapSize() {
    return _rateLimiterMap.size();
  }

  @VisibleForTesting
  public void cleanUpRateLimiterMap() {
    _rateLimiterMap.clear();
  }

  /**
   * Process query quota change when number of online brokers has changed.
   */
  public void processQueryQuotaChange() {
    LOGGER.info("Start processing qps quota change.");
    long startTime = System.currentTimeMillis();

    ExternalView currentBrokerResource = HelixHelper
        .getExternalViewForResource(_helixManager.getClusterManagmentTool(), _helixManager.getClusterName(),
            BROKER_RESOURCE_INSTANCE);
    if (currentBrokerResource == null) {
      LOGGER.warn("Finish processing qps quota change: external view for broker resource is null!");
      return;
    }
    int currentVersionNumber = currentBrokerResource.getRecord().getVersion();
    if (currentVersionNumber == _lastKnownBrokerResourceVersion.get()) {
      LOGGER.info("No qps quota change: external view for broker resource remains the same.");
      return;
    }

    int numRebuilt = 0;
    for (Map.Entry<String, QueryQuotaConfig> entry : _rateLimiterMap.entrySet()) {
      String tableNameWithType = entry.getKey();
      QueryQuotaConfig queryQuotaConfig = entry.getValue();
      String rawTableName = TableNameBuilder.extractRawTableName(tableNameWithType);
      TableType tableType = TableNameBuilder.getTableTypeFromTableName(tableNameWithType);

      // Get latest quota config for table.
      QuotaConfig quotaConfig = getQuotaConfigFromPropertyStore(rawTableName, tableType);
      if (quotaConfig == null || quotaConfig.getMaxQueriesPerSecond() == null || !quotaConfig
          .isMaxQueriesPerSecondValid()) {
        LOGGER.info("No query quota config or the config is invalid for Table {}. Removing its rate limit.",
            tableNameWithType);
        removeRateLimiter(tableNameWithType);
        continue;
      }

      // Get number of online brokers.
      Map<String, String> stateMap = currentBrokerResource.getStateMap(tableNameWithType);
      if (stateMap == null) {
        LOGGER.info("No broker resource for Table {}. Removing its rate limit.", tableNameWithType);
        removeRateLimiter(tableNameWithType);
        continue;
      }
      int otherOnlineBrokerCount = 0;
      for (Map.Entry<String, String> state : stateMap.entrySet()) {
        if (!_helixManager.getInstanceName().equals(state.getKey()) && state.getValue()
            .equals(CommonConstants.Helix.StateModel.SegmentOnlineOfflineStateModel.ONLINE)) {
          otherOnlineBrokerCount++;
        }
      }
      int onlineBrokerCount = otherOnlineBrokerCount + 1;

      double overallRate = Double.parseDouble(quotaConfig.getMaxQueriesPerSecond());
      double latestRate = overallRate / onlineBrokerCount;
      double previousRate = queryQuotaConfig.getRateLimiter().getRate();
      if (Math.abs(latestRate - previousRate) > 0.001) {
        queryQuotaConfig.getRateLimiter().setRate(latestRate);
        LOGGER.info(
            "Rate limiter for table: {} has been updated. Overall rate: {}. Previous per-broker rate: {}. New per-broker rate: {}. Number of online broker instances: {}",
            tableNameWithType, overallRate, previousRate, latestRate, onlineBrokerCount);
        numRebuilt++;
      }
    }
    _lastKnownBrokerResourceVersion.set(currentVersionNumber);
    long endTime = System.currentTimeMillis();
    LOGGER
        .info("Processed query quota change in {}ms, {} out of {} query quota configs rebuilt.", (endTime - startTime),
            numRebuilt, _rateLimiterMap.size());
  }
}
