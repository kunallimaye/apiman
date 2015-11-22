/// <reference path="apimanPlugin.ts"/>
module Apiman {

    export var NavbarController = _module.controller("Apiman.NavbarController",
        ['$scope', 'Logger', 'Configuration', ($scope, Logger, Configuration) => {
            Logger.log("Current user is {0}.", Configuration.user.username);
            $scope.username = Configuration.user.username;
            $scope.logoutUrl = Configuration.apiman.logoutUrl;
            $scope.goBack = function() {
                Logger.info('Returning to parent UI: {0}', Configuration.ui.backToConsole);
                window.location.href = Configuration.ui.backToConsole;
            };
        }]);

}
