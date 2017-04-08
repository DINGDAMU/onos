/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 Sample Demo module. This contains the "business logic" for the topology
 overlay that we are implementing.
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs, flash, wss, api, linkData, des, linkPanel,panel,ls,gs;

    // constants
    var displayStart = 'mmwaveTopovDisplayStart',
        displayUpdate = 'mmwaveTopovDisplayUpdate',
        displayStop = 'mmwaveTopovDisplayStop';


    // internal state
    var trafficMode = null,
        hoverMode = null;




    // === -------------------------------------
    // ----------------
    //  Helper functions

    // invoked in response to change in selection and/or mouseover/out:
    function requestTrafficForMode(mouse) {
     if (trafficMode === 'intents') {
            if (!mouse || hoverMode === 'intents') {
                requestRelatedIntents();
            }
        } else {
            // do nothing
        }
    }


    function requestRelatedIntents() {
        // generates payload based on current hover-state
        var hov = api.hovered();

        function hoverValid() {
            return hoverMode === 'intents' && hov && (
                hov.class === 'host' ||
                hov.class === 'device' ||
                hov.class === 'link');
        }

        if (api.somethingSelected()) {
            wss.sendEvent('requestRelatedIntents', {
                ids: api.selectOrder(),
                hover: hoverValid() ? hov.id : ''
            });
        }
    }


    // === -------------------------------------------------------------
    //  Traffic requests invoked from keystrokes or toolbar buttons...

    function showRelatedIntents () {
        trafficMode = hoverMode = 'intents';
        requestRelatedIntents();
        flash.flash('Related Paths');
    }

    function showPrevIntent() {
        if (trafficMode === 'intents') {
            hoverMode = null;
            wss.sendEvent('requestPrevRelatedIntent');
            flash.flash('Previous related intent');
        }
    }

    function showNextIntent() {
        if (trafficMode === 'intents') {
            hoverMode = null;
            wss.sendEvent('requestNextRelatedIntent');
            flash.flash('Next related intent');
        }
    }

    function showSelectedIntentTraffic() {
        if (trafficMode === 'intents') {
            hoverMode = null;
            wss.sendEvent('requestSelectedIntentTraffic');
            flash.flash('Traffic on Selected Path');
        }
    }

    // force the system to create a single intent selection
    function selectIntent(data) {
        trafficMode = 'intents';
        hoverMode = null;
        wss.sendEvent('selectIntent', data);
        flash.flash('Selecting Intent ' + data.key);
    }

    // === ---------------------------
    // === Helper functions

    function sendDisplayStart() {
        wss.sendEvent(displayStart);
    }

    function sendDisplayUpdate() {
        wss.sendEvent(displayUpdate);
    }

    function sendDisplayStop() {
        wss.sendEvent(displayStop);
    }



    // === ---------------------------
    // === Main API functions

    function startDisplay() {
        sendDisplayStart();
        flash.flash('Show all the mm-wave links');
    }

    function updateDisplay() {
            sendDisplayUpdate();
    }

    function stopDisplay() {
        sendDisplayStop();
        flash.flash('Canceling display mm-wave links');
    }



    // === ---------------------------
    // === Module Factory Definition

    angular.module('ovMmwaveTopov', [])
        .factory('mmwaveTopovDemoService',
        ['$log', 'FnService', 'FlashService', 'WebSocketService',

        function (_$log_, _fs_, _flash_, _wss_) {
            $log = _$log_;
            fs = _fs_;
            flash = _flash_;
            wss = _wss_;



            return {


                // invoked from toolbar overlay buttons or keystrokes
                showRelatedIntents: showRelatedIntents,
                showPrevIntent: showPrevIntent,
                showNextIntent: showNextIntent,
                showSelectedIntentTraffic: showSelectedIntentTraffic,
                selectIntent: selectIntent,

                // invoked from mouseover/mouseout and selection change
                requestTrafficForMode: requestTrafficForMode,

                startDisplay: startDisplay,
                updateDisplay: updateDisplay,
                stopDisplay: stopDisplay,
            };
        }]);
}());
