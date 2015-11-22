/// <reference path="../apimanPlugin.ts"/>
/// <reference path="../services.ts"/>
module Apiman {

    export var OrgRedirectController = _module.controller("Apiman.OrgRedirectController",
        ['$q', '$scope', '$location', 'OrgSvcs', 'PageLifecycle', '$rootScope', 'CurrentUser', '$routeParams',
        ($q, $scope, $location, OrgSvcs, PageLifecycle, $rootScope, CurrentUser, $routeParams) => {
            var params = $routeParams;
            $scope.organizationId = params.org;
            if (!CurrentUser.isMember(params.org)) {
              delete $rootScope['currentUser'];
            }
            PageLifecycle.loadPage('OrgRedirect', 'orgView', undefined, $scope, function() {
                var orgId = $routeParams.org;
                var tab = 'members';
                if (CurrentUser.hasPermission(orgId, 'planEdit')) {
                    tab = 'plans';
                } else if (CurrentUser.hasPermission(orgId, 'svcEdit')) {
                    tab = 'services';
                } else if (CurrentUser.hasPermission(orgId, 'appEdit')) {
                    tab = 'apps';
                }
                PageLifecycle.forwardTo('/orgs/{0}/{1}', orgId, tab);
            });
        }]);

}
