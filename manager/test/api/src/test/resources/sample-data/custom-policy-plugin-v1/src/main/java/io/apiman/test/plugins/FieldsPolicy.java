/*
 * Copyright 2015 JBoss Inc
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
package io.apiman.test.plugins;

import io.apiman.gateway.engine.beans.ServiceRequest;
import io.apiman.gateway.engine.beans.ServiceResponse;
import io.apiman.gateway.engine.policies.AbstractMappedPolicy;
import io.apiman.gateway.engine.policy.IPolicyChain;
import io.apiman.gateway.engine.policy.IPolicyContext;

/**
 * @author eric.wittmann@redhat.com
 */
public class FieldsPolicy extends AbstractMappedPolicy<FieldsBean> {

    /**
     * Constructor.
     */
    public FieldsPolicy() {
    }

    /**
     * @see io.apiman.gateway.engine.policies.AbstractMappedPolicy#getConfigurationClass()
     */
    @Override
    protected Class<FieldsBean> getConfigurationClass() {
        return FieldsBean.class;
    }

    /**
     * @see io.apiman.gateway.engine.policies.AbstractMappedPolicy#doApply(io.apiman.gateway.engine.beans.ServiceRequest, io.apiman.gateway.engine.policy.IPolicyContext, java.lang.Object, io.apiman.gateway.engine.policy.IPolicyChain)
     */
    @SuppressWarnings("nls")
    @Override
    protected void doApply(ServiceRequest request, IPolicyContext context, FieldsBean config,
            IPolicyChain<ServiceRequest> chain) {
        System.out.println("Executing v1 of FieldsPolicy.");
        request.getHeaders().put("X-FieldsPolicy-Version", "v1");
        super.doApply(request, context, config, chain);
    }

    /**
     * @see io.apiman.gateway.engine.policies.AbstractMappedPolicy#doApply(io.apiman.gateway.engine.beans.ServiceResponse, io.apiman.gateway.engine.policy.IPolicyContext, java.lang.Object, io.apiman.gateway.engine.policy.IPolicyChain)
     */
    @Override
    protected void doApply(ServiceResponse response, IPolicyContext context, FieldsBean config,
            IPolicyChain<ServiceResponse> chain) {
        super.doApply(response, context, config, chain);
    }

}
