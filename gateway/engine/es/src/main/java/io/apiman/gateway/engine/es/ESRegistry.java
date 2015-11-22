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

import io.apiman.gateway.engine.IRegistry;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Application;
import io.apiman.gateway.engine.beans.Contract;
import io.apiman.gateway.engine.beans.Service;
import io.apiman.gateway.engine.beans.ServiceContract;
import io.apiman.gateway.engine.beans.ServiceRequest;
import io.apiman.gateway.engine.beans.exceptions.InvalidContractException;
import io.apiman.gateway.engine.beans.exceptions.PublishingException;
import io.apiman.gateway.engine.beans.exceptions.RegistrationException;
import io.apiman.gateway.engine.es.i18n.Messages;
import io.searchbox.client.JestResult;
import io.searchbox.core.Delete;
import io.searchbox.core.DeleteByQuery;
import io.searchbox.core.Get;
import io.searchbox.core.Index;
import io.searchbox.params.Parameters;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * An implementation of the Registry that uses elasticsearch as a storage
 * mechanism.
 *
 * @author eric.wittmann@redhat.com
 */
public class ESRegistry extends AbstractESComponent implements IRegistry {

    /**
     * Constructor.
     * @param config map of configuration options
     */
    public ESRegistry(Map<String, String> config) {
        super(config);
    }

    /**
     * @see io.apiman.gateway.engine.IRegistry#publishService(io.apiman.gateway.engine.beans.Service, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void publishService(final Service service, final IAsyncResultHandler<Void> handler) {
        try {
            String id = getServiceId(service);
            Index index = new Index.Builder(ESRegistryMarshalling.marshall(service).string()).refresh(false)
                    .index(getIndexName()).setParameter(Parameters.OP_TYPE, "create") //$NON-NLS-1$
                    .type("service").id(id).build(); //$NON-NLS-1$
            JestResult result = getClient().execute(index);
            if (!result.isSucceeded()) {
                handler.handle(AsyncResultImpl.create(
                        new PublishingException(Messages.i18n.format("ESRegistry.ServiceAlreadyPublished")),  //$NON-NLS-1$
                        Void.class));
            } else {
                handler.handle(AsyncResultImpl.create((Void) null));
            }
        } catch (Exception e) {
            handler.handle(AsyncResultImpl.create(
                    new PublishingException(Messages.i18n.format("ESRegistry.ErrorPublishingService"), e),  //$NON-NLS-1$
                    Void.class));
        }
    }

    /**
     * @see io.apiman.gateway.engine.IRegistry#retireService(io.apiman.gateway.engine.beans.Service, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void retireService(Service service, final IAsyncResultHandler<Void> handler) {
        final String id = getServiceId(service);

        try {
            Delete delete = new Delete.Builder(id).index(getIndexName()).type("service").build(); //$NON-NLS-1$
            JestResult result = getClient().execute(delete);
            if (result.isSucceeded()) {
                handler.handle(AsyncResultImpl.create((Void) null));
            } else {
                handler.handle(AsyncResultImpl.create(new PublishingException(Messages.i18n.format("ESRegistry.ServiceNotFound")), Void.class)); //$NON-NLS-1$
            }
        } catch (IOException e) {
            handler.handle(AsyncResultImpl.create(new PublishingException(Messages.i18n.format("ESRegistry.ErrorRetiringService"), e), Void.class)); //$NON-NLS-1$
        }
    }

    /**
     * @see io.apiman.gateway.engine.IRegistry#registerApplication(io.apiman.gateway.engine.beans.Application, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void registerApplication(final Application application, final IAsyncResultHandler<Void> handler) {
        final Map<String, Service> svcMap = new HashMap<>();
        
        try {
            // Validate the app and populate the service map with services found during validation.
            validateApplication(application, svcMap);
            String id = getApplicationId(application);
            Index index = new Index.Builder(ESRegistryMarshalling.marshall(application).string())
                    .refresh(false).index(getIndexName())
                    .setParameter(Parameters.OP_TYPE, "create") //$NON-NLS-1$
                    .type("application").id(id).build(); //$NON-NLS-1$
            JestResult result = getClient().execute(index);
            if (!result.isSucceeded()) {
                throw new RegistrationException(Messages.i18n.format("ESRegistry.AppAlreadyRegistered")); //$NON-NLS-1$
            } else {
                Set<Contract> contracts = application.getContracts();
                application.setContracts(null);
                for (Contract contract : contracts) {
                    registerContract(application, contract, svcMap);
                }
                handler.handle(AsyncResultImpl.create((Void) null));
            }
        } catch (IOException e) {
            handler.handle(AsyncResultImpl.create(
                    new RegistrationException(Messages.i18n.format("ESRegistry.ErrorRegisteringApplication"), e),  //$NON-NLS-1$
                    Void.class));
        } catch (RegistrationException re) {
            handler.handle(AsyncResultImpl.create(re, Void.class));
        }
    }

    /**
     * Validate that the application should be registered.
     * @param application
     * @param serviceMap
     */
    private void validateApplication(Application application, Map<String, Service> serviceMap) throws RegistrationException {
        Set<Contract> contracts = application.getContracts();
        if (contracts.isEmpty()) {
            throw new RegistrationException(Messages.i18n.format("ESRegistry.NoContracts")); //$NON-NLS-1$
        }
        for (Contract contract : contracts) {
            validateContract(contract, serviceMap);
        }
    }

    /**
     * Ensures that the service referenced by the Contract at the head of
     * the iterator actually exists (is published).
     * @param contract
     * @param serviceMap
     */
    private void validateContract(final Contract contract, final Map<String, Service> serviceMap)
            throws RegistrationException {
        
        final String serviceId = getServiceId(contract);

        try {
            Get get = new Get.Builder(getIndexName(), serviceId).type("service").build(); //$NON-NLS-1$
            JestResult result = getClient().execute(get);
            if (result.isSucceeded()) {
                Map<String, Object> source = result.getSourceAsObject(Map.class);
                Service service = ESRegistryMarshalling.unmarshallService(source);
                service.setServicePolicies(null);
                serviceMap.put(serviceId, service);
            } else {
                String svcId = contract.getServiceId();
                String orgId = contract.getServiceOrgId();
                throw new RegistrationException(Messages.i18n.format("ESRegistry.ServiceNotFoundInOrg", svcId, orgId));  //$NON-NLS-1$
            }
        } catch (IOException e) {
            throw new RegistrationException(Messages.i18n.format("ESRegistry.ErrorValidatingApp"), e); //$NON-NLS-1$
        }
    }

    /**
     * Register all the contracts in ES so they can be looked up quickly by
     * their ID by all nodes in the cluster.
     * @param application
     * @param contracts
     * @param serviceMap
     */
    private void registerContract(final Application application, final Contract contract,
            final Map<String, Service> serviceMap) throws RegistrationException {
        try {
            String svcId = getServiceId(contract);
            Service service = serviceMap.get(svcId);
            ServiceContract sc = new ServiceContract(contract.getApiKey(), service, application,
                    contract.getPlan(), contract.getPolicies());
            final String contractId = getContractId(contract);

            Index index = new Index.Builder(ESRegistryMarshalling.marshall(sc).string()).refresh(false)
                    .setParameter(Parameters.OP_TYPE, "create") //$NON-NLS-1$
                    .index(getIndexName()).type("serviceContract").id(contractId).build(); //$NON-NLS-1$
            JestResult result = getClient().execute(index);
            if (!result.isSucceeded()) {
                throw new RegistrationException(Messages.i18n.format("ESRegistry.ContractAlreadyPublished", contractId)); //$NON-NLS-1$
            }
        } catch (Exception e) {
            throw new RegistrationException(Messages.i18n.format("ESRegistry.ErrorRegisteringContract"), e);  //$NON-NLS-1$
        }
    }

    /**
     * @see io.apiman.gateway.engine.IRegistry#unregisterApplication(io.apiman.gateway.engine.beans.Application, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void unregisterApplication(final Application application, final IAsyncResultHandler<Void> handler) {
        final String id = getApplicationId(application);

        try {
            Delete delete = new Delete.Builder(id).index(getIndexName()).type("application").build(); //$NON-NLS-1$
            JestResult result = getClient().execute(delete);
            if (result.isSucceeded()) {
                unregisterServiceContracts(application);
                handler.handle(AsyncResultImpl.create((Void) null));
            } else {
                handler.handle(AsyncResultImpl.create(new PublishingException(Messages.i18n.format("ESRegistry.AppNotFound")), Void.class)); //$NON-NLS-1$
            }
        } catch (IOException e) {
            handler.handle(AsyncResultImpl.create(new PublishingException(Messages.i18n.format("ESRegistry.ErrorUnregisteringApp"), e), Void.class)); //$NON-NLS-1$
        }
    }

    /**
     * Removes all of the service contracts from ES.
     * @param application
     * @throws IOException 
     */
    protected void unregisterServiceContracts(Application application) throws IOException {
        QueryBuilder qb = QueryBuilders.filteredQuery(
                QueryBuilders.matchAllQuery(),
                FilterBuilders.andFilter(
                        FilterBuilders.termFilter("application.organizationId", application.getOrganizationId()), //$NON-NLS-1$
                        FilterBuilders.termFilter("application.applicationId", application.getApplicationId()), //$NON-NLS-1$
                        FilterBuilders.termFilter("application.version", application.getVersion()) //$NON-NLS-1$
                )
            );
        @SuppressWarnings("nls")
        String dquery = "{\"query\" : " + qb.toString() + "}";
        DeleteByQuery delete = new DeleteByQuery.Builder(dquery).addIndex(getIndexName()).addType("serviceContract").build(); //$NON-NLS-1$
        getClient().execute(delete);
    }

    /**
     * @see io.apiman.gateway.engine.IRegistry#getContract(io.apiman.gateway.engine.beans.ServiceRequest, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void getContract(final ServiceRequest request, final IAsyncResultHandler<ServiceContract> handler) {
        final String id = getContractId(request);

        try {
            Get get = new Get.Builder(getIndexName(), id).type("serviceContract").build(); //$NON-NLS-1$
            JestResult result = getClient().execute(get);
            if (!result.isSucceeded()) {
                Exception error = new InvalidContractException(Messages.i18n.format("ESRegistry.NoContractForAPIKey", id)); //$NON-NLS-1$
                handler.handle(AsyncResultImpl.create(error, ServiceContract.class));
            } else {
                Map<String, Object> source = result.getSourceAsObject(Map.class);
                ServiceContract contract = ESRegistryMarshalling.unmarshallServiceContract(source);
                checkService(contract);
                handler.handle(AsyncResultImpl.create(contract));
            }
        } catch (IOException e) {
            handler.handle(AsyncResultImpl.create(e, ServiceContract.class));
        }
    }

    /**
     * Ensure that the service still exists.  If not, it was retired.
     * @param contract
     * @throws InvalidContractException
     * @throws IOException
     */
    protected void checkService(final ServiceContract contract) throws InvalidContractException, IOException {
        final Service service = contract.getService();
        String id = getServiceId(service);

        Get get = new Get.Builder(getIndexName(), id).type("service").build(); //$NON-NLS-1$
        JestResult result = getClient().execute(get);
        if (!result.isSucceeded()) {
            throw new InvalidContractException(Messages.i18n.format("ESRegistry.ServiceWasRetired", //$NON-NLS-1$
                    service.getServiceId(), service.getOrganizationId()));
        }
    }

    /**
     * @see io.apiman.gateway.engine.IRegistry#getService(java.lang.String, java.lang.String, java.lang.String, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void getService(String organizationId, String serviceId, String serviceVersion,
            IAsyncResultHandler<Service> handler) {
        String id = getServiceId(organizationId, serviceId, serviceVersion);
        getService(id, handler);
    }

    /**
     * Asynchronously gets a service.
     * @param id
     * @param handler
     */
    protected void getService(String id, final IAsyncResultHandler<Service> handler) {
        try {
            Service service = getService(id);
            handler.handle(AsyncResultImpl.create(service));
        } catch (IOException e) {
            handler.handle(AsyncResultImpl.create(e, Service.class));
        }
    }
    
    /**
     * Gets the service synchronously.
     * @param id
     * @throws IOException
     */
    protected Service getService(String id) throws IOException {
        Get get = new Get.Builder(getIndexName(), id).type("service").build(); //$NON-NLS-1$
        JestResult result = getClient().execute(get);
        if (result.isSucceeded()) {
            Map<String, Object> source = result.getSourceAsObject(Map.class);
            Service service = ESRegistryMarshalling.unmarshallService(source);
            return service;
        } else {
            return null;
        }
    }

    /**
     * Generates a valid document ID for a service, used to index the service in ES.
     * @param service an service
     * @return a service key
     */
    private String getServiceId(Service service) {
        return getServiceId(service.getOrganizationId(), service.getServiceId(), service.getVersion());
    }

    /**
     * Generates a valid document ID for a service referenced by a contract, used to
     * retrieve the service from ES.
     * @param contract
     */
    private String getServiceId(Contract contract) {
        return getServiceId(contract.getServiceOrgId(), contract.getServiceId(), contract.getServiceVersion());
    }

    /**
     * Generates a valid document ID for a service, used to index the service in ES.
     * @param orgId
     * @param serviceId
     * @param version
     * @return a service key
     */
    protected String getServiceId(String orgId, String serviceId, String version) {
        return orgId + ":" + serviceId + ":" + version; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Generates a valid document ID for an application, used to index the app in ES.
     * @param app an application
     * @return an application key
     */
    protected String getApplicationId(Application app) {
        return app.getOrganizationId() + ":" + app.getApplicationId() + ":" + app.getVersion(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Generates a valid document ID for a contract, used to index the contract in ES.
     * @param request
     */
    private String getContractId(ServiceRequest request) {
        return request.getApiKey();
    }

    /**
     * Generates a valid document ID for a contract, used to index the contract in ES.
     * @param contract
     */
    private String getContractId(Contract contract) {
        return contract.getApiKey();
    }

    /**
     * @see io.apiman.gateway.engine.es.AbstractESComponent#getIndexName()
     */
    @Override
    protected String getIndexName() {
        return ESConstants.GATEWAY_INDEX_NAME;
    }

}
