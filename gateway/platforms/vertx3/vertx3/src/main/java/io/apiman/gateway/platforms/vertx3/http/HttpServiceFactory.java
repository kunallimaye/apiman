/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.gateway.platforms.vertx3.http;

import io.apiman.common.util.ApimanPathUtils;
import io.apiman.common.util.ApimanPathUtils.ServiceRequestPathInfo;
import io.apiman.gateway.platforms.vertx3.io.VertxServiceRequest;
import io.apiman.gateway.platforms.vertx3.io.VertxServiceResponse;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Construct {@link VertxServiceRequest} and {@link VertxServiceResponse} objects from {@link HttpServerRequest},
 * {@link HttpServerResponse} and {@link HttpClientResponse}
 *
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
@SuppressWarnings("nls")
public class HttpServiceFactory {

    final static Set<String> IGNORESET = new HashSet<>();
    static {
        IGNORESET.add(ApimanPathUtils.X_API_VERSION_HEADER);
        IGNORESET.add("Host");
    }

    public static VertxServiceResponse buildResponse(HttpClientResponse response, Set<String> suppressHeaders) {
        VertxServiceResponse apimanResponse = new VertxServiceResponse();
        apimanResponse.setCode(response.statusCode());
        apimanResponse.setMessage(response.statusMessage());
        multimapToMap(apimanResponse.getHeaders(), response.headers(), suppressHeaders);
        return apimanResponse;
    }

    public static void buildResponse(HttpServerResponse httpServerResponse, VertxServiceResponse amanResponse) {
        httpServerResponse.headers().addAll(amanResponse.getHeaders());
        httpServerResponse.setStatusCode(amanResponse.getCode());
        httpServerResponse.setStatusMessage(amanResponse.getMessage());
    }

    public static VertxServiceRequest buildRequest(HttpServerRequest req, boolean isTransportSecure) {
        VertxServiceRequest apimanRequest = new VertxServiceRequest();
        apimanRequest.setApiKey(parseApiKey(req));
        apimanRequest.setRemoteAddr(req.remoteAddress().host());
        apimanRequest.setType(req.method().toString());
        apimanRequest.setTransportSecure(isTransportSecure);
        multimapToMap(apimanRequest.getHeaders(), req.headers(), IGNORESET);
        multimapToMap(apimanRequest.getQueryParams(), req.params(), Collections.<String>emptySet());
        mungePath(req, apimanRequest);
        return apimanRequest;
    }

    private static void mungePath(HttpServerRequest request, VertxServiceRequest apimanRequest) {
        ServiceRequestPathInfo parsedPath = ApimanPathUtils.parseServiceRequestPath(
                request.getHeader(ApimanPathUtils.X_API_VERSION_HEADER),
                request.getHeader(ApimanPathUtils.ACCEPT_HEADER),
                request.path());

        apimanRequest.setServiceOrgId(parsedPath.orgId);
        apimanRequest.setServiceId(parsedPath.serviceId);
        apimanRequest.setServiceVersion(parsedPath.serviceVersion);
        apimanRequest.setUrl(request.absoluteURI());
        apimanRequest.setDestination(parsedPath.resource);

        if (apimanRequest.getServiceOrgId() == null) {
            throw new IllegalArgumentException("Invalid endpoint provided: " + request.path());
        }
    }

    private static void multimapToMap(Map<String, String> map, MultiMap multimap, Set<String> suppressHeaders) {
        for (Map.Entry<String, String> entry : multimap) {
            if(!suppressHeaders.contains(entry.getKey())) {
                String key = entry.getKey();
                String val = entry.getValue();

                if (key != null && key.equalsIgnoreCase("accept") && val != null
                        && val.startsWith("application/apiman.")) { //$NON-NLS-1$
                    if (val.contains("+json")) { //$NON-NLS-1$
                        val = "application/json"; //$NON-NLS-1$
                    } else if (val.contains("+xml")) { //$NON-NLS-1$
                        val = "text/xml"; //$NON-NLS-1$
                    }
                }

                map.put(key, val);
            }
        }
    }

    private static String parseApiKey(HttpServerRequest req) {
        String headerKey = req.headers().get("X-API-Key");
        if (headerKey == null || headerKey.trim().length() == 0) {
            headerKey = req.getParam("apikey");
        }
        return headerKey;
    }
}
