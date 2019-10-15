import Component from '@ember/component';
import {
  computed,
  setProperties,
  getProperties
} from '@ember/object';
import { checkStatus } from 'thirdeye-frontend/utils/utils';
import fetch from 'fetch';
import {
  makeTime,
  dateFormatFull
} from 'thirdeye-frontend/utils/rca-utils';
import _ from 'lodash';

export default Component.extend({
  //
  // external properties
  //
  selectedUrn: null,

  anomalyRange: null,

  entities: null,

  //
  // internal properties
  //

  /**
   * Max available data range for the selected metric.
   * @type {Int}
   */
  maxTime: null,

  didReceiveAttrs() {
    const { selectedUrn, anomalyRange } = getProperties(this, 'selectedUrn', 'anomalyRange');

    // if no metric selected, reset to null
    if (!selectedUrn || !anomalyRange || !selectedUrn.startsWith('thirdeye:metric:')) {
      this.setProperties({ maxTime: null });
      return;
    }

    fetch(`/rootcause/raw?framework=identity&urns=${selectedUrn}`)
      .then(checkStatus)
      .then(res => {
        if (_.isEmpty(res[0].attributes.maxTime)) {
          setProperties(this, { maxTime: null });
        } else {
          setProperties(this, { maxTime: parseInt(res[0].attributes.maxTime[0], 10) });
        }
      });
  },

  /**
   * True, if the analysis period exceeds the max available data range
   * @type {boolean}
   */
  hasMaxTimeWarning: computed('anomalyRange', 'maxTime', function () {
    const { anomalyRange, maxTime } = getProperties(this, 'anomalyRange', 'maxTime');

    if (!anomalyRange || !maxTime) { return false; }

    return anomalyRange[1] > maxTime;
  }),

  /**
   * Formatted max available data range
   * @type {string}
   */
  maxTimeFormatted: computed('maxTime', function () {
    const { maxTime } = getProperties(this, 'maxTime');

    if (!maxTime) { return; }

    return makeTime(maxTime).format(dateFormatFull);
  })

});
