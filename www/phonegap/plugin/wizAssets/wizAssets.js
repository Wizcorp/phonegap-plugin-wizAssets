/* Download and show, PhoneGap Example
 *
 * @author Ally Ogilvie
 * @copyright Wizcorp Inc. [ Incorporated Wizards ] 2014
 * @file - wizAssets.js
 * @about - JavaScript download and update asset example for PhoneGap
 *
 *
 */
var exec = require("cordova/exec");
var wizAssets = {
    initialize: function (s, f) {
        return exec(s, function (error) {
            return f(WizAssetsError.generate(error));
        }, "WizAssetsPlugin", "initialize", []);
    },
    downloadFile: function (url, filePath, s, f) {
        return window.setTimeout(function () {
            return exec(s, function (error) {
                return f(WizAssetsError.generate(error));
            }, "WizAssetsPlugin", "downloadFile", [url, filePath]);
        }, 0);
    },
    deleteFile: function (uri, s, f) {
        return exec(s, function (error) {
            return f(WizAssetsError.generate(error));
        }, "WizAssetsPlugin", "deleteFile", [uri]);
    },
    deleteFiles: function (uris, s, f) {
        return exec(s, function (error) {
            return f(WizAssetsError.generate(error));
        }, "WizAssetsPlugin", "deleteFiles", uris);
    },
    getFileURIs: function (s, f) {
        return exec(s, function (error) {
            return f(WizAssetsError.generate(error));
        }, "WizAssetsPlugin", "getFileURIs", []);
    },
    getFileURI: function (uri, s, f) {
        return exec(s, function (error) {
            return f(WizAssetsError.generate(error));
        }, "WizAssetsPlugin", "getFileURI", [uri]);
    }
};

module.exports = wizAssets;