/// <reference path="../apimanPlugin.ts"/>
module Apiman {

    export var NewOrgController = _module.controller("Apiman.NewOrgController",
        ['$q', '$location', '$scope', 'OrgSvcs', 'PageLifecycle', 'CurrentUser',
        ($q, $location, $scope, OrgSvcs, PageLifecycle, CurrentUser) => {
            $scope.saveNewOrg = function() {
                $scope.createButton.state = 'in-progress';
                OrgSvcs.save($scope.org, function(reply) {
                    CurrentUser.clear();
                    PageLifecycle.redirectTo('/orgs/{0}/plans', reply.id);
                }, PageLifecycle.handleError);
            };
            
            PageLifecycle.loadPage('NewOrg', undefined, undefined, $scope, function() {
                PageLifecycle.setPageTitle('new-org');
                $scope.$applyAsync(function() {
                    $('#apiman-entityname').focus();
                });
            });
        }]);

}
