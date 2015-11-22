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
package io.apiman.gateway.engine.es;

import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Application;
import io.apiman.gateway.engine.beans.Contract;
import io.apiman.gateway.engine.beans.Service;
import io.apiman.gateway.engine.beans.ServiceContract;
import io.apiman.gateway.engine.beans.ServiceRequest;
import io.apiman.gateway.engine.beans.exceptions.InvalidContractException;
import io.apiman.gateway.engine.es.i18n.Messages;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Get;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extends the {@link ESRegistry} to provide single-node caching.  This caching solution
 * will not work in a cluster.  If looking for cluster support, either go with the core
 * ESRegistry or find/implement a caching registry that works in a cluster (e.g. leverage
 * jgroups?).
 *
 * @author eric.wittmann@redhat.com
 */
public abstract class CachingESRegistry extends ESRegistry {

    private Map<String, ServiceContract> contractCache = new ConcurrentHashMap<>();
    private Map<String, Service> serviceCache = new HashMap<>();
    private Map<String, Application> applicationCache = new HashMap<>();
    private Object mutex = new Object();

    /**
     * Constructor.
     */
    public CachingESRegistry(Map<String, String> config) {
        super(config);
    }

    /**
     * Called to invalidate the cache - clearing it so that subsequent calls to getService()
     * or getContract() will trigger a new fetch from the ES store.
     */
    protected void invalidateCache() {
        synchronized (mutex) {
            contractCache.clear();
            serviceCache.clear();
            applicationCache.clear();
        }
    }

    /**
     * @see io.apiman.gateway.engine.es.ESRegistry#getContract(io.apiman.gateway.engine.beans.ServiceRequest, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void getContract(final ServiceRequest request, final IAsyncResultHandler<ServiceContract> handler) {
        ServiceContract contract = null;
        
        String contractKey = getContractKey(request);
        synchronized (mutex) {
            contract = contractCache.get(contractKey);
        }
        
        try {
            if (contract == null) {
                super.getContract(request, new IAsyncResultHandler<ServiceContract>() {
                    @Override
                    public void handle(IAsyncResult<ServiceContract> result) {
                        if (result.isSuccess()) {
                            loadAndCacheApp(result.getResult().getApplication());
                        }
                        handler.handle(result);
                    }
                });
            } else {
                Service service = getService(request.getServiceOrgId(), request.getServiceId(), request.getServiceVersion());
                if (service == null) {
                    throw new InvalidContractException(Messages.i18n.format("ESRegistry.ServiceWasRetired", //$NON-NLS-1$
                            request.getServiceId(), request.getServiceOrgId()));
                }
                contract.setService(service);
                handler.handle(AsyncResultImpl.create(contract));
            }
        } catch (Throwable e) {
            handler.handle(AsyncResultImpl.create(e, ServiceContract.class));
        }
    }

    /**
     * @see io.apiman.gateway.engine.es.ESRegistry#getService(java.lang.String, java.lang.String, java.lang.String, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void getService(final String organizationId, final String serviceId, final String serviceVersion,
            final IAsyncResultHandler<Service> handler) {
        try {
            Service service = getService(organizationId, serviceId, serviceVersion);
            handler.handle(AsyncResultImpl.create(service));
        } catch (IOException e) {
            handler.handle(AsyncResultImpl.create(e, Service.class));
        }
    }
    
    /**
     * Gets the service either from the cache or from ES.
     * @param orgId
     * @param serviceId
     * @param version
     */
    protected Service getService(String orgId, String serviceId, String version) throws IOException {
        String serviceKey = getServiceKey(orgId, serviceId, version);
        Service service = null;
        synchronized (mutex) {
            service = serviceCache.get(serviceKey);
        }
        
        if (service == null) {
            service = super.getService(getServiceId(orgId, serviceId, version));
            synchronized (mutex) {
                if (service != null) {
                    serviceCache.put(serviceKey, service);
                }
            }
        }
        
        return service;
    }
    
    /**
     * @see io.apiman.gateway.engine.es.ESRegistry#checkService(io.apiman.gateway.engine.beans.ServiceContract)
     */
    @Override
    protected void checkService(ServiceContract contract) throws InvalidContractException, IOException {
        Service service = getService(contract.getService().getOrganizationId(), 
                contract.getService().getServiceId(),
                contract.getService().getVersion());
        if (service == null) {
            throw new InvalidContractException(Messages.i18n.format("ESRegistry.ServiceWasRetired", //$NON-NLS-1$
                    contract.getService().getServiceId(), contract.getService().getOrganizationId()));
        }
    }

    /**
     * @param application
     */
    protected void cacheApplication(Application application) {
        String applicationKey = getApplicationKey(application);
        synchronized (mutex) {
            applicationCache.put(applicationKey, application);
            if (application.getContracts() != null) {
                for (Contract contract : application.getContracts()) {
                    ServiceContract sc = new ServiceContract(contract.getApiKey(), null, application, contract.getPlan(), contract.getPolicies());
                    String contractKey = getContractKey(contract);
                    contractCache.put(contractKey, sc);
                }
            }
        }
    }

    /**
     * @param application
     */
    protected void loadAndCacheApp(Application application) {
        String id = getApplicationId(application);
        Get get = new Get.Builder(getIndexName(), id).type("application").build(); //$NON-NLS-1$
        getClient().executeAsync(get, new JestResultHandler<JestResult>() {
            @Override
            public void completed(JestResult result) {
                if (result.isSucceeded()) {
                    Map<String, Object> source = result.getSourceAsObject(Map.class);
                    Application app = ESRegistryMarshalling.unmarshallApplication(source);
                    cacheApplication(app);
                }
            }
            @Override
            public void failed(Exception e) {
            }
        });
    }

    /**
     * Generates an in-memory key for an service, used to index the app for later quick
     * retrieval.
     * @param orgId
     * @param serviceId
     * @param version
     * @return a service key
     */
    private String getServiceKey(String orgId, String serviceId, String version) {
        return "SVC::" + orgId + "|" + serviceId + "|" + version; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /**
     * Generates an in-memory key for an application, used to index the app for later quick
     * retrieval.
     * @param app an application
     * @return an application key
     */
    private String getApplicationKey(Application app) {
        return "APP::" + app.getOrganizationId() + "|" + app.getApplicationId() + "|" + app.getVersion(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /**
     * Generates an in-memory key for a contract.
     * @param request
     */
    private String getContractKey(ServiceRequest request) {
        return "CONTRACT::" + request.getApiKey(); //$NON-NLS-1$
    }

    /**
     * Generates an in-memory key for a service contract, used to index the app for later quick
     * retrieval.
     * @param contract
     */
    private String getContractKey(Contract contract) {
        return "CONTRACT::" + contract.getApiKey(); //$NON-NLS-1$
    }

}
