/*
 * Copyright 2013 JBoss Inc
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

package io.apiman.gateway.platforms.servlet;

import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.beans.ServiceRequest;
import io.apiman.gateway.engine.beans.ServiceResponse;

/**
 * Thread context for the WAR implementation of the gateway.
 *
 * @author eric.wittmann@redhat.com
 */
public class GatewayThreadContext {
    private static final ThreadLocal<ServiceRequest> serviceRequest = new ThreadLocal<>();
    private static final ThreadLocal<ServiceResponse> serviceResponse = new ThreadLocal<>();
    private static final ThreadLocal<PolicyFailure> policyFailure = new ThreadLocal<>();

    /**
     * @return the thread-local service request
     */
    public static final ServiceRequest getServiceRequest() {
        ServiceRequest request = serviceRequest.get();
        if (request == null) {
            request = new ServiceRequest();
            serviceRequest.set(request);
        }
        request.setApiKey(null);
        request.setUrl(null);
        request.setDestination(null);
        request.getHeaders().clear();
        request.setRawRequest(null);
        request.setRemoteAddr(null);
        request.setType(null);
        request.setTransportSecure(false);
        return request;
    }

    /**
     * @return the thread-local service response
     */
    public static final ServiceResponse getServiceResponse() {
        ServiceResponse response = serviceResponse.get();
        if (response == null) {
            response = new ServiceResponse();
            serviceResponse.set(response);
        }
        response.setCode(0);
        response.getHeaders().clear();
        response.setMessage(null);
        response.getAttributes().clear();
        return response;
    }

    /**
     * @return the thread-local policy failure
     */
    public static final PolicyFailure getPolicyFailure() {
        PolicyFailure failure = policyFailure.get();
        if (failure == null) {
            failure = new PolicyFailure();
            policyFailure.set(failure);
        }
        failure.setResponseCode(0);
        failure.setFailureCode(0);
        failure.setMessage(null);
        failure.setType(null);
        failure.getHeaders().clear();
        return failure;
    }

}
