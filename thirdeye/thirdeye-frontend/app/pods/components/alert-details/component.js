/**
 * This component displays the alert details. It would be used in places like Alert Details, and Preview pages/modules.
 * @module components/alert-details
 * @property {Object} alertYaml - the alert yaml
 * @property {boolean} disableYamlSave  - detect flag for yaml changes
 * @example
   {{#alert-details
     alertYaml=alertYaml
     disableYamlSave=disableYamlSave
   }}
     {{yield}}
   {{/alert-details}}
 * @exports alert-details
 */

import Component from '@ember/component';
import { computed, set, get, getProperties } from '@ember/object';
import { later } from '@ember/runloop';
import { checkStatus, humanizeFloat, postProps, stripNonFiniteValues } from 'thirdeye-frontend/utils/utils';
import { toastOptions } from 'thirdeye-frontend/utils/constants';
import { colorMapping, makeTime, toMetricLabel, extractTail } from 'thirdeye-frontend/utils/rca-utils';
import { getYamlPreviewAnomalies,
  getAnomaliesByAlertId,
  getBounds  } from 'thirdeye-frontend/utils/anomaly';
import { inject as service } from '@ember/service';
import { task } from 'ember-concurrency';
import floatToPercent from 'thirdeye-frontend/utils/float-to-percent';
import { setUpTimeRangeOptions } from 'thirdeye-frontend/utils/manage-alert-utils';
import moment from 'moment';
import _ from 'lodash';

const TABLE_DATE_FORMAT = 'MMM DD, hh:mm A'; // format for anomaly table
const TIME_PICKER_INCREMENT = 5; // tells date picker hours field how granularly to display time
const DEFAULT_ACTIVE_DURATION = '1m'; // setting this date range selection as default (Last 24 Hours)
const UI_DATE_FORMAT = 'MMM D, YYYY hh:mm a'; // format for date picker to use (usually varies by route or metric)
const DISPLAY_DATE_FORMAT = 'YYYY-MM-DD HH:mm'; // format used consistently across app to display custom date range
const TIME_RANGE_OPTIONS = ['48h', '1w', '1m', '3m'];
const ANOMALY_LEGEND_THRESHOLD = 20; // If number of anomalies is larger than this threshold, don't show the legend

export default Component.extend({
  anomaliesApiService: service('services/api/anomalies'),
  notifications: service('toast'),
  timeseries: null,
  isLoading: false,
  analysisRange: [moment().subtract(2, 'day').startOf('day').valueOf(), moment().add(1, 'day').startOf('day').valueOf()],
  isPendingData: false,
  colorMapping: colorMapping,
  zoom: {
    enabled: true,
    rescale: true
  },
  point: {
    show: false,
    r: 5
  },
  errorTimeseries: null,
  metricUrn: null,
  metricUrnList: [],
  errorBaseline: null,
  compareMode: 'wo1w',
  baseline: null,
  showDetails: false,
  componentId: 'timeseries-chart',
  anomaliesOld: [],
  anomaliesNew: [],
  selectedBaseline: null,
  pageSize: 10,
  currentPage: 1,
  isPreviewMode: false,
  alertId: null,
  alertData: null,
  anomalyResponseNames: ['Not reviewed yet', 'Yes - unexpected', 'Expected temporary change', 'Expected permanent change', 'No change observed'],
  selectedDimension: null,
  isReportSuccess: false,
  isReportFailure: false,
  openReportModal: false,
  missingAnomalyProps: {},
  uniqueTimeSeries: [],
  selectedRule: null,
  isLoadingTimeSeries: false,
  granularity: null,
  alertYaml: null,
  dimensionExploration: null,
  getAnomaliesError:false, // stops the component from fetching more anomalies until user changes state
  detectionHealth: null, // result of call to detection/health/{id}, passed in by parent
  timeWindowSize: null, // passed in by parent, which retrieves from endpoint.  Do not set
  originalYaml: null, // passed by parent in Edit Alert Preview only. Do not set


  /**
   * This needs to be a computed variable until there is an endpoint for showing predicted with any metricurn
   * @type {Array}
   */
  baselineOptions: computed(
    'showRules',
    function() {
      const showRules = get(this, 'showRules');
      let options;
      if (showRules) {
        options = [
          { name: 'predicted', isActive: true},
          { name: 'wo1w', isActive: false},
          { name: 'wo2w', isActive: false},
          { name: 'wo3w', isActive: false},
          { name: 'wo4w', isActive: false},
          { name: 'mean4w', isActive: false},
          { name: 'median4w', isActive: false},
          { name: 'min4w', isActive: false},
          { name: 'max4w', isActive: false},
          { name: 'none', isActive: false}
        ];
      } else {
        options = [
          { name: 'wo1w', isActive: true},
          { name: 'wo2w', isActive: false},
          { name: 'wo3w', isActive: false},
          { name: 'wo4w', isActive: false},
          { name: 'mean4w', isActive: false},
          { name: 'median4w', isActive: false},
          { name: 'min4w', isActive: false},
          { name: 'max4w', isActive: false},
          { name: 'none', isActive: false}
        ];
      }
      return options;
    }
  ),

  /**
   * Separate time range for anomalies in preview mode
   * @type {Array}
   */
  anomaliesRange: computed(
    'analysisRange',
    function() {
      const analysisRange = get(this, 'analysisRange');
      let range = [];
      range.push(analysisRange[0]);
      // set end to now if the end time is in the future
      const end = Math.min(moment().valueOf(), analysisRange[1]);
      range.push(end);
      return range;
    }
  ),

  /**
   * Rules to display in rules dropdown
   * @type {Array}
   */
  ruleOptions: computed(
    'uniqueTimeSeries',
    function() {
      const uniqueTimeSeries = get(this, 'uniqueTimeSeries');
      if (uniqueTimeSeries) {
        return [...new Set(uniqueTimeSeries.map(series => series.detectorName))].map(detector => {
          const nameOnly = detector.split(':')[0];
          return {
            detectorName: detector,
            name: nameOnly
          };
        });
      }
      return [];
    }
  ),

  /**
   * flag to differentiate preview loading and graph loading
   * @type {Boolean}
   */
  isPreviewLoading: computed(
    'isPreviewMode',
    'isLoading',
    function() {
      return (get(this, 'isPreviewMode') && get(this, 'isLoading'));
    }
  ),

  /**
   * flag to differentiate whether we show bounds and rules or not
   * @type {Boolean}
   */
  showRules: computed(
    'isPreviewMode',
    'granularity',
    'dimensionExploration',
    function() {
      const {
        isPreviewMode,
        granularity,
        dimensionExploration
      } = this.getProperties('isPreviewMode', 'granularity', 'dimensionExploration');
      return (isPreviewMode || (!dimensionExploration && ((granularity || '').includes('DAYS'))));
    }
  ),

  /**
   * dimensions to display in dimensions dropdown
   * @type {Array}
   */
  dimensionOptions: computed(
    'metricUrnList',
    function() {
      const metricUrnList = get(this, 'metricUrnList');
      let options = [];
      metricUrnList.forEach(urn => {
        let dimensionUrn = toMetricLabel(extractTail(decodeURIComponent(urn)));
        dimensionUrn = dimensionUrn ? dimensionUrn : 'All Dimensions';
        options.push(dimensionUrn);
      });
      return options;
    }
  ),

  /**
   * Whether the alert has multiple dimensions
   * @type {Boolean}
   */
  alertHasDimensions: computed(
    'metricUrnList',
    function() {
      const metricUrnList = get(this, 'metricUrnList');
      return (metricUrnList.length > 1);
    }
  ),

  /**
   * Table pagination: number of pages to display
   * @type {Number}
   */
  paginationSize: computed(
    'pagesNum',
    'pageSize',
    function() {
      const { pagesNum, pageSize } = this.getProperties('pagesNum', 'pageSize');
      return Math.min(pagesNum, pageSize/2);
    }
  ),

  /**
   * Table pagination: total Number of pages to display
   * @type {Number}
   */
  pagesNum: computed(
    'tableAnomalies',
    'pageSize',
    function() {
      const { tableAnomalies, pageSize } = this.getProperties('tableAnomalies', 'pageSize');
      const anomalyCount = tableAnomalies.length || 0;
      return Math.ceil(anomalyCount/pageSize);
    }
  ),

  /**
   * Return state of anomalies and time series for updating state correctly
   * 1 - set to old (Alert Overview or Create Alert Preview w/o old)
   * 2 - set to new (Edit Alert Preview with old or Create Alert Preview w/o new)
   * 3 - shuffle then set to new (Create Alert Preview with 2 sets already)
   * 4 - get originalYaml then get updated (Edit Alert Preview w/o any anomalies loaded yet)
   * 5 - error getting anomalies
   * @type {Number}
   */
  stateOfAnomaliesAndTimeSeries: computed(
    'isPreviewMode',
    'anomaliesOld',
    'anomaliesNew',
    'isEditMode',
    'getAnomaliesError',
    function() {
      let state = 1;
      if (this.get('isPreviewMode')) {
        // Not Alert Preview
        if (!_.isEmpty(this.get('anomaliesOld'))) {
          // At least one set of anomalies already loaded
          if (this.get('isEditMode') || _.isEmpty(this.get('anomaliesNew'))) {
            // replace new if Edit Alert Preview or it's Create Alert Preview with only one set
            state = 2;
          } else {
            // Create Alert Preview with 2 sets of anomalies - shuffle
            state = 3;
          }
        } else if (this.get('isEditMode')) {
          // Edit Alert Preview w/o any anomalies
          state = 4;
        }
      }
      if (this.get('getAnomaliesError')) {
        state = 5;
      }
      return state;
    }
  ),

  /**
   * date-time-picker: indicates the date format to be used based on granularity
   * @type {String}
   */
  uiDateFormat: computed('alertData.windowUnit', function() {
    const rawGranularity = this.get('alertData.bucketUnit');
    const granularity = rawGranularity ? rawGranularity.toLowerCase() : '';

    switch(granularity) {
      case 'days':
        return 'MMM D, YYYY';
      case 'hours':
        return 'MMM D, YYYY h a';
      default:
        return 'MMM D, YYYY hh:mm a';
    }
  }),

  disablePreviewButton: computed(
    'alertYaml',
    'isLoading',
    function() {
      return (get(this, 'alertYaml') === null || get(this, 'isLoading') === true);
    }
  ),

  axis: computed(
    'analysisRange',
    function () {
      const analysisRange = get(this, 'analysisRange');

      return {
        y: {
          show: true,
          tick: {
            format: function(d){return humanizeFloat(d);}
          }
        },
        y2: {
          show: false,
          min: 0,
          max: 1
        },
        x: {
          type: 'timeseries',
          show: true,
          min: analysisRange[0],
          max: analysisRange[1],
          tick: {
            fit: false,
            format: (d) => {
              const t = makeTime(d);
              if (t.valueOf() === t.clone().startOf('day').valueOf()) {
                return t.format('MMM D');
              }
              return t.format('h:mm a');
            }
          }
        }
      };
    }
  ),

  /**
   * Old anomalies to show in graph based on current dimension/rule combination
   * @type {Array}
   */
  filteredAnomaliesOld: computed(
    'anomaliesOld',
    'metricUrn',
    'selectedRule',
    'selectedDimension',
    'showRules',
    function() {
      let filteredAnomaliesOld = [];
      const {
        metricUrn, anomaliesOld, selectedRule, showRules
      } = getProperties(this, 'metricUrn', 'anomaliesOld', 'selectedRule', 'showRules');
      if (!_.isEmpty(anomaliesOld)) {

        filteredAnomaliesOld = anomaliesOld.filter(anomaly => {
          if (anomaly.metricUrn === metricUrn) {
            if(showRules && anomaly.properties && typeof anomaly.properties === 'object' && selectedRule && typeof selectedRule === 'object') {
              return ((anomaly.properties.detectorComponentName || '').includes(selectedRule.detectorName));
            } else if (!showRules) {
              // This is necessary until we surface rule selector in Alert Overview
              return true;
            }
          }
          return false;
        });
      }
      return filteredAnomaliesOld;
    }
  ),



  /**
   * Old anomalies to show in graph based on current dimension/rule combination
   * @type {Array}
   */
  filteredAnomaliesNew: computed(
    'anomaliesNew',
    'metricUrn',
    'selectedRule',
    'selectedDimension',
    'showRules',
    function() {
      let filteredAnomaliesNew = [];
      const {
        metricUrn, anomaliesNew, selectedRule, showRules
      } = getProperties(this, 'metricUrn', 'anomaliesNew', 'selectedRule', 'showRules');
      if (!_.isEmpty(anomaliesNew)) {

        filteredAnomaliesNew = anomaliesNew.filter(anomaly => {
          if (anomaly.metricUrn === metricUrn) {
            if(showRules && anomaly.properties && typeof anomaly.properties === 'object' && selectedRule && typeof selectedRule === 'object') {
              return (anomaly.properties.detectorComponentName.includes(selectedRule.detectorName));
            } else if (!showRules) {
              // This is necessary until we surface rule selector in Alert Overview
              return true;
            }
          }
          return false;
        });
      }
      return filteredAnomaliesNew;
    }
  ),

  legend: computed(
    'numFilteredAnomalies',
    function() {
      if (get(this, 'numFilteredAnomalies') > ANOMALY_LEGEND_THRESHOLD) {
        return {
          show: false,
          position: 'right'
        };
      }
      return {
        show: true,
        position: 'right'
      };
    }
  ),

  numFilteredAnomalies: computed(
    'filteredAnomaliesOld.@each',
    'filteredAnomaliesNew.@each',
    function() {
      const filteredAnomalies = [...this.get('filteredAnomaliesOld'), ...this.get('filteredAnomaliesNew')];
      return filteredAnomalies.length;
    }
  ),

  series: computed(
    'filteredAnomaliesOld.@each',
    'filteredAnomaliesNew.@each',
    'timeseries',
    'baseline',
    'analysisRange',
    'selectedRule',
    'metricUrn',
    function () {
      const {
        filteredAnomaliesOld, filteredAnomaliesNew, timeseries, baseline, showRules, isPreviewMode
      } = getProperties(this, 'filteredAnomaliesOld', 'filteredAnomaliesNew',
        'timeseries', 'baseline', 'showRules', 'isPreviewMode');

      const series = {};
      // Should be displayed in Create Mode of Preview with one set of anomalies
      let anomaliesOldLabel = 'Current Settings Anomalies';
      // Should be displayed in Create Mode of Preview, if there are two sets of anomalies
      if (isPreviewMode && (this.get('stateOfAnomaliesAndTimeSeries') === 3)) {
        anomaliesOldLabel = 'Old Settings Anomalies';
      // Should be displayed in Alert Overview or in Edit Mode of Preview ('real' anomalies saved in db)
      } else if (!isPreviewMode || this.get('isEditMode')) {
        anomaliesOldLabel = 'Current Anomalies';
      }
      const anomaliesNewLabel = 'New Settings Anomalies';
      // The current time series has a different naming convention in Preview
      if (showRules) {
        if (timeseries && !_.isEmpty(timeseries.current)) {
          series['Current'] = {
            timestamps: timeseries.timestamp,
            values: stripNonFiniteValues(timeseries.current),
            type: 'line',
            color: 'screenshot-current'
          };
        }
      } else {
        if (timeseries && !_.isEmpty(timeseries.value)) {
          series['Current'] = {
            timestamps: timeseries.timestamp,
            values: stripNonFiniteValues(timeseries.value),
            type: 'line',
            color: 'screenshot-current'
          };
        }
      }

      if (baseline && !_.isEmpty(baseline.value)) {
        series['Baseline'] = {
          timestamps: baseline.timestamp,
          values: stripNonFiniteValues(baseline.value),
          type: 'line',
          color: 'screenshot-predicted'
        };
      }

      if (baseline && !_.isEmpty(baseline.upper_bound)) {
        series['Upper and lower bound'] = {
          timestamps: baseline.timestamp,
          values: stripNonFiniteValues(baseline.upper_bound),
          type: 'line',
          color: 'screenshot-bounds'
        };
      }

      if (baseline && !_.isEmpty(baseline.lower_bound)) {
        series['lowerBound'] = {
          timestamps: baseline.timestamp,
          values: stripNonFiniteValues(baseline.lower_bound),
          type: 'line',
          color: 'screenshot-bounds'
        };
      }
      // build set of anomalous values (older of 2 sets of anomalies)
      if (!_.isEmpty(filteredAnomaliesOld) && timeseries && !_.isEmpty(series.Current)) {
        const valuesOld = [];
        // needed because anomalies with startTime before time window are possible
        let currentAnomaly = filteredAnomaliesOld.find(anomaly => {
          return anomaly.startTime <= series.Current.timestamps[0];
        });
        let inAnomalyRange = currentAnomaly ? true : false;
        let anomalyEdgeValues = [];
        let anomalyEdgeTimestamps = [];
        for (let i = 0; i < series.Current.timestamps.length; ++i) {
          if (!inAnomalyRange) {
            currentAnomaly = filteredAnomaliesOld.find(anomaly => {
              return anomaly.startTime === series.Current.timestamps[i];
            });
            if (currentAnomaly) {
              inAnomalyRange = true;
              valuesOld.push(series.Current.values[i]);
              anomalyEdgeValues.push(series.Current.values[i]);
              anomalyEdgeTimestamps.push(series.Current.timestamps[i]);
            } else {
              valuesOld.push(null);
            }
          } else if (currentAnomaly.endTime === series.Current.timestamps[i]) {
            inAnomalyRange = false;
            // we don't want to include the endTime in anomaly range
            currentAnomaly = filteredAnomaliesOld.find(anomaly => {
              return anomaly.startTime === series.Current.timestamps[i];
            });
            if (currentAnomaly) {
              inAnomalyRange = true;
              valuesOld.push(series.Current.values[i]);
              anomalyEdgeValues.push(series.Current.values[i]);
              anomalyEdgeTimestamps.push(series.Current.timestamps[i]);
            } else {
              anomalyEdgeValues.push(series.Current.values[i-1]);
              anomalyEdgeTimestamps.push(series.Current.timestamps[i-1]);
              valuesOld.push(null);
            }
          } else {
            valuesOld.push(series.Current.values[i]);
          }
        }
        series[anomaliesOldLabel] = {
          timestamps: series.Current.timestamps,
          values: valuesOld,
          type: 'line',
          color: 'red'
        };
        series['old-anomaly-edges'] = {
          timestamps: anomalyEdgeTimestamps,
          values: anomalyEdgeValues,
          type: 'scatter',
          color: 'red'
        };
      }
      // build set of new anomalies
      if (!_.isEmpty(filteredAnomaliesNew) && timeseries && !_.isEmpty(series.Current)) {
        const valuesNew = [];
        // needed because anomalies with startTime before time window are possible
        let currentAnomaly = filteredAnomaliesNew.find(anomaly => {
          return anomaly.startTime <= series.Current.timestamps[0];
        });
        let inAnomalyRange = currentAnomaly ? true : false;
        let anomalyEdgeValues = [];
        let anomalyEdgeTimestamps = [];
        for (let i = 0; i < series.Current.timestamps.length; ++i) {
          if (!inAnomalyRange) {
            currentAnomaly = filteredAnomaliesNew.find(anomaly => {
              return anomaly.startTime === series.Current.timestamps[i];
            });
            if (currentAnomaly) {
              inAnomalyRange = true;
              valuesNew.push(1.0);
              anomalyEdgeValues.push(1.0);
              anomalyEdgeTimestamps.push(series.Current.timestamps[i]);
            } else {
              valuesNew.push(null);
            }
          } else if (currentAnomaly.endTime === series.Current.timestamps[i]) {
            inAnomalyRange = false;
            // we don't want to include the endTime in anomaly range
            currentAnomaly = filteredAnomaliesOld.find(anomaly => {
              return anomaly.startTime === series.Current.timestamps[i];
            });
            if (currentAnomaly) {
              inAnomalyRange = true;
              valuesNew.push(1.0);
              anomalyEdgeValues.push(1.0);
              anomalyEdgeTimestamps.push(series.Current.timestamps[i]);
            } else {
              anomalyEdgeValues.push(1.0);
              anomalyEdgeTimestamps.push(series.Current.timestamps[i-1]);
              valuesNew.push(null);
            }
          } else {
            valuesNew.push(1.0);
          }
        }
        series[anomaliesNewLabel] = {
          timestamps: series.Current.timestamps,
          values: valuesNew,
          type: 'line',
          color: 'grey',
          axis: 'y2'
        };
        series['new-anomaly-edges'] = {
          timestamps: anomalyEdgeTimestamps,
          values: anomalyEdgeValues,
          type: 'scatter',
          color: 'grey',
          axis: 'y2'
        };
      }

      return series;
    }
  ),

  /**
   * formats anomalies for table
   * @method tableAnomalies
   * @return {Array}
   */
  tableAnomalies: computed(
    'anomaliesOld',
    'anomaliesNew',
    function() {
      const {
        anomaliesOld,
        anomaliesNew,
        analysisRange,
        stateOfAnomaliesAndTimeSeries
      } = this.getProperties('anomaliesOld', 'anomaliesNew', 'analysisRange', 'stateOfAnomaliesAndTimeSeries');
      let tableData = [];
      const humanizedObject = {
        queryDuration: (get(this, 'duration') || DEFAULT_ACTIVE_DURATION),
        queryStart: analysisRange[0],
        queryEnd: analysisRange[1]
      };
      // we give the anomaly an arbitrary id for distinguishin in the frontend
      let fakeId = 0;
      if (anomaliesOld) {
        anomaliesOld.forEach(a => {
          // 'settings' field only matters if column for settings shown
          const dimensionKeys = Object.keys(a.dimensions || {});
          const dimensionValues = dimensionKeys.map(d => a.dimensions[d]);
          const dimensionsString = [...dimensionKeys, ...dimensionValues].join();
          set(a, 'dimensionStr', dimensionsString);
          set(a, 'settings', ((stateOfAnomaliesAndTimeSeries === 2) && this.get('isEditMode')) ? 'Current' : 'Old');
          set(a, 'id', (!a.id) ? fakeId : a.id);
          set(a, 'startDateStr', this._formatAnomaly(a));
          set(a, 'current', a.avgCurrentVal);
          set(a, 'baseline', a.avgBaselineVal);
          set(a, 'rule', this.get('_formattedRule')(a.properties));
          set(a, 'modifiedBy', this.get('_formattedModifiedBy')(a.feedback));
          set(a, 'start', a.startTime);
          set(a, 'end', a.endTime);
          set(a, 'feedback', a.feedback ? a.feedback.feedbackType : a.statusClassification);
          if (a.feedback === 'NONE') {
            set(a, 'feedback', 'NO_FEEDBACK');
          }
          let tableRow = this.get('anomaliesApiService').getHumanizedEntity(a, humanizedObject);
          tableData.push(tableRow);
          ++fakeId;
        });
      }
      if (anomaliesNew) {
        anomaliesNew.forEach(a => {
          // 'settings' field only matters if column for settings shown
          set(a, 'settings', 'New');
          // always give the new ones fake id's
          set(a, 'id', fakeId);
          set(a, 'startDateStr', this._formatAnomaly(a));
          set(a, 'current', a.avgCurrentVal);
          set(a, 'baseline', a.avgBaselineVal);
          set(a, 'rule', this.get('_formattedRule')(a.properties));
          set(a, 'modifiedBy', this.get('_formattedModifiedBy')(a.feedback));
          set(a, 'start', a.startTime);
          set(a, 'end', a.endTime);
          set(a, 'feedback', a.feedback ? a.feedback.feedbackType : a.statusClassification);
          if (a.feedback === 'NONE') {
            set(a, 'feedback', 'NO_FEEDBACK');
          }
          let tableRow = this.get('anomaliesApiService').getHumanizedEntity(a, humanizedObject);
          tableData.push(tableRow);
          ++fakeId;
        });
      }
      return tableData;
    }
  ),

  /**
   * generates columns for anomaly table
   * @method columns
   * @return {Array}
   */
  columns: computed(
    'alertHasDimensions',
    'isPreviewMode',
    'stateOfAnomaliesAndTimeSeries',
    'isEditMode',
    function() {
      const {
        alertHasDimensions,
        isPreviewMode,
        stateOfAnomaliesAndTimeSeries,
        isEditMode
      } = this.getProperties('alertHasDimensions', 'isPreviewMode',
        'stateOfAnomaliesAndTimeSeries', 'isEditMode');
      const settingsColumn = ((isEditMode && stateOfAnomaliesAndTimeSeries === 2) ||
      stateOfAnomaliesAndTimeSeries === 3) ? [{
          title: 'Detection Settings',
          propertyName: 'settings'
        }] : [];
      const startColumn = [{
        template: 'custom/anomalies-table/start-duration',
        title: 'Start / Duration (PDT)',
        propertyName: 'startDateStr',
        sortedBy: 'start'
      }];
      const dimensionColumn = alertHasDimensions ? [{
        template: 'custom/anomalies-table/dimensions-only',
        title: 'Dimensions',
        propertyName: 'dimensionStr'
      }] : [];
      const middleColumns = [{
        template: 'custom/anomalies-table/current-wow',
        title: 'Current / Predicted',
        propertyName: 'change'
      }, {
        propertyName: 'rule',
        title: 'Rule'
      }];
      const rightmostColumns = isPreviewMode ? [] : [{
        component: 'custom/anomalies-table/resolution',
        title: 'Feedback',
        propertyName: 'anomalyFeedback'
      }, {
        propertyName: 'modifiedBy',
        title: 'Modified'
      }, {
        component: 'custom/anomalies-table/investigation-link',
        title: 'RCA',
        propertyName: 'id'
      }];
      return [...settingsColumn, ...startColumn, ...dimensionColumn,
        ...middleColumns, ...rightmostColumns];
    }
  ),

  /**
   * Stats to display in cards
   * @type {Object[]} - array of objects, each of which represents a stats card
   */
  stats: computed(
    'anomaliesOld',
    'anomaliesNew',
    'stateOfAnomaliesAndTimeSeries',
    function() {
      const {
        anomaliesOld,
        isPreviewMode,
        isEditMode
      } = this.getProperties('anomaliesOld', 'isPreviewMode', 'isEditMode');
      if (!anomaliesOld) {
        return [];
      }
      let respondedAnomaliesCount = 0;
      let truePositives = 0;
      let falsePositives = 0;
      let falseNegatives = 0;
      let numberOfAnomalies = 0;
      anomaliesOld.forEach(function (attr) {
        numberOfAnomalies++;
        if(attr.anomaly && attr.anomaly.statusClassification) {
          const classification = attr.anomaly.statusClassification;
          if (classification !== 'NONE') {
            respondedAnomaliesCount++;
            if (classification === 'TRUE_POSITIVE') {
              truePositives++;
            } else if (classification === 'FALSE_POSITIVE') {
              falsePositives++;
            } else if (classification === 'FALSE_NEGATIVE') {
              falseNegatives++;
            }
          }
        }
      });

      const totalAnomaliesCount = numberOfAnomalies;
      const totalAlertsDescription = 'Total number of anomalies that occured over a period of time';
      let statsArray = [];
      if(!isPreviewMode || isEditMode) {
        const responseRate = respondedAnomaliesCount / totalAnomaliesCount;
        const precision = truePositives / (truePositives + falsePositives);
        const recall = truePositives / (truePositives + falseNegatives);
        const responseRateDescription = '% of anomalies that are reviewed';
        const precisionDescription = '% of all anomalies detected by the system that are true';
        const recallDescription = '% of all anomalies detected by the system';
        statsArray = [
          ['Anomalies', totalAlertsDescription, totalAnomaliesCount, 'digit'],
          ['Response Rate', responseRateDescription, floatToPercent(responseRate), 'percent'],
          ['Precision', precisionDescription, floatToPercent(precision), 'percent'],
          ['Recall', recallDescription, floatToPercent(recall), 'percent']
        ];
      } else {
        statsArray = [
          ['Anomalies', totalAlertsDescription, totalAnomaliesCount, 'digit']
        ];
      }
      return statsArray;
    }
  ),


  /**
   * Date types to display in the pills
   * @type {Object[]} - array of objects, each of which represents each date pill
   */
  pill: computed(
    'analysisRange', 'startDate', 'endDate', 'duration',
    function() {
      const analysisRange = get(this, 'analysisRange');
      const startDate = Number(analysisRange[0]);
      const endDate = Number(analysisRange[1]);
      const duration = get(this, 'duration') || DEFAULT_ACTIVE_DURATION;
      const predefinedRanges = {
        'Today': [moment().startOf('day'), moment().startOf('day').add(1, 'days')],
        'Last 24 hours': [moment().subtract(1, 'day'), moment()],
        'Yesterday': [moment().subtract(1, 'day').startOf('day'), moment().startOf('day')],
        'Last Week': [moment().subtract(1, 'week').startOf('day'), moment().startOf('day')]
      };

      return {
        uiDateFormat: UI_DATE_FORMAT,
        activeRangeStart: moment(startDate).format(DISPLAY_DATE_FORMAT),
        activeRangeEnd: moment(endDate).format(DISPLAY_DATE_FORMAT),
        timeRangeOptions: setUpTimeRangeOptions(TIME_RANGE_OPTIONS, duration),
        timePickerIncrement: TIME_PICKER_INCREMENT,
        predefinedRanges
      };
    }
  ),

  _getAnomalies: task (function * (alertYaml) {//TODO: need to add to anomaly util - LH
    const {
      analysisRange,
      anomaliesRange,
      notifications,
      showRules,
      alertId,
      granularity,
      stateOfAnomaliesAndTimeSeries
    } = this.getProperties('analysisRange', 'anomaliesRange', 'notifications',
      'showRules', 'alertId', 'granularity', 'stateOfAnomaliesAndTimeSeries');
    //detection alert fetch
    const start = analysisRange[0];
    const end = analysisRange[1];
    const startAnomalies = anomaliesRange[0];
    const endAnomalies = anomaliesRange[1];
    let anomalies;
    let uniqueTimeSeries;
    let applicationAnomalies;
    let metricUrnList;
    let firstDimension;
    try {
      if(showRules){
        applicationAnomalies = ((granularity || '').includes('DAYS')) ? yield getBounds(alertId, startAnomalies, endAnomalies) : yield getYamlPreviewAnomalies(alertYaml, startAnomalies, endAnomalies, alertId);
        if (applicationAnomalies && applicationAnomalies.diagnostics && applicationAnomalies.diagnostics['0']) {
          metricUrnList = Object.keys(applicationAnomalies.diagnostics['0']);
          set(this, 'metricUrnList', metricUrnList);
          firstDimension = toMetricLabel(extractTail(decodeURIComponent(metricUrnList[0])));
          firstDimension = firstDimension ? firstDimension : 'All Dimensions';
          set(this, 'selectedDimension', firstDimension);
          if (applicationAnomalies.predictions && Array.isArray(applicationAnomalies.predictions) && (typeof applicationAnomalies.predictions[0] === 'object')){
            const detectorName = applicationAnomalies.predictions[0].detectorName;
            const selectedRule = {
              detectorName,
              name: detectorName.split(':')[0]
            };
            set(this, 'selectedRule', selectedRule);
          }
          set(this, 'metricUrn', metricUrnList[0]);
        }
        // case 1 is Alert Overview, case 4 is anomaliesOld for Edit Alert Preview (should be real anomalies with ids)
        anomalies = ((stateOfAnomaliesAndTimeSeries === 1 && !this.get('isPreviewMode')) || stateOfAnomaliesAndTimeSeries === 4) ? yield getAnomaliesByAlertId(alertId, start, end) : applicationAnomalies.anomalies;
        uniqueTimeSeries = applicationAnomalies.predictions;
      } else {
        applicationAnomalies = yield getAnomaliesByAlertId(alertId, start, end);
        const metricUrnObj = {};
        if (applicationAnomalies) {
          applicationAnomalies.forEach(anomaly => {
            metricUrnObj[anomaly.metricUrn] = 1;
          });
          metricUrnList = Object.keys(metricUrnObj);
          if (metricUrnList.length > 0) {
            firstDimension = toMetricLabel(extractTail(decodeURIComponent(metricUrnList[0])));
            firstDimension = firstDimension ? firstDimension : 'All Dimensions';
            this.setProperties({
              metricUrnList,
              selectedDimension: firstDimension,
              metricUrn: metricUrnList[0]
            });
          }
        }
        anomalies = applicationAnomalies;
      }
    } catch (error) {
      notifications.error(`_getAnomalies failed: ${error}`, 'Error', toastOptions);
      this.set('getAnomaliesError', true);
    }

    return {
      anomalies,
      uniqueTimeSeries
    };
  }).drop(),

  init() {
    this._super(...arguments);
    const {
      granularity,
      isPreviewMode,
      dimensionExploration
    } = this.getProperties('granularity', 'isPreviewMode', 'dimensionExploration');
    let timeWindowSize = get(this, 'timeWindowSize');
    timeWindowSize = timeWindowSize ? timeWindowSize : 172800000; // 48 hours in milliseconds
    if (!isPreviewMode) {
      this.setProperties({
        analysisRange: [moment().subtract(timeWindowSize, 'milliseconds').startOf('day').valueOf(), moment().add(1, 'day').startOf('day').valueOf()],
        duration: (timeWindowSize === 172800000) ? '48h' : 'custom',
        selectedDimension: 'Choose a dimension',
        // For now, we will only show predicted and bounds on daily metrics with no dimensions, for the Alert Overview page
        selectedBaseline: ((granularity || '').includes('DAYS') && !dimensionExploration) ? 'predicted' : 'wo1w'
      });
      this._fetchAnomalies();
    } else {
      this.setProperties({
        analysisRange: [moment().subtract(timeWindowSize, 'milliseconds').startOf('day').valueOf(), moment().add(1, 'day').startOf('day').valueOf()],
        duration: 'custom',
        selectedBaseline: 'predicted'
      });
    }
  },

  _formattedModifiedBy(feedback) {
    let result;
    if (feedback && typeof feedback === 'object') {
      if (feedback.updatedBy && feedback.updatedBy !== 'no-auth-user') {
        result = feedback.updatedBy.split('@')[0];
      } else {
        result = '--';
      }
    }
    return result;
  },

  _formattedRule(properties) {
    let result;
    if (properties && typeof properties === 'object') {
      if (properties.detectorComponentName) {
        result = properties.detectorComponentName.split(':')[0];
      } else {
        result = '--';
      }
    }
    return result;
  },

  _formatAnomaly(anomaly) {
    return `${moment(anomaly.startTime).format(TABLE_DATE_FORMAT)}`;
  },

  _filterAnomalies(rows) {
    return rows.filter(row => (row.startTime && row.endTime && !row.child));
  },

  _fetchTimeseries() {
    const {
      metricUrn,
      analysisRange,
      selectedBaseline,
      showRules,
      selectedRule,
      uniqueTimeSeries
    } = this.getProperties('metricUrn', 'analysisRange', 'selectedBaseline', 'showRules', 'selectedRule', 'uniqueTimeSeries');
    const timeZone = 'America/Los_Angeles';

    this.setProperties({
      errorTimeseries: null,
      isLoadingTimeSeries: true
    });

    if (showRules) {
      const seriesSet = uniqueTimeSeries.find(series => {
        if (series.detectorName === selectedRule.detectorName && series.metricUrn === metricUrn) {
          return series;
        }
      });
      if (seriesSet) {
        if (selectedBaseline === 'predicted') {
          this.setProperties({
            timeseries: seriesSet.predictedTimeSeries,
            baseline: seriesSet.predictedTimeSeries,
            isLoadingTimeSeries: false
          });
        } else {
          const urlBaseline = `/rootcause/metric/timeseries?urn=${metricUrn}&start=${analysisRange[0]}&end=${analysisRange[1]}&offset=${selectedBaseline}&timezone=${timeZone}`;
          fetch(urlBaseline)
            .then(checkStatus)
            .then(res => {
              this.setProperties({
                timeseries: seriesSet.predictedTimeSeries,
                baseline: res,
                isLoadingTimeSeries: false
              });
            });
        }
      }
    } else {
      const urlCurrent = `/rootcause/metric/timeseries?urn=${metricUrn}&start=${analysisRange[0]}&end=${analysisRange[1]}&offset=current&timezone=${timeZone}`;
      fetch(urlCurrent)
        .then(checkStatus)
        .then(res => {
          this.setProperties({
            timeseries: res,
            isLoadingTimeSeries: false
          });
        });
      const urlBaseline = `/rootcause/metric/timeseries?urn=${metricUrn}&start=${analysisRange[0]}&end=${analysisRange[1]}&offset=${selectedBaseline}&timezone=${timeZone}`;
      fetch(urlBaseline)
        .then(checkStatus)
        .then(res => set(this, 'baseline', res));
    }
    set(this, 'errorBaseline', null);
  },

  _fetchAnomalies() {
    this.setProperties({
      getAnomaliesError: false,
      isLoading: true
    });

    try {
      // in Edit Alert Preview, we want the original yaml used for comparisons
      const content = (get(this, 'isEditMode') && _.isEmpty(get(this, 'anomaliesOld'))) ? get(this, 'originalYaml') : get(this, 'alertYaml');
      return this.get('_getAnomalies').perform(content)
        .then(results => this._setAnomaliesAndTimeSeries(results))
        .then(() => {
          if (get(this, 'metricUrn')) {
            this._fetchTimeseries();
          } else {
            throw new Error('Unable to get MetricUrn from response');
          }
        })
        .catch(error => {
          set(this, 'isLoading', false);
          this.get('notifications').error(error, 'Error', toastOptions);
          set(this, 'getAnomaliesError', true);
        });
    } catch (error) {
      set(this, 'isLoading', false);
      this.get('notifications').error(error, 'Error', toastOptions);
      set(this, 'getAnomaliesError', true);
    }
  },

  /**
   * Set retrieved anomalies/timeSeries based on current state
   * @method _setAnomaliesAndTimeSeries
   * @param {Object} results - The results object from _getAnomalies method
   * @return {undefined}
   */
  _setAnomaliesAndTimeSeries(results) {
    const state = get(this, 'stateOfAnomaliesAndTimeSeries');
    switch (state) {
      case 1:
        this.setProperties({
          anomaliesOld: results.anomalies,
          uniqueTimeSeries: results.uniqueTimeSeries,
          isLoading: false
        });
        break;
      case 2:
        this.setProperties({
          anomaliesNew: results.anomalies,
          uniqueTimeSeries: results.uniqueTimeSeries,
          isLoading: false
        });
        break;
      case 3:
        set(this, 'anomaliesOld', this.get('anomaliesNew'));
        this.setProperties({
          anomaliesNew: results.anomalies,
          uniqueTimeSeries: results.uniqueTimeSeries,
          isLoading: false
        });
        break;
      case 4:
        this.setProperties({
          anomaliesOld: results.anomalies,
          anomaliesNew: []
        });
        this._fetchAnomalies();
        break;
      // don't set props if there was an error with _getAnomalies
      default:
        break;
    }
  },

  /**
   * Send a POST request to the report anomaly API (2-step process)
   * http://go/te-ss-alert-flow-api
   * @method reportAnomaly
   * @param {String} id - The alert id
   * @param {Object} data - The input values from 'report new anomaly' modal
   * @return {Promise}
   */
  _reportAnomaly(id, metricUrn, data) {
    const reportUrl = `/detection/report-anomaly/${id}?metricUrn=${metricUrn}`;
    const requiredProps = ['startTime', 'endTime', 'feedbackType'];
    let missingData = false;
    requiredProps.forEach(prop => {
      if (!data[prop]) {
        missingData = true;
      }
    });
    let queryStringUrl = reportUrl;

    if (missingData) {
      return Promise.reject(new Error('missing data'));
    } else {
      Object.entries(data).forEach(([key, value]) => {
        queryStringUrl += `&${encodeURIComponent(key)}=${encodeURIComponent(value)}`;
      });
      // Step 1: Report the anomaly
      return fetch(queryStringUrl, postProps('')).then((res) => checkStatus(res, 'post'));
    }
  },

  /**
   * Modal opener for "report missing anomaly".
   * @method _triggerOpenReportModal
   * @return {undefined}
   */
  _triggerOpenReportModal() {
    this.setProperties({
      isReportSuccess: false,
      isReportFailure: false,
      openReportModal: true
    });
    // We need the C3/D3 graph to render after its containing parent elements are rendered
    // in order to avoid strange overflow effects.
    later(() => {
      this.set('renderModalContent', true);
    });
  },

  actions: {
    /**
     * Handle missing anomaly modal cancel
     */
    onCancel() {
      this.setProperties({
        isReportSuccess: false,
        isReportFailure: false,
        openReportModal: false,
        renderModalContent: false
      });
    },

    /**
     * Open modal for missing anomalies
     */
    onClickReportAnomaly() {
      this._triggerOpenReportModal();
    },

    /**
     * Received bubbled-up action from modal
     * @param {Object} all input field values
     */
    onInputMissingAnomaly(inputObj) {
      this.set('missingAnomalyProps', inputObj);
    },

    /**
     * Handle submission of missing anomaly form from alert-report-modal
     */
    onSave() {
      const { alertId, missingAnomalyProps, metricUrn } = this.getProperties('alertId', 'missingAnomalyProps', 'metricUrn');
      this._reportAnomaly(alertId, metricUrn, missingAnomalyProps)
        .then(() => {
          const rangeFormat = 'YYYY-MM-DD HH:mm';
          const startStr = moment(missingAnomalyProps.startTime).format(rangeFormat);
          const endStr = moment(missingAnomalyProps.endTime).format(rangeFormat);
          this.setProperties({
            isReportSuccess: true,
            isReportFailure: false,
            openReportModal: false,
            reportedRange: `${startStr} - ${endStr}`
          });
        })
        // If failure, leave modal open and report
        .catch(() => {
          this.setProperties({
            missingAnomalyProps: {},
            isReportFailure: true,
            isReportSuccess: false
          });
        });
    },

    onSelectRule(selected) {
      set(this, 'selectedRule', selected);
      this._fetchTimeseries();
    },

    onSelectDimension(selected) {
      const metricUrnList = get(this, 'metricUrnList');
      const newMetricUrn = metricUrnList.find(urn => {
        const dimensionUrn = toMetricLabel(extractTail(decodeURIComponent(urn)));
        if (dimensionUrn === selected) {
          return urn;
          // if there is no tail, this will be called 'All Dimensions' in the UI
        } else if (dimensionUrn === '' && selected === 'All Dimensions') {
          return urn;
        }
      });
      let dimension = toMetricLabel(extractTail(decodeURIComponent(newMetricUrn)));
      dimension = dimension ? dimension : 'All Dimensions';
      this.setProperties({
        metricUrn: newMetricUrn,
        selectedDimension: dimension
      });
      this._fetchTimeseries();
    },

    /**
     * Sets the new custom date range for anomaly coverage
     * @method onRangeSelection
     * @param {Object} rangeOption - the user-selected time range to load
     */
    onRangeSelection(timeRangeOptions) {
      const {
        start,
        end,
        value: duration
      } = timeRangeOptions;

      const startDate = moment(start).valueOf();
      const endDate = moment(end).valueOf();
      //Update the time range option selected
      set(this, 'analysisRange', [startDate, endDate]);
      set(this, 'duration', duration);
      // This makes sure we don't fetch if the preview is collapsed
      if(get(this, 'showDetails') && get(this, 'dataIsCurrent')){
        // With a new date range, we should reset the state of time series and anomalies for comparison
        if (get(this, 'isPreviewMode')) {
          this.setProperties({
            anomaliesOld: [],
            anomaliesNew: []
          });
        }
        this._fetchAnomalies();
      }
    },

    /**
    * triggered by preview button
    */
    getPreview() {
      this.setProperties({
        showDetails: true,
        dataIsCurrent: true
      });
      this._fetchAnomalies();
    },

    /**
     * Handle display of selected baseline options
     * @param {Object} clicked - the baseline selection
     */
    onBaselineOptionClick(clicked) {
      const baselineOptions = get(this, 'baselineOptions');
      const isValidSelection = !clicked.isActive;
      let newOptions = baselineOptions.map((val) => {
        return { name: val.name, isActive: false };
      });

      // Set active option
      newOptions.find((val) => val.name === clicked.name).isActive = true;
      this.set('baselineOptions', newOptions);

      if(isValidSelection) {
        set(this, 'selectedBaseline', clicked.name);
        this._fetchTimeseries();
      }
    }
  }
});