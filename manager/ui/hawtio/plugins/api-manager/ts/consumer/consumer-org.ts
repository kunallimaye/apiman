/// <reference path="../apimanPlugin.ts"/>
/// <reference path="../services.ts"/>
module Apiman {

    export var ConsumerOrgController = _module.controller("Apiman.ConsumerOrgController",
        ['$q', '$scope', '$location', 'OrgSvcs', 'PageLifecycle', 'CurrentUser', '$routeParams',
        ($q, $scope, $location, OrgSvcs, PageLifecycle, CurrentUser, $routeParams) => {

            $scope.filterServices = function(value) {
                if (!value) {
                    $scope.filteredServices = $scope.services;
                } else {
                    var filtered = [];
                    angular.forEach($scope.services, function(service) {
                        if (service.name.toLowerCase().indexOf(value.toLowerCase()) > -1) {
                            filtered.push(service);
                        }
                    });
                    $scope.filteredServices = filtered;
                }
            };
            
            var pageData = {
                org: $q(function(resolve, reject) {
                    OrgSvcs.get({ organizationId: $routeParams.org, entityType: '' }, resolve, reject);
                }),
                members: $q(function(resolve, reject) {
                    OrgSvcs.query({ organizationId: $routeParams.org, entityType: 'members' }, resolve, reject);
                }),
                services: $q(function(resolve, reject) {
                    OrgSvcs.query({ organizationId: $routeParams.org, entityType: 'services' }, resolve, reject);
                })
            };
            
            PageLifecycle.loadPage('ConsumerOrg', undefined, pageData, $scope, function() {
                $scope.org.isMember = CurrentUser.isMember($scope.org.id);
                $scope.filteredServices = $scope.services;
                PageLifecycle.setPageTitle('consumer-org', [ $scope.org.name ]);
            });
        }])

}
