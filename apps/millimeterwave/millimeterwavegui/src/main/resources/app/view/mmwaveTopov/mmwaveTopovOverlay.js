// sample topology overlay - client side
//
// This is the glue that binds our business logic (in mmwaveTopovDemo.js)
// to the overlay framework.

(function () {
    'use strict';

    // injected refs
    var $log, tov, mtds, tts;

    // internal state should be kept in the service module (not here)
        // the viewbox is the same name as the icon, prefixed with an underscore:
        var viewbox = '0 0 110 110';
        // the path data (concatenated so it fits nicely on the screen)
        var mmwaveIcon = 'M57.2,20.1c-12.2,0-22,9.9-22,22s9.9,22,' +
        '22,22s22-9.9,22-22S69.3,20.1,57.2,20.1z M63.4,51.6l-' +
        '6.2-3.3l-6.2,3.3l1.2-6.9l-5-4.9l6.9-1l3.1-6.3l3.1,' +
        '6.3l6.9,1l-5,4.9L63.4,51.6z M93.7,81.6H20.6V70.6h73.' +
        '2V81.6z';





    // our overlay definition
    var overlay = {
        // NOTE: this must match the ID defined in AppUiTopovOverlay
        overlayId: 'mm-wave overlay',
        glyphId: '*mmwaveIcon',
        tooltip: 'MM-wave Topo Overlay',

        // These glyphs get installed using the overlayId as a prefix.
        // e.g. 'star4' is installed as 'meowster-overlay-star4'
        // They can be referenced (from this overlay) as '*star4'
        // That is, the '*' prefix stands in for 'meowster-overlay-'
        glyphs: {
            mmwaveIcon: {
                vb: viewbox,
                d:  mmwaveIcon
            }
        },

        activate: function () {
            $log.debug("MM-wave topology overlay ACTIVATED");
        },
        deactivate: function () {
            $log.debug("MM-wave topology overlay DEACTIVATED");
        },

        //detail panel button definitions
        //Added to device panel, defined in server java part
        buttons: {
            foo: {
                gid: 'chain',
                tt: 'A FOO action',
                cb: function (data) {
                    $log.debug('FOO action invoked with data:', data);
                }
            },
            bar: {
                gid: '*banner',
                tt: 'A BAR action',
                cb: function (data) {
                    $log.debug('BAR action invoked with data:', data);
                }
            }
        },

        // Key bindings for traffic overlay buttons
        // NOTE: fully qual. button ID is derived from overlay-id and key-name
        keyBindings: {
            0: {
                cb: function () { mtds.stopDisplay(); },
                tt: 'Cancel Display Mode',
                gid: 'xMark'
            },
            K: {
                cb: function () { mtds.startDisplay(); },
                tt: 'Start Display Mode',
                gid: '*mmwaveIcon'
            },
            V: {
                cb: function () { tts.showRelatedIntents(); },
                tt: 'Show all related intents',
                gid: 'm_relatedIntents'
            },
            leftArrow: {
                cb: function () { tts.showPrevIntent(); },
                tt: 'Show previous related intent',
                gid: 'm_prev'
            },
            rightArrow: {
                cb: function () { tts.showNextIntent(); },
                tt: 'Show next related intent',
                gid: 'm_next'
            },
            W: {
                cb: function () { tts.showSelectedIntentTraffic(); },
                tt: 'Monitor traffic of selected intent',
                gid: 'm_intentTraffic'
            },


            _keyOrder: [
                'K', '0', 'V', 'leftArrow', 'rightArrow', 'W'
            ]
        },

        hooks: {
            // hook for handling escape key
            // Must return true to consume ESC, false otherwise.
            escape: function () {
                // Must return true to consume ESC, false otherwise.
                return mtds.stopDisplay();
            },

            // hooks for when the selection changes...
            empty: function () {
                selectionCallback('empty');
            },
            single: function (data) {
                selectionCallback('single', data);
            },
            mouseout: function () {
                $log.debug('mouseout');
                mtds.updateDisplay();
            }
        }
    };


    function buttonCallback(x) {
        $log.debug('Toolbar-button callback', x);
    }

    function selectionCallback(x, d) {
        $log.debug('Selection callback', x, d);
    }

    // invoke code to register with the overlay service
    angular.module('ovMmwaveTopov')
        .run(['$log', 'TopoOverlayService', 'mmwaveTopovDemoService','TopoTrafficService',

        function (_$log_, _tov_, _mtds_,_tts_) {
            $log = _$log_;
            tov = _tov_;
            mtds = _mtds_;
            tts = _tts_;
            tov.register(overlay);
        }]);

}());
