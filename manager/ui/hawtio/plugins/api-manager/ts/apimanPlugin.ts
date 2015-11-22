/// <reference path='../../includes.ts'/>
/// <reference path='apimanGlobals.ts'/>
module Apiman {

    export var _module = angular.module(Apiman.pluginName, [
        'angular-clipboard',
        'ngRoute',
        'ui.bootstrap',
        'ui.sortable',
        'xeditable',
        'ngFileUpload',
        'ngAnimate',

        'ApimanServices',
        'ApimanFilters',
        'ApimanLogger',
        'ApimanConfiguration',
        'ApimanTranslation',
        'ApimanPageLifecycle',
        'ApimanCurrentUser',
        'ApimanDialogs'
    ]);

    _module.config([
        '$locationProvider',
        '$routeProvider', ($locationProvider,
                           $routeProvider) => {
            var path = 'plugins/api-manager/html/';
            var prefix = '/api-manager';

            // Define Routes

            $routeProvider
                .when(prefix + '/', {
                    templateUrl: path + 'dash.html'
                })
                .when(prefix + '/about', {
                    templateUrl: path + 'about.html'
                })
                .when(prefix + '/profile', {
                    templateUrl: path + 'profile.html'
                })
                .when(prefix + '/admin/gateways', {
                    templateUrl: path + 'admin/admin-gateways.html'
                })
                .when(prefix + '/admin/plugins', {
                    templateUrl: path + 'admin/admin-plugins.html'
                })
                .when(prefix + '/admin/policyDefs', {
                    templateUrl: path + 'admin/admin-policyDefs.html'
                })
                .when(prefix + '/admin/roles', {
                    templateUrl: path + 'admin/admin-roles.html'
                })
                .when(prefix + '/admin/export', {
                    templateUrl: path + 'admin/admin-export.html'
                })
                .when(prefix + '/admin/gateways/:gateway', {
                    templateUrl: path + 'forms/edit-gateway.html'
                })
                .when(prefix + '/admin/plugins/:plugin', {
                    templateUrl: path + 'forms/edit-plugin.html'
                })
                .when(prefix + '/admin/policyDefs/:policyDef', {
                    templateUrl: path + 'forms/edit-policyDef.html'
                })
                .when(prefix + '/admin/roles/:role', {
                    templateUrl: path + 'forms/edit-role.html'
                })
                .when(prefix + '/orgs/:org/:type/:id/:ver/policies/:policy', {
                    templateUrl: path + 'forms/edit-policy.html'
                })
                .when(prefix + '/orgs/:org/:type/:id/:ver/new-policy', {
                    templateUrl: path + 'forms/new-policy.html'
                })
                .when(prefix + '/orgs/:org/apps/:app', {
                    templateUrl: path + 'app/app.html'
                })
                .when(prefix + '/orgs/:org/apps/:app/:version', {
                    templateUrl: path + 'app/app-overview.html'
                })
                .when(prefix + '/orgs/:org/apps/:app/:version/contracts', {
                    templateUrl: path + 'app/app-contracts.html'
                })
                .when(prefix + '/orgs/:org/apps/:app/:version/apis', {
                    templateUrl: path + 'app/app-apis.html'
                })
                .when(prefix + '/orgs/:org/apps/:app/:version/metrics', {
                    templateUrl: path + 'app/app-metrics.html'
                })
                .when(prefix + '/orgs/:org/apps/:app/:version/policies', {
                    templateUrl: path + 'app/app-policies.html'
                })
                .when(prefix + '/orgs/:org/apps/:app/:version/activity', {
                    templateUrl: path + 'app/app-activity.html'
                })
                .when(prefix + '/orgs/:org/apps/:app/:version/new-version', {
                    templateUrl: path + 'forms/new-appversion.html'
                })
                .when(prefix + '/orgs/:org/plans/:plan', {
                    templateUrl: path + 'plan/plan.html'
                })
                .when(prefix + '/orgs/:org/plans/:plan/:version', {
                    templateUrl: path + 'plan/plan-overview.html'
                })
                .when(prefix + '/orgs/:org/plans/:plan/:version/policies', {
                    templateUrl: path + 'plan/plan-policies.html'
                })
                .when(prefix + '/orgs/:org/plans/:plan/:version/activity', {
                    templateUrl: path + 'plan/plan-activity.html'
                })
                .when(prefix + '/orgs/:org/plans/:plan/:version/new-version', {
                    templateUrl: path + 'forms/new-planversion.html'
                })
                .when(prefix + '/orgs/:org/services/:service', {
                    templateUrl: path + 'service/service.html'
                })
                .when(prefix + '/orgs/:org/services/:service/:version', {
                    templateUrl: path + 'service/service-overview.html'
                })
                .when(prefix + '/orgs/:org/services/:service/:version/impl', {
                    templateUrl: path + 'service/service-impl.html'
                })
                .when(prefix + '/orgs/:org/services/:service/:version/def', {
                    templateUrl: path + 'service/service-def.html'
                })
                .when(prefix + '/orgs/:org/services/:service/:version/plans', {
                    templateUrl: path + 'service/service-plans.html'
                })
                .when(prefix + '/orgs/:org/services/:service/:version/policies', {
                    templateUrl: path + 'service/service-policies.html'
                })
                .when(prefix + '/orgs/:org/services/:service/:version/endpoint', {
                    templateUrl: path + 'service/service-endpoint.html'
                })
                .when(prefix + '/orgs/:org/services/:service/:version/contracts', {
                    templateUrl: path + 'service/service-contracts.html'
                })
                .when(prefix + '/orgs/:org/services/:service/:version/metrics', {
                    templateUrl: path + 'service/service-metrics.html'
                })
                .when(prefix + '/orgs/:org/services/:service/:version/activity', {
                    templateUrl: path + 'service/service-activity.html'
                })
                .when(prefix + '/orgs/:org/services/:service/:version/new-version', {
                    templateUrl: path + 'forms/new-serviceversion.html'
                })
                .when(prefix + '/orgs/:org/import/services', {
                    templateUrl: path + 'service/import-services.html'
                })
                .when(prefix + '/browse/orgs', {
                    templateUrl: path + 'consumer/consumer-orgs.html'
                })
                .when(prefix + '/browse/services', {
                    templateUrl: path + 'consumer/consumer-services.html'
                })
                .when(prefix + '/browse/orgs/:org', {
                    templateUrl: path + 'consumer/consumer-org.html'
                })
                .when(prefix + '/browse/orgs/:org/:service', {
                    templateUrl: path + 'consumer/consumer-service-redirect.html'
                })
                .when(prefix + '/browse/orgs/:org/:service/:version', {
                    templateUrl: path + 'consumer/consumer-service.html'
                })
                .when(prefix + '/browse/orgs/:org/:service/:version/def', {
                    templateUrl: path + 'consumer/consumer-service-def.html'
                })
                .when(prefix + '/new-app', {
                    templateUrl: path + 'forms/new-app.html'
                })
                .when(prefix + '/new-contract', {
                    templateUrl: path + 'forms/new-contract.html'
                })
                .when(prefix + '/new-gateway', {
                    templateUrl: path + 'forms/new-gateway.html'
                })
                .when(prefix + '/new-org', {
                    templateUrl: path + 'forms/new-org.html'
                })
                .when(prefix + '/new-plan', {
                    templateUrl: path + 'forms/new-plan.html'
                })
                .when(prefix + '/new-plugin', {
                    templateUrl: path + 'forms/new-plugin.html'
                })
                .when(prefix + '/new-role', {
                    templateUrl: path + 'forms/new-role.html'
                })
                .when(prefix + '/new-service', {
                    templateUrl: path + 'forms/new-service.html'
                })
                .when(prefix + '/import-policyDefs', {
                    templateUrl: path + 'forms/import-policyDefs.html'
                })
                .when(prefix + '/orgs/:org', {
                    templateUrl: path + 'org/org.html'
                })
                .when(prefix + '/orgs/:org/plans', {
                    templateUrl: path + 'org/org-plans.html'
                })
                .when(prefix + '/orgs/:org/services', {
                    templateUrl: path + 'org/org-services.html'
                })
                .when(prefix + '/orgs/:org/apps', {
                    templateUrl: path + 'org/org-apps.html'
                })
                .when(prefix + '/orgs/:org/members', {
                    templateUrl: path + 'org/org-members.html'
                })
                .when(prefix + '/orgs/:org/manage-members', {
                    templateUrl: path + 'org/org-manage-members.html'
                })
                .when(prefix + '/orgs/:org/activity', {
                    templateUrl: path + 'org/org-activity.html'
                })
                .when(prefix + '/orgs/:org/new-member', {
                    templateUrl: path + 'org/org-new-member.html'
                })
                .when(prefix + '/users/:user', {
                    templateUrl: path + 'user/user.html'
                })
                .when(prefix + '/users/:user/activity', {
                    templateUrl: path + 'user/user-activity.html'
                })
                .when(prefix + '/users/:user/apps', {
                    templateUrl: path + 'user/user-apps.html'
                })
                .when(prefix + '/users/:user/orgs', {
                    templateUrl: path + 'user/user-orgs.html'
                })
                .when(prefix + '/users/:user/services', {
                    templateUrl: path + 'user/user-services.html'
                })
                .when(prefix + '/errors/invalid_server', {
                    templateUrl: path + 'errors/invalid_server.html'
                })
                .when(prefix + '/errors/400', {
                    templateUrl: path + 'errors/400.html'
                })
                .when(prefix + '/errors/403', {
                    templateUrl: path + 'errors/403.html'
                })
                .when(prefix + '/errors/404', {
                    templateUrl: path + 'errors/404.html'
                })
                .when(prefix + '/errors/409', {
                    templateUrl: path + 'errors/409.html'
                })
                .when(prefix + '/errors/500', {
                    templateUrl: path + 'errors/500.html'
                })
                .otherwise({redirectTo: prefix + '/'});

            $locationProvider.html5Mode(true);
        }]);

    _module.factory('authInterceptor',
        ['$q', '$timeout', 'Configuration', 'Logger',
            ($q, $timeout, Configuration, Logger) => {
                var refreshBearerToken = function () {
                    Logger.info('Refreshing bearer token now.');
                    // Note: we need to use jquery directly for this call, otherwise we will have
                    // a circular dependency in angular.
                    $.get('rest/tokenRefresh', function (reply) {
                        Logger.info('Bearer token successfully refreshed: {0}', reply);
                        Configuration.api.auth.bearerToken.token = reply.token;
                        var refreshPeriod = reply.refreshPeriod;
                        if (!refreshPeriod || refreshPeriod < 1) {
                            Logger.info('Refresh period was invalid! (using 60s)');
                            refreshPeriod = 60;
                        }
                        $timeout(refreshBearerToken, refreshPeriod * 1000);
                    }).fail(function (error) {
                        Logger.error('Failed to refresh bearer token: {0}', error);
                    });
                };
                if (Configuration.api.auth.type == 'bearerToken') {
                    var refreshPeriod = Configuration.api.auth.bearerToken.refreshPeriod;
                    $timeout(refreshBearerToken, refreshPeriod * 1000);
                }
                var requestInterceptor = {
                    request: function (config) {
                        var authHeader = Configuration.getAuthorizationHeader();
                        if (authHeader) {
                            config.headers.Authorization = authHeader;
                        }
                        return config;
                    }
                };
                return requestInterceptor;
            }]);


    _module.config(['$httpProvider', function ($httpProvider) {
        $httpProvider.interceptors.push('authInterceptor');
    }]);

    _module.run([
        '$rootScope',
        'SystemSvcs',
        'Configuration',
        '$location', ($rootScope,
                      SystemSvcs,
                      Configuration,
                      $location) => {

            $rootScope.isDirty = false;

            $rootScope.$on('$locationChangeStart', function (event, newUrl, oldUrl) {
                if ($rootScope.isDirty) {
                    if (confirm('You have unsaved changes. Are you sure you would like to navigate away from this page? You will lose these changes.') != true) {
                        event.preventDefault();
                    }
                }
            });

            if (Configuration.api
                && Configuration.api.auth
                && Configuration.api.auth.type == 'bearerTokenFromHash') {
                var bearerToken = null;
                var backTo = null;
                var tokenKey = 'Apiman.BearerToken';
                var backToKey = 'Apiman.BackToConsole';

                var hash = $location.hash();

                if (hash) {
                    var settings = JSON.parse(hash);
                    localStorage[tokenKey] = settings.token;
                    localStorage[backToKey] = settings.backTo;
                    $location.hash(null);
                    bearerToken = settings.token;
                    backTo = settings.backTo;
                    console.log('*** Bearer token from hash: ' + bearerToken);
                } else {
                    try {
                        bearerToken = localStorage[tokenKey];
                        console.log('*** Bearer token from local storage: ' + bearerToken);
                    } catch (e) {
                        console.log('*** Bearer token from local storage was invalid!');
                        localStorage.removeItem(tokenKey);
                        bearerToken = null;
                    }
                    try {
                        backTo = localStorage[backToKey];
                        console.log('*** Back-to-console link: ' + backTo);
                    } catch (e) {
                        console.log('*** Back-to-console link from local storage was invalid!');
                        localStorage.removeItem(backToKey);
                        backTo = null;
                    }
                }

                if (bearerToken) {
                    Configuration.api.auth.bearerToken = {
                        token: bearerToken
                    };
                }

                if (backTo) {
                    Configuration.ui.backToConsole = backTo;
                }
            }

            $rootScope.pluginName = Apiman.pluginName;
        }]);

    // Load the configuration jsonp script
    $.getScript('apiman/config.js')
        .done((script, textStatus) => {
            log.info('Loaded the config.js config!');
        })
        .fail((response) => {
            log.debug('Error fetching configuration: ', response);
        })
        .always(() => {
            // Load the i18n jsonp script
            $.getScript('apiman/translations.js').done((script, textStatus) => {
                log.info('Loaded the translations.js bundle!');

                angular.element(document).ready(function () {
                    angular.bootstrap(document, ['api-manager']);
                });
            }).fail((response) => {
                log.debug('Error fetching translations: ', response);
            });
        });
}
