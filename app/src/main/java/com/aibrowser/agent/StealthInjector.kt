package com.aibrowser.agent

import android.webkit.WebView

object StealthInjector {

    private val STEALTH_JS = """
        (function() {
            Object.defineProperty(navigator, 'webdriver', {
                get: () => undefined,
                configurable: true
            });
            delete navigator.__proto__.webdriver;

            if (!navigator.languages || navigator.languages.length === 0) {
                Object.defineProperty(navigator, 'languages', {
                    get: () => ['en-US', 'en'],
                    configurable: true
                });
            }

            Object.defineProperty(navigator, 'plugins', {
                get: () => {
                    var plugins = [
                        { name: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer', description: 'Portable Document Format' },
                        { name: 'Chrome PDF Viewer', filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai', description: '' },
                        { name: 'Native Client', filename: 'internal-nacl-plugin', description: '' }
                    ];
                    plugins.length = 3;
                    return plugins;
                },
                configurable: true
            });

            if (navigator.permissions) {
                var originalQuery = navigator.permissions.query;
                navigator.permissions.query = (parameters) => {
                    if (parameters.name === 'notifications') {
                        return Promise.resolve({ state: 'denied', onchange: null });
                    }
                    return originalQuery.call(navigator.permissions, parameters);
                };
            }

            if (!window.chrome) {
                window.chrome = {};
            }
            if (!window.chrome.runtime) {
                window.chrome.runtime = {
                    connect: function() {},
                    sendMessage: function() {}
                };
            }

            if (window.outerWidth === 0) {
                Object.defineProperty(window, 'outerWidth', { get: () => window.innerWidth });
            }
            if (window.outerHeight === 0) {
                Object.defineProperty(window, 'outerHeight', { get: () => window.innerHeight + 85 });
            }

            if (screen.availWidth === 0) {
                Object.defineProperty(screen, 'availWidth', { get: () => screen.width });
            }
            if (screen.availHeight === 0) {
                Object.defineProperty(screen, 'availHeight', { get: () => screen.height });
            }

            if (!navigator.connection) {
                Object.defineProperty(navigator, 'connection', {
                    get: () => ({
                        effectiveType: '4g',
                        rtt: 50,
                        downlink: 10,
                        saveData: false
                    }),
                    configurable: true
                });
            }

            var getParameter = WebGLRenderingContext.prototype.getParameter;
            WebGLRenderingContext.prototype.getParameter = function(parameter) {
                if (parameter === 37445) return 'Intel Inc.';
                if (parameter === 37446) return 'Intel Iris OpenGL Engine';
                return getParameter.call(this, parameter);
            };

            if (!navigator.mediaDevices) {
                Object.defineProperty(navigator, 'mediaDevices', {
                    get: () => ({
                        enumerateDevices: () => Promise.resolve([]),
                        getUserMedia: () => Promise.reject(new DOMException('Not allowed', 'NotAllowedError'))
                    }),
                    configurable: true
                });
            }

            if (!navigator.getBattery) {
                navigator.getBattery = () => Promise.resolve({
                    charging: true, chargingTime: 0, dischargingTime: Infinity, level: 1,
                    addEventListener: () => {}, removeEventListener: () => {}
                });
            }

            if (!navigator.vibrate) {
                navigator.vibrate = () => false;
            }

            var noop = function() {};
            ['debug', 'info', 'warn', 'error', 'log', 'assert', 'trace', 'dir'].forEach(function(method) {
                console[method] = noop;
            });

            if (navigator.hardwareConcurrency === 0) {
                Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 4, configurable: true });
            }
            if (!navigator.deviceMemory) {
                Object.defineProperty(navigator, 'deviceMemory', { get: () => 8, configurable: true });
            }

            var originalNow = performance.now.bind(performance);
            performance.now = function() {
                return Math.round(originalNow() * 1000) / 1000;
            };
            var originalDateNow = Date.now;
            Date.now = function() {
                return Math.round(originalDateNow() / 1000) * 1000;
            };

            if (!window.speechSynthesis) {
                window.speechSynthesis = {
                    speak: function() {}, cancel: function() {}, pause: function() {}, resume: function() {},
                    getVoices: function() { return []; }, addEventListener: function() {}, removeEventListener: function() {}
                };
            }

            if (!window.Notification) {
                window.Notification = { permission: 'denied', requestPermission: () => Promise.resolve('denied') };
            }
        })();
    """.trimIndent()

    fun inject(webView: WebView) {
        webView.evaluateJavascript(STEALTH_JS, null)
    }

    fun getInjectionScript(): String = STEALTH_JS
}
