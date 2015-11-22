/// <reference path="../../includes.ts"/>
module ApimanServices {

    export var _module = angular.module("ApimanServices", ['ngResource', 'ApimanConfiguration']);
    
    var formatEndpoint = function(endpoint, params) {
        return endpoint.replace(/:(\w+)/g, function(match, key) {
            return params[key] ? params[key] : (':' + key);
        });
    };

    export var ApimanSvcs = _module.factory('ApimanSvcs', ['$resource', 'Configuration',
        function($resource, Configuration) {
            var endpoint = Configuration.api.endpoint + '/:entityType/:secondaryType';
            return $resource(endpoint,
                { entityType: '@entityType', secondaryType: '@secondaryType' }, {
                update: {
                  method: 'PUT' // this method issues a PUT request
                }});
        }]);

    export var UserSvcs = _module.factory('UserSvcs', ['$resource', 'Configuration',
        function($resource, Configuration) {
            var endpoint = Configuration.api.endpoint + '/users/:user/:entityType';
            return $resource(endpoint,
                { user: '@user', entityType: '@entityType' });
        }]);

    export var OrganizationSvcs = _module.factory('OrgSvcs', ['$resource', 'Configuration',
        function($resource, Configuration) {
            var endpoint = Configuration.api.endpoint + '/organizations/:organizationId/:entityType/:entityId/:versionsOrActivity/:version/:policiesOrActivity/:policyId/:policyChain';
            return $resource(endpoint,
                {
                    organizationId: '@organizationId', 
                    entityType: '@entityType', 
                    entityId: '@entityId', 
                    versionsOrActivity: '@versionsOrActivity', 
                    version: '@version', 
                    policiesOrActivity: '@policiesOrActivity', 
                    policyId: '@policyId', 
                    chain: '@policyChain',
                    
                    page: '@page',
                    count: '@count'
                }, {
                update: {
                  method: 'PUT' // update issues a PUT request
                }});
        }]);

    export var CurrentUserSvcs = _module.factory('CurrentUserSvcs', ['$resource', 'Configuration',
        function($resource, Configuration) {
            var endpoint = Configuration.api.endpoint + '/currentuser/:what';
            return $resource(endpoint,
                { entityType: '@what' }, {
                update: {
                  method: 'PUT' // this method issues a PUT request
                }});
        }]);

    export var ActionSvcs = _module.factory('ActionSvcs', ['$resource', 'Configuration',
        function($resource, Configuration) {
            var endpoint = Configuration.api.endpoint + '/actions';
            return $resource(endpoint);
        }]);

    export var AuditSvcs = _module.factory('AuditSvcs', ['$resource', 'Configuration',
        function($resource, Configuration) {
            var endpoint = Configuration.api.endpoint + '/organizations/:organizationId/:entityType/:entityId/activity';
            return $resource(endpoint,
                {
                    organizationId: '@organizationId',
                    entityType: '@entityType',
                    entityId: '@entityId',
                    
                    page: '@page',
                    count: '@count'
                });
        }]);

    export var UserAuditSvcs = _module.factory('UserAuditSvcs', ['$resource', 'Configuration',
        function($resource, Configuration) {
            var endpoint = Configuration.api.endpoint + '/users/:user/activity';
            return $resource(endpoint,
                {
                    user: '@user',
                    
                    page: '@page',
                    count: '@count'
                });
        }]);

    
    export var PluginSvcs = _module.factory('PluginSvcs', ['$resource', 'Configuration',
        function($resource, Configuration) {
            return {
                getPolicyForm: function(pluginId, policyDefId, handler, errorHandler) {
                    var endpoint = Configuration.api.endpoint + '/plugins/:pluginId/policyDefs/:policyDefId/form';
                    $resource(endpoint, { pluginId: '@pluginId', policyDefId: '@policyDefId' }).get(
                        {pluginId: pluginId, policyDefId: policyDefId}, 
                        handler, errorHandler);
                }
            }
        }]);

    export var ServiceDefinitionSvcs = _module.factory('ServiceDefinitionSvcs', ['$resource', '$http', 'Configuration',
        function($resource, $http, Configuration) {
            return {
                getServiceDefinitionUrl: function(orgId, serviceId, version) {
                    var endpoint = formatEndpoint(
                        Configuration.api.endpoint + '/organizations/:organizationId/services/:serviceId/versions/:version/definition',
                        { organizationId: orgId, serviceId: serviceId, version: version });
                    return endpoint;
                },
                getServiceDefinition: function(orgId, serviceId, version, handler, errorHandler) {
                    var endpoint = formatEndpoint(
                        Configuration.api.endpoint + '/organizations/:organizationId/services/:serviceId/versions/:version/definition',
                        { organizationId: orgId, serviceId: serviceId, version: version });
                    $http({
                        method: 'GET', 
                        url: endpoint, 
                        transformResponse: function(value) { return value; }
                    }).success(handler).error(errorHandler);
                },
                updateServiceDefinition: function(orgId, serviceId, version, definition, definitionType, handler, errorHandler) {
                    var ct = 'application/json';
                    if (definitionType == 'SwaggerYAML') {
                        ct = 'application/x-yaml';
                    }
                    var endpoint = formatEndpoint(
                        Configuration.api.endpoint + '/organizations/:organizationId/services/:serviceId/versions/:version/definition',
                        { organizationId: orgId, serviceId: serviceId, version: version });
                    $http({
                        method: 'PUT', 
                        url: endpoint,
                        headers: { 'Content-Type' : ct },
                        data: definition
                    }).success(handler).error(errorHandler);
                }
            }
        }]);

    export var MetricsSvcs = _module.factory('MetricsSvcs', ['$resource', 'Configuration',
        function($resource, Configuration) {
            return {
                getUsage: function(orgId, serviceId, version, interval, from, to, handler, errorHandler) {
                    var endpoint = formatEndpoint(
                        Configuration.api.endpoint + '/organizations/:organizationId/services/:serviceId/versions/:version/metrics/usage',
                        { organizationId: orgId, serviceId: serviceId, version: version });
                    $resource(endpoint, {interval: interval, from: from, to: to}).get({}, handler, errorHandler);
                },
                getUsagePerApp: function(orgId, serviceId, version, from, to, handler, errorHandler) {
                    var endpoint = formatEndpoint(
                        Configuration.api.endpoint + '/organizations/:organizationId/services/:serviceId/versions/:version/metrics/appUsage',
                        { organizationId: orgId, serviceId: serviceId, version: version });
                    $resource(endpoint, {from: from, to: to}).get({}, handler, errorHandler);
                },
                getUsagePerPlan: function(orgId, serviceId, version, from, to, handler, errorHandler) {
                    var endpoint = formatEndpoint(
                        Configuration.api.endpoint + '/organizations/:organizationId/services/:serviceId/versions/:version/metrics/planUsage',
                        { organizationId: orgId, serviceId: serviceId, version: version });
                    $resource(endpoint, {from: from, to: to}).get({}, handler, errorHandler);
                },
                getResponseStats: function(orgId, serviceId, version, interval, from, to, handler, errorHandler) {
                    var endpoint = formatEndpoint(
                        Configuration.api.endpoint + '/organizations/:organizationId/services/:serviceId/versions/:version/metrics/responseStats',
                        { organizationId: orgId, serviceId: serviceId, version: version });
                    $resource(endpoint, {interval: interval, from: from, to: to}).get({}, handler, errorHandler);
                },
                getResponseStatsSummary: function(orgId, serviceId, version, from, to, handler, errorHandler) {
                    var endpoint = formatEndpoint(
                        Configuration.api.endpoint + '/organizations/:organizationId/services/:serviceId/versions/:version/metrics/summaryResponseStats',
                        { organizationId: orgId, serviceId: serviceId, version: version });
                    $resource(endpoint, {from: from, to: to}).get({}, handler, errorHandler);
                },
                getResponseStatsPerApp: function(orgId, serviceId, version, from, to, handler, errorHandler) {
                    var endpoint = formatEndpoint(
                        Configuration.api.endpoint + '/organizations/:organizationId/services/:serviceId/versions/:version/metrics/appResponseStats',
                        { organizationId: orgId, serviceId: serviceId, version: version });
                    $resource(endpoint, {from: from, to: to}).get({}, handler, errorHandler);
                },
                getResponseStatsPerPlan: function(orgId, serviceId, version, from, to, handler, errorHandler) {
                    var endpoint = formatEndpoint(
                        Configuration.api.endpoint + '/organizations/:organizationId/services/:serviceId/versions/:version/metrics/planResponseStats',
                        { organizationId: orgId, serviceId: serviceId, version: version });
                    $resource(endpoint, {from: from, to: to}).get({}, handler, errorHandler);
                },
                getAppUsagePerService: function(orgId, applicationId, version, from, to, handler, errorHandler) {
                    var endpoint = formatEndpoint(
                        Configuration.api.endpoint + '/organizations/:organizationId/applications/:applicationId/versions/:version/metrics/serviceUsage',
                        { organizationId: orgId, applicationId: applicationId, version: version });
                    $resource(endpoint, {from: from, to: to}).get({}, handler, errorHandler);
                },
            }
        }]);

    export var SystemSvcs = _module.factory('SystemSvcs', ['$resource', 'Configuration', 'Logger', 'Upload',
        function($resource, Configuration, Logger, Upload) {
            return {
                getStatus: function(handler, errorHandler) {
                    var endpoint = formatEndpoint(Configuration.api.endpoint + '/system/status', {});
                    $resource(endpoint).get({}, handler, errorHandler);
                },
                exportAsJson: function(handler, errorHandler) {
                    var endpoint = formatEndpoint(Configuration.api.endpoint + '/system/export?download=true', {});
                    $resource(endpoint).get({}, handler, errorHandler);
                },
                importJson: function(file, progressHandler, handler, errorHandler) {
	                var endpoint = formatEndpoint(Configuration.api.endpoint + '/system/import', {});
	                file.upload = Upload.http({
	                    url: endpoint,
	                    method: 'POST',
	                    headers: {
	                      'Content-Type': 'application/json',
	                    },
	                    data: file
	                  });
                  file.upload.then(handler, errorHandler);
                  file.upload.progress(progressHandler);
                }
            }
        }]);

    export var DownloadSvcs = _module.factory('DownloadSvcs', ['$resource', 'Configuration',
        function($resource, Configuration) {
            return {
                getDownloadLink: function(downloadId) {
                    var endpoint = formatEndpoint(Configuration.api.endpoint + '/downloads/:downloadId', {
                    	"downloadId" : downloadId
                    });
                    return endpoint;
                }
            }
        }]);

    export var ApiRegistrySvcs = _module.factory('ApiRegistrySvcs', ['$resource', 'Configuration',
        function($resource, Configuration) {
            return {
                exportApiRegistryAsJson: function(orgId, appId, version, handler, errorHandler) {
                    var endpoint = formatEndpoint(Configuration.api.endpoint + '/organizations/:organizationId/applications/:applicationId/versions/:version/apiregistry/json?download=true', {
                    	"organizationId" : orgId,
                    	"applicationId" : appId,
                    	"version" : version
                    });
                    $resource(endpoint).get({}, handler, errorHandler);
                },
                exportApiRegistryAsXml: function(orgId, appId, version, handler, errorHandler) {
                    var endpoint = formatEndpoint(Configuration.api.endpoint + '/organizations/:organizationId/applications/:applicationId/versions/:version/apiregistry/xml?download=true', {
                    	"organizationId" : orgId,
                    	"applicationId" : appId,
                    	"version" : version
                    });
                    $resource(endpoint).get({}, handler, errorHandler);
                }
            }
        }]);

}
