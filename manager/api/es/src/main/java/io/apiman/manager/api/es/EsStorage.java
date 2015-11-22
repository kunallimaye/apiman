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
package io.apiman.manager.api.es;

import io.apiman.manager.api.beans.apps.ApplicationBean;
import io.apiman.manager.api.beans.apps.ApplicationVersionBean;
import io.apiman.manager.api.beans.audit.AuditEntityType;
import io.apiman.manager.api.beans.audit.AuditEntryBean;
import io.apiman.manager.api.beans.contracts.ContractBean;
import io.apiman.manager.api.beans.download.DownloadBean;
import io.apiman.manager.api.beans.gateways.GatewayBean;
import io.apiman.manager.api.beans.idm.PermissionBean;
import io.apiman.manager.api.beans.idm.PermissionType;
import io.apiman.manager.api.beans.idm.RoleBean;
import io.apiman.manager.api.beans.idm.RoleMembershipBean;
import io.apiman.manager.api.beans.idm.UserBean;
import io.apiman.manager.api.beans.orgs.OrganizationBean;
import io.apiman.manager.api.beans.plans.PlanBean;
import io.apiman.manager.api.beans.plans.PlanVersionBean;
import io.apiman.manager.api.beans.plugins.PluginBean;
import io.apiman.manager.api.beans.policies.PolicyBean;
import io.apiman.manager.api.beans.policies.PolicyDefinitionBean;
import io.apiman.manager.api.beans.policies.PolicyType;
import io.apiman.manager.api.beans.search.OrderByBean;
import io.apiman.manager.api.beans.search.PagingBean;
import io.apiman.manager.api.beans.search.SearchCriteriaBean;
import io.apiman.manager.api.beans.search.SearchCriteriaFilterBean;
import io.apiman.manager.api.beans.search.SearchCriteriaFilterOperator;
import io.apiman.manager.api.beans.search.SearchResultsBean;
import io.apiman.manager.api.beans.services.ServiceBean;
import io.apiman.manager.api.beans.services.ServiceGatewayBean;
import io.apiman.manager.api.beans.services.ServicePlanBean;
import io.apiman.manager.api.beans.services.ServiceVersionBean;
import io.apiman.manager.api.beans.summary.ApiEntryBean;
import io.apiman.manager.api.beans.summary.ApiRegistryBean;
import io.apiman.manager.api.beans.summary.ApplicationSummaryBean;
import io.apiman.manager.api.beans.summary.ApplicationVersionSummaryBean;
import io.apiman.manager.api.beans.summary.ContractSummaryBean;
import io.apiman.manager.api.beans.summary.GatewaySummaryBean;
import io.apiman.manager.api.beans.summary.OrganizationSummaryBean;
import io.apiman.manager.api.beans.summary.PlanSummaryBean;
import io.apiman.manager.api.beans.summary.PlanVersionSummaryBean;
import io.apiman.manager.api.beans.summary.PluginSummaryBean;
import io.apiman.manager.api.beans.summary.PolicyDefinitionSummaryBean;
import io.apiman.manager.api.beans.summary.PolicySummaryBean;
import io.apiman.manager.api.beans.summary.ServicePlanSummaryBean;
import io.apiman.manager.api.beans.summary.ServiceSummaryBean;
import io.apiman.manager.api.beans.summary.ServiceVersionSummaryBean;
import io.apiman.manager.api.core.IStorage;
import io.apiman.manager.api.core.IStorageQuery;
import io.apiman.manager.api.core.exceptions.StorageException;
import io.apiman.manager.api.core.util.PolicyTemplateUtil;
import io.apiman.manager.api.es.beans.PoliciesBean;
import io.apiman.manager.api.es.beans.ServiceDefinitionBean;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.core.Delete;
import io.searchbox.core.DeleteByQuery;
import io.searchbox.core.Get;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.SearchResult.Hit;
import io.searchbox.core.SearchScroll;
import io.searchbox.core.SearchScroll.Builder;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.params.Parameters;
import io.searchbox.params.SearchType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.Base64;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.BaseQueryBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.google.gson.Gson;

/**
 * An implementation of the API Manager persistence layer that uses git to store
 * the entities.
 *
 * @author eric.wittmann@redhat.com
 */
@ApplicationScoped @Alternative
public class EsStorage implements IStorage, IStorageQuery {

    private static final String INDEX_NAME = "apiman_manager"; //$NON-NLS-1$

    private static int guidCounter = 100;

    @Inject @Named("storage")
    JestClient esClient;

    /**
     * Constructor.
     */
    public EsStorage() {
    }

    /**
     * Called to initialize the storage.
     */
    public void initialize() {
        try {
            esClient.execute(new Health.Builder().build());
            // TODO Do we need a loop to wait for all nodes to join the cluster?
            Action<JestResult> action = new IndicesExists.Builder(INDEX_NAME).build();
            JestResult result = esClient.execute(action);
            if (! result.isSucceeded()) {
                createIndex(INDEX_NAME);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param indexName
     * @throws Exception
     */
    private void createIndex(String indexName) throws Exception {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        URL settings = getClass().getResource("index-settings.json"); //$NON-NLS-1$
        String source = IOUtils.toString(settings);
        request.source(source);
        JestResult response = esClient.execute(new CreateIndex.Builder(indexName).settings(source).build());
        if (!response.isSucceeded()) {
            throw new StorageException("Failed to create index " + indexName + ": " + response.getErrorMessage()); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#beginTx()
     */
    @Override
    public void beginTx() throws StorageException {
        // No Transaction support for ES
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#commitTx()
     */
    @Override
    public void commitTx() throws StorageException {
        // No Transaction support for ES
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#rollbackTx()
     */
    @Override
    public void rollbackTx() {
        // No Transaction support for ES
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createOrganization(io.apiman.manager.api.beans.orgs.OrganizationBean)
     */
    @Override
    public void createOrganization(OrganizationBean organization) throws StorageException {
        indexEntity("organization", organization.getId(), EsMarshalling.marshall(organization), true); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createApplication(io.apiman.manager.api.beans.apps.ApplicationBean)
     */
    @Override
    public void createApplication(ApplicationBean application) throws StorageException {
        indexEntity("application", id(application.getOrganization().getId(), application.getId()), EsMarshalling.marshall(application)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createApplicationVersion(io.apiman.manager.api.beans.apps.ApplicationVersionBean)
     */
    @Override
    public void createApplicationVersion(ApplicationVersionBean version) throws StorageException {
        ApplicationBean application = version.getApplication();
        String id = id(application.getOrganization().getId(), application.getId(), version.getVersion());
        indexEntity("applicationVersion", id, EsMarshalling.marshall(version)); //$NON-NLS-1$
        PoliciesBean policies = PoliciesBean.from(PolicyType.Application, application.getOrganization().getId(),
                application.getId(), version.getVersion());
        indexEntity("applicationPolicies", id, EsMarshalling.marshall(policies)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createContract(io.apiman.manager.api.beans.contracts.ContractBean)
     */
    @Override
    public void createContract(ContractBean contract) throws StorageException {
        List<ContractSummaryBean> contracts = getApplicationContracts(contract.getApplication().getApplication().getOrganization().getId(),
                contract.getApplication().getApplication().getId(), contract.getApplication().getVersion());
        for (ContractSummaryBean csb : contracts) {
            if (csb.getServiceOrganizationId().equals(contract.getService().getService().getOrganization().getId()) &&
                    csb.getServiceId().equals(contract.getService().getService().getId()) &&
                    csb.getServiceVersion().equals(contract.getService().getVersion()) &&
                    csb.getPlanId().equals(contract.getPlan().getPlan().getId()))
                {
                    throw new StorageException("Error creating contract: duplicate contract detected."); //$NON-NLS-1$
                }
        }
        contract.setId(generateGuid());
        indexEntity("contract", String.valueOf(contract.getId()), EsMarshalling.marshall(contract), true); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createService(io.apiman.manager.api.beans.services.ServiceBean)
     */
    @Override
    public void createService(ServiceBean service) throws StorageException {
        indexEntity("service", id(service.getOrganization().getId(), service.getId()), EsMarshalling.marshall(service)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createServiceVersion(io.apiman.manager.api.beans.services.ServiceVersionBean)
     */
    @Override
    public void createServiceVersion(ServiceVersionBean version) throws StorageException {
        ServiceBean service = version.getService();
        String id = id(service.getOrganization().getId(), service.getId(), version.getVersion());
        indexEntity("serviceVersion", id, EsMarshalling.marshall(version)); //$NON-NLS-1$
        PoliciesBean policies = PoliciesBean.from(PolicyType.Service, service.getOrganization().getId(),
                service.getId(), version.getVersion());
        indexEntity("servicePolicies", id, EsMarshalling.marshall(policies)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createPlan(io.apiman.manager.api.beans.plans.PlanBean)
     */
    @Override
    public void createPlan(PlanBean plan) throws StorageException {
        indexEntity("plan", id(plan.getOrganization().getId(), plan.getId()), EsMarshalling.marshall(plan)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createPlanVersion(io.apiman.manager.api.beans.plans.PlanVersionBean)
     */
    @Override
    public void createPlanVersion(PlanVersionBean version) throws StorageException {
        PlanBean plan = version.getPlan();
        String id = id(plan.getOrganization().getId(), plan.getId(), version.getVersion());
        indexEntity("planVersion", id, EsMarshalling.marshall(version)); //$NON-NLS-1$
        PoliciesBean policies = PoliciesBean.from(PolicyType.Plan, plan.getOrganization().getId(),
                plan.getId(), version.getVersion());
        indexEntity("planPolicies", id, EsMarshalling.marshall(policies)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createPolicy(io.apiman.manager.api.beans.policies.PolicyBean)
     */
    @Override
    public void createPolicy(PolicyBean policy) throws StorageException {
        String docType = getPoliciesDocType(policy.getType());
        String id = id(policy.getOrganizationId(), policy.getEntityId(), policy.getEntityVersion());
        Map<String, Object> source = getEntity(docType, id);
        if (source == null) {
            throw new StorageException("Failed to create policy (missing PoliciesBean)."); //$NON-NLS-1$
        }
        PoliciesBean policies = EsMarshalling.unmarshallPolicies(source);
        policy.setId(generateGuid());
        policies.getPolicies().add(policy);
        orderPolicies(policies);
        updateEntity(docType, id, EsMarshalling.marshall(policies));
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#reorderPolicies(io.apiman.manager.api.beans.policies.PolicyType, java.lang.String, java.lang.String, java.lang.String, java.util.List)
     */
    @Override
    public void reorderPolicies(PolicyType type, String organizationId, String entityId,
            String entityVersion, List<Long> newOrder) throws StorageException {
        String docType = getPoliciesDocType(type);
        String pid = id(organizationId, entityId, entityVersion);
        Map<String, Object> source = getEntity(docType, pid);
        if (source == null) {
            return;
        }
        PoliciesBean policiesBean = EsMarshalling.unmarshallPolicies(source);
        List<PolicyBean> policies = policiesBean.getPolicies();
        List<PolicyBean> reordered = new ArrayList<>(policies.size());
        for (Long policyId : newOrder) {
            ListIterator<PolicyBean> iterator = policies.listIterator();
            while (iterator.hasNext()) {
                PolicyBean policyBean = iterator.next();
                if (policyBean.getId().equals(policyId)) {
                    iterator.remove();
                    reordered.add(policyBean);
                    break;
                }
            }
        }
        // Make sure we don't stealth-delete any policies.  Put anything
        // remaining at the end of the list.
        for (PolicyBean policyBean : policies) {
            reordered.add(policyBean);
        }
        policiesBean.setPolicies(reordered);
        updateEntity(docType, pid, EsMarshalling.marshall(policiesBean));
    }

    /**
     * Set the order index of all policies.
     * @param policies
     */
    private void orderPolicies(PoliciesBean policies) {
        int idx = 1;
        for (PolicyBean policy : policies.getPolicies()) {
            policy.setOrderIndex(idx++);
        }
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createGateway(io.apiman.manager.api.beans.gateways.GatewayBean)
     */
    @Override
    public void createGateway(GatewayBean gateway) throws StorageException {
        indexEntity("gateway", gateway.getId(), EsMarshalling.marshall(gateway)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createPlugin(io.apiman.manager.api.beans.plugins.PluginBean)
     */
    @Override
    public void createPlugin(PluginBean plugin) throws StorageException {
        plugin.setId(generateGuid());
        indexEntity("plugin", String.valueOf(plugin.getId()), EsMarshalling.marshall(plugin), true); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createDownload(io.apiman.manager.api.beans.download.DownloadBean)
     */
    @Override
    public void createDownload(DownloadBean download) throws StorageException {
        indexEntity("download", download.getId(), EsMarshalling.marshall(download)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createPolicyDefinition(io.apiman.manager.api.beans.policies.PolicyDefinitionBean)
     */
    @Override
    public void createPolicyDefinition(PolicyDefinitionBean policyDef) throws StorageException {
        indexEntity("policyDef", policyDef.getId(), EsMarshalling.marshall(policyDef)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createRole(io.apiman.manager.api.beans.idm.RoleBean)
     */
    @Override
    public void createRole(RoleBean role) throws StorageException {
        indexEntity("role", role.getId(), EsMarshalling.marshall(role)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createAuditEntry(io.apiman.manager.api.beans.audit.AuditEntryBean)
     */
    @Override
    public void createAuditEntry(AuditEntryBean entry) throws StorageException {
        if (entry == null) {
            return;
        }
        entry.setId(generateGuid());
        indexEntity("auditEntry", String.valueOf(entry.getId()), EsMarshalling.marshall(entry)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updateOrganization(io.apiman.manager.api.beans.orgs.OrganizationBean)
     */
    @Override
    public void updateOrganization(OrganizationBean organization) throws StorageException {
        updateEntity("organization", organization.getId(), EsMarshalling.marshall(organization)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updateApplication(io.apiman.manager.api.beans.apps.ApplicationBean)
     */
    @Override
    public void updateApplication(ApplicationBean application) throws StorageException {
        updateEntity("application", id(application.getOrganization().getId(), application.getId()), EsMarshalling.marshall(application)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updateApplicationVersion(io.apiman.manager.api.beans.apps.ApplicationVersionBean)
     */
    @Override
    public void updateApplicationVersion(ApplicationVersionBean version) throws StorageException {
        ApplicationBean application = version.getApplication();
        updateEntity("applicationVersion", id(application.getOrganization().getId(), application.getId(), version.getVersion()),  //$NON-NLS-1$
                EsMarshalling.marshall(version));
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updateService(io.apiman.manager.api.beans.services.ServiceBean)
     */
    @Override
    public void updateService(ServiceBean service) throws StorageException {
        updateEntity("service", id(service.getOrganization().getId(), service.getId()), EsMarshalling.marshall(service)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updateServiceVersion(io.apiman.manager.api.beans.services.ServiceVersionBean)
     */
    @Override
    public void updateServiceVersion(ServiceVersionBean version) throws StorageException {
        ServiceBean service = version.getService();
        updateEntity("serviceVersion", id(service.getOrganization().getId(), service.getId(), version.getVersion()),  //$NON-NLS-1$
                EsMarshalling.marshall(version));
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updateServiceDefinition(io.apiman.manager.api.beans.services.ServiceVersionBean, java.io.InputStream)
     */
    @Override
    public void updateServiceDefinition(ServiceVersionBean version, InputStream definitionStream)
            throws StorageException {
        InputStream serviceDefinition = null;
        try {
            String id = id(version.getService().getOrganization().getId(), version.getService().getId(), version.getVersion()) + ":def"; //$NON-NLS-1$
            serviceDefinition = getServiceDefinition(version);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(definitionStream, baos);
            String data = Base64.encodeBytes(baos.toByteArray());
            ServiceDefinitionBean definition = new ServiceDefinitionBean();
            definition.setData(data);
            if (serviceDefinition == null) {
                indexEntity("serviceDefinition", id, EsMarshalling.marshall(definition)); //$NON-NLS-1$
            } else {
                updateEntity("serviceDefinition", id, EsMarshalling.marshall(definition)); //$NON-NLS-1$
            }
        } catch (IOException e) {
            throw new StorageException(e);
        } finally {
            IOUtils.closeQuietly(serviceDefinition);
        }
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updatePlan(io.apiman.manager.api.beans.plans.PlanBean)
     */
    @Override
    public void updatePlan(PlanBean plan) throws StorageException {
        updateEntity("plan", id(plan.getOrganization().getId(), plan.getId()), EsMarshalling.marshall(plan)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updatePlanVersion(io.apiman.manager.api.beans.plans.PlanVersionBean)
     */
    @Override
    public void updatePlanVersion(PlanVersionBean version) throws StorageException {
        PlanBean plan = version.getPlan();
        updateEntity("planVersion", id(plan.getOrganization().getId(), plan.getId(), version.getVersion()),  //$NON-NLS-1$
                EsMarshalling.marshall(version));
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updatePolicy(io.apiman.manager.api.beans.policies.PolicyBean)
     */
    @Override
    public void updatePolicy(PolicyBean policy) throws StorageException {
        String docType = getPoliciesDocType(policy.getType());
        String pid = id(policy.getOrganizationId(), policy.getEntityId(), policy.getEntityVersion());
        Map<String, Object> source = getEntity(docType, pid);
        if (source == null) {
            throw new StorageException("Policy not found."); //$NON-NLS-1$
        }
        PoliciesBean policies = EsMarshalling.unmarshallPolicies(source);
        List<PolicyBean> policyBeans = policies.getPolicies();
        boolean found = false;
        if (policyBeans != null) {
            for (PolicyBean policyBean : policyBeans) {
                if (policyBean.getId().equals(policy.getId())) {
                    policyBean.setConfiguration(policy.getConfiguration());
                    policyBean.setModifiedBy(policy.getModifiedBy());
                    policyBean.setModifiedOn(policy.getModifiedOn());
                    found = true;
                    break;
                }
            }
        }
        if (found) {
            updateEntity(docType, pid, EsMarshalling.marshall(policies));
        } else {
            throw new StorageException("Policy not found."); //$NON-NLS-1$
        }
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updateGateway(io.apiman.manager.api.beans.gateways.GatewayBean)
     */
    @Override
    public void updateGateway(GatewayBean gateway) throws StorageException {
        updateEntity("gateway", gateway.getId(), EsMarshalling.marshall(gateway)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updatePolicyDefinition(io.apiman.manager.api.beans.policies.PolicyDefinitionBean)
     */
    @Override
    public void updatePolicyDefinition(PolicyDefinitionBean policyDef) throws StorageException {
        updateEntity("policyDef", policyDef.getId(), EsMarshalling.marshall(policyDef)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updatePlugin(io.apiman.manager.api.beans.plugins.PluginBean)
     */
    @Override
    public void updatePlugin(PluginBean pluginBean) throws StorageException {
        updateEntity("plugin", String.valueOf(pluginBean.getId()), EsMarshalling.marshall(pluginBean)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updateRole(io.apiman.manager.api.beans.idm.RoleBean)
     */
    @Override
    public void updateRole(RoleBean role) throws StorageException {
        updateEntity("role", role.getId(), EsMarshalling.marshall(role)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deleteOrganization(io.apiman.manager.api.beans.orgs.OrganizationBean)
     */
    @Override
    public void deleteOrganization(OrganizationBean organization) throws StorageException {
        deleteEntity("organization", organization.getId()); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deleteApplication(io.apiman.manager.api.beans.apps.ApplicationBean)
     */
    @Override
    public void deleteApplication(ApplicationBean application) throws StorageException {
        deleteEntity("application", id(application.getOrganization().getId(), application.getId())); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deleteApplicationVersion(io.apiman.manager.api.beans.apps.ApplicationVersionBean)
     */
    @Override
    public void deleteApplicationVersion(ApplicationVersionBean version) throws StorageException {
        ApplicationBean application = version.getApplication();
        deleteEntity("applicationVersion", id(application.getOrganization().getId(), application.getId(), version.getVersion())); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deleteContract(io.apiman.manager.api.beans.contracts.ContractBean)
     */
    @Override
    public void deleteContract(ContractBean contract) throws StorageException {
        deleteEntity("contract", String.valueOf(contract.getId())); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deleteService(io.apiman.manager.api.beans.services.ServiceBean)
     */
    @Override
    public void deleteService(ServiceBean service) throws StorageException {
        deleteEntity("service", id(service.getOrganization().getId(), service.getId())); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deleteServiceVersion(io.apiman.manager.api.beans.services.ServiceVersionBean)
     */
    @Override
    public void deleteServiceVersion(ServiceVersionBean version) throws StorageException {
        ServiceBean service = version.getService();
        deleteEntity("serviceVersion", id(service.getOrganization().getId(), service.getId(), version.getVersion())); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deleteServiceDefinition(io.apiman.manager.api.beans.services.ServiceVersionBean)
     */
    @Override
    public void deleteServiceDefinition(ServiceVersionBean version) throws StorageException {
        String id = id(version.getService().getOrganization().getId(), version.getService().getId(), version.getVersion()) + ":def"; //$NON-NLS-1$
        deleteEntity("serviceDefinition", id); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deletePlan(io.apiman.manager.api.beans.plans.PlanBean)
     */
    @Override
    public void deletePlan(PlanBean plan) throws StorageException {
        deleteEntity("plan", id(plan.getOrganization().getId(), plan.getId())); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deletePlanVersion(io.apiman.manager.api.beans.plans.PlanVersionBean)
     */
    @Override
    public void deletePlanVersion(PlanVersionBean version) throws StorageException {
        PlanBean plan = version.getPlan();
        deleteEntity("planVersion", id(plan.getOrganization().getId(), plan.getId(), version.getVersion())); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deletePolicy(io.apiman.manager.api.beans.policies.PolicyBean)
     */
    @Override
    public void deletePolicy(PolicyBean policy) throws StorageException {
        String docType = getPoliciesDocType(policy.getType());
        String pid = id(policy.getOrganizationId(), policy.getEntityId(), policy.getEntityVersion());
        Map<String, Object> source = getEntity(docType, pid);
        if (source == null) {
            throw new StorageException("Policy not found."); //$NON-NLS-1$
        }
        PoliciesBean policies = EsMarshalling.unmarshallPolicies(source);
        if (policies == null) throw new StorageException("Policy not found."); //$NON-NLS-1$
        List<PolicyBean> policyBeans = policies.getPolicies();
        boolean found = false;
        if (policyBeans != null) {
            for (PolicyBean policyBean : policyBeans) {
                if (policyBean.getId().equals(policy.getId())) {
                    policies.getPolicies().remove(policyBean);
                    found = true;
                    break;
                }
            }
        }
        if (found) {
            updateEntity(docType, pid, EsMarshalling.marshall(policies));
        } else {
            throw new StorageException("Policy not found."); //$NON-NLS-1$
        }
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deleteGateway(io.apiman.manager.api.beans.gateways.GatewayBean)
     */
    @Override
    public void deleteGateway(GatewayBean gateway) throws StorageException {
        deleteEntity("gateway", gateway.getId()); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deletePlugin(io.apiman.manager.api.beans.plugins.PluginBean)
     */
    @Override
    public void deletePlugin(PluginBean plugin) throws StorageException {
        deleteEntity("plugin", String.valueOf(plugin.getId())); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deleteDownload(io.apiman.manager.api.beans.download.DownloadBean)
     */
    @Override
    public void deleteDownload(DownloadBean download) throws StorageException {
        deleteEntity("download", download.getId()); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deletePolicyDefinition(io.apiman.manager.api.beans.policies.PolicyDefinitionBean)
     */
    @Override
    public void deletePolicyDefinition(PolicyDefinitionBean policyDef) throws StorageException {
        deleteEntity("policyDef", policyDef.getId()); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see io.apiman.manager.api.core.IStorage#deleteRole(io.apiman.manager.api.beans.idm.RoleBean)
     */
    @Override
    public void deleteRole(RoleBean role) throws StorageException {
        deleteEntity("role", role.getId()); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getOrganization(java.lang.String)
     */
    @Override
    public OrganizationBean getOrganization(String id) throws StorageException {
        Map<String, Object> source = getEntity("organization", id); //$NON-NLS-1$
        return EsMarshalling.unmarshallOrganization(source);
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getApplication(java.lang.String, java.lang.String)
     */
    @Override
    public ApplicationBean getApplication(String organizationId, String id) throws StorageException {
        Map<String, Object> source = getEntity("application", id(organizationId, id)); //$NON-NLS-1$
        if (source == null) {
            return null;
        }
        ApplicationBean bean = EsMarshalling.unmarshallApplication(source);
        bean.setOrganization(getOrganization(organizationId));
        return bean;
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getApplicationVersion(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ApplicationVersionBean getApplicationVersion(String organizationId, String applicationId,
            String version) throws StorageException {
        Map<String, Object> source = getEntity("applicationVersion", id(organizationId, applicationId, version)); //$NON-NLS-1$
        if (source == null) {
            return null;
        }
        ApplicationVersionBean bean = EsMarshalling.unmarshallApplicationVersion(source);
        bean.setApplication(getApplication(organizationId, applicationId));
        return bean;
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getContract(java.lang.Long)
     */
    @SuppressWarnings("nls")
    @Override
    public ContractBean getContract(Long id) throws StorageException {
        Map<String, Object> source = getEntity("contract", String.valueOf(id)); //$NON-NLS-1$
        ContractBean contract = EsMarshalling.unmarshallContract(source);
        String appOrgId = (String) source.get("appOrganizationId");
        String appId = (String) source.get("appId");
        String appVersion = (String) source.get("appVersion");
        String svcOrgId = (String) source.get("serviceOrganizationId");
        String svcId = (String) source.get("serviceId");
        String svcVersion = (String) source.get("serviceVersion");
        String planId = (String) source.get("planId");
        String planVersion = (String) source.get("planVersion");
        ApplicationVersionBean avb = getApplicationVersion(appOrgId, appId, appVersion);
        ServiceVersionBean svb = getServiceVersion(svcOrgId, svcId, svcVersion);
        PlanVersionBean pvb = getPlanVersion(svcOrgId, planId, planVersion);
        contract.setApplication(avb);
        contract.setPlan(pvb);
        contract.setService(svb);
        return contract;
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getService(java.lang.String, java.lang.String)
     */
    @Override
    public ServiceBean getService(String organizationId, String id) throws StorageException {
        Map<String, Object> source = getEntity("service", id(organizationId, id)); //$NON-NLS-1$
        if (source == null) {
            return null;
        }
        ServiceBean bean = EsMarshalling.unmarshallService(source);
        bean.setOrganization(getOrganization(organizationId));
        return bean;
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getServiceVersion(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ServiceVersionBean getServiceVersion(String organizationId, String serviceId, String version)
            throws StorageException {
        Map<String, Object> source = getEntity("serviceVersion", id(organizationId, serviceId, version)); //$NON-NLS-1$
        if (source == null) {
            return null;
        }
        ServiceVersionBean bean = EsMarshalling.unmarshallServiceVersion(source);
        bean.setService(getService(organizationId, serviceId));
        return bean;
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getServiceDefinition(io.apiman.manager.api.beans.services.ServiceVersionBean)
     */
    @Override
    public InputStream getServiceDefinition(ServiceVersionBean version) throws StorageException {
        try {
            String id = id(version.getService().getOrganization().getId(), version.getService().getId(), version.getVersion()) + ":def"; //$NON-NLS-1$
            Map<String, Object> source = getEntity("serviceDefinition", id); //$NON-NLS-1$
            if (source == null) {
                return null;
            }
            ServiceDefinitionBean def = EsMarshalling.unmarshallServiceDefinition(source);
            if (def == null) return null;
            String data = def.getData();
            return new ByteArrayInputStream(Base64.decode(data));
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getPlan(java.lang.String, java.lang.String)
     */
    @Override
    public PlanBean getPlan(String organizationId, String id) throws StorageException {
        Map<String, Object> source = getEntity("plan", id(organizationId, id)); //$NON-NLS-1$
        if (source == null) {
            return null;
        }
        PlanBean bean = EsMarshalling.unmarshallPlan(source);
        bean.setOrganization(getOrganization(organizationId));
        return bean;
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getPlanVersion(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public PlanVersionBean getPlanVersion(String organizationId, String planId, String version)
            throws StorageException {
        Map<String, Object> source = getEntity("planVersion", id(organizationId, planId, version)); //$NON-NLS-1$
        if (source == null) {
            return null;
        }
        PlanVersionBean bean = EsMarshalling.unmarshallPlanVersion(source);
        bean.setPlan(getPlan(organizationId, planId));
        return bean;
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getPolicy(io.apiman.manager.api.beans.policies.PolicyType, java.lang.String, java.lang.String, java.lang.String, java.lang.Long)
     */
    @Override
    public PolicyBean getPolicy(PolicyType type, String organizationId, String entityId, String version,
            Long id) throws StorageException {
        String docType = getPoliciesDocType(type);
        String pid = id(organizationId, entityId, version);
        Map<String, Object> source = getEntity(docType, pid);
        if (source == null) {
            return null;
        }
        PoliciesBean policies = EsMarshalling.unmarshallPolicies(source);
        if (policies == null) return null;
        List<PolicyBean> policyBeans = policies.getPolicies();
        if (policyBeans != null) {
            for (PolicyBean policyBean : policyBeans) {
                if (policyBean.getId().equals(id)) {
                    PolicyDefinitionBean def = getPolicyDefinition(policyBean.getDefinition().getId());
                    policyBean.setDefinition(def);
                    return policyBean;
                }
            }
        }
        return null;
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getGateway(java.lang.String)
     */
    @Override
    public GatewayBean getGateway(String id) throws StorageException {
        Map<String, Object> source = getEntity("gateway", id); //$NON-NLS-1$
        return EsMarshalling.unmarshallGateway(source);
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getDownload(java.lang.String)
     */
    @Override
    public DownloadBean getDownload(String id) throws StorageException {
        Map<String, Object> source = getEntity("download", id); //$NON-NLS-1$
        return EsMarshalling.unmarshallDownload(source);
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getPlugin(long)
     */
    @Override
    public PluginBean getPlugin(long id) throws StorageException {
        Map<String, Object> source = getEntity("plugin", String.valueOf(id)); //$NON-NLS-1$
        return EsMarshalling.unmarshallPlugin(source);
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getPlugin(java.lang.String, java.lang.String)
     */
    @Override
    public PluginBean getPlugin(String groupId, String artifactId) throws StorageException {
        try {
            @SuppressWarnings("nls")
            QueryBuilder qb = QueryBuilders.filteredQuery(
                    QueryBuilders.matchAllQuery(),
                    FilterBuilders.andFilter(
                            FilterBuilders.termFilter("groupId", groupId),
                            FilterBuilders.termFilter("artifactId", artifactId)
                    )
                );
            SearchSourceBuilder builder = new SearchSourceBuilder().query(qb).size(2);

            SearchRequest request = new SearchRequest(INDEX_NAME);
            request.types("plugin"); //$NON-NLS-1$
            request.source(builder);
            List<Hit<Map<String,Object>,Void>> hits = listEntities("plugin", builder); //$NON-NLS-1$
            if (hits.size() == 1) {
                Hit<Map<String,Object>,Void> hit = hits.iterator().next();
                return EsMarshalling.unmarshallPlugin(hit.source);
            }
            return null;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getPolicyDefinition(java.lang.String)
     */
    @Override
    public PolicyDefinitionBean getPolicyDefinition(String id) throws StorageException {
        Map<String, Object> source = getEntity("policyDef", id); //$NON-NLS-1$
        return EsMarshalling.unmarshallPolicyDefinition(source);

    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getRole(java.lang.String)
     */
    @Override
    public RoleBean getRole(String id) throws StorageException {
        Map<String, Object> source = getEntity("role", id); //$NON-NLS-1$
        return EsMarshalling.unmarshallRole(source);
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#listPlugins()
     */
    @Override
    public List<PluginSummaryBean> listPlugins() throws StorageException {
        @SuppressWarnings("nls")
        String[] fields = {"id", "artifactId", "groupId", "version", "classifier", "type", "name",
            "description", "createdBy", "createdOn"};

        @SuppressWarnings("nls")
        QueryBuilder query = QueryBuilders.filteredQuery(
            QueryBuilders.matchAllQuery(),
            FilterBuilders.orFilter(
                    FilterBuilders.missingFilter("deleted"),
                    FilterBuilders.termFilter("deleted", false))
        );
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .fetchSource(fields, null).query(query).sort("name.raw", SortOrder.ASC).size(200); //$NON-NLS-1$
        List<Hit<Map<String,Object>,Void>> hits = listEntities("plugin", builder); //$NON-NLS-1$
        List<PluginSummaryBean> rval = new ArrayList<>(hits.size());
        for (Hit<Map<String,Object>,Void> hit : hits) {
            PluginSummaryBean bean = EsMarshalling.unmarshallPluginSummary(hit.source);
            rval.add(bean);
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#listGateways()
     */
    @Override
    public List<GatewaySummaryBean> listGateways() throws StorageException {
        @SuppressWarnings("nls")
        String[] fields = {"id", "name", "description","type"};
        SearchSourceBuilder builder = new SearchSourceBuilder().fetchSource(fields, null).sort("name.raw", SortOrder.ASC).size(100); //$NON-NLS-1$
        List<Hit<Map<String,Object>,Void>> hits = listEntities("gateway", builder); //$NON-NLS-1$
        List<GatewaySummaryBean> rval = new ArrayList<>(hits.size());
        for (Hit<Map<String,Object>,Void> hit : hits) {
            GatewaySummaryBean bean = EsMarshalling.unmarshallGatewaySummary(hit.source);
            rval.add(bean);
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#findOrganizations(io.apiman.manager.api.beans.search.SearchCriteriaBean)
     */
    @Override
    public SearchResultsBean<OrganizationSummaryBean> findOrganizations(SearchCriteriaBean criteria)
            throws StorageException {
        return find(criteria, "organization", new IUnmarshaller<OrganizationSummaryBean>() { //$NON-NLS-1$
            @Override
            public OrganizationSummaryBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallOrganizationSummary(source);
            }
        });
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#findApplications(io.apiman.manager.api.beans.search.SearchCriteriaBean)
     */
    @Override
    public SearchResultsBean<ApplicationSummaryBean> findApplications(SearchCriteriaBean criteria)
            throws StorageException {
        return find(criteria, "application", new IUnmarshaller<ApplicationSummaryBean>() { //$NON-NLS-1$
            @Override
            public ApplicationSummaryBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallApplicationSummary(source);
            }
        });
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#findServices(io.apiman.manager.api.beans.search.SearchCriteriaBean)
     */
    @Override
    public SearchResultsBean<ServiceSummaryBean> findServices(SearchCriteriaBean criteria)
            throws StorageException {
        return find(criteria, "service", new IUnmarshaller<ServiceSummaryBean>() { //$NON-NLS-1$
            @Override
            public ServiceSummaryBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallServiceSummary(source);
            }
        });
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#findPlans(java.lang.String, io.apiman.manager.api.beans.search.SearchCriteriaBean)
     */
    @Override
    public SearchResultsBean<PlanSummaryBean> findPlans(String organizationId, SearchCriteriaBean criteria)
            throws StorageException {
        criteria.addFilter("organizationId", organizationId, SearchCriteriaFilterOperator.eq); //$NON-NLS-1$
        return find(criteria, "plan", new IUnmarshaller<PlanSummaryBean>() { //$NON-NLS-1$
            @Override
            public PlanSummaryBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallPlanSummary(source);
            }
        });
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#auditEntity(java.lang.String, java.lang.String, java.lang.String, java.lang.Class, io.apiman.manager.api.beans.search.PagingBean)
     */
    @Override
    public <T> SearchResultsBean<AuditEntryBean> auditEntity(String organizationId, String entityId,
            String entityVersion, Class<T> type, PagingBean paging) throws StorageException {
        SearchCriteriaBean criteria = new SearchCriteriaBean();
        if (paging != null) {
            criteria.setPaging(paging);
        } else {
            criteria.setPage(1);
            criteria.setPageSize(20);
        }
        criteria.setOrder("createdOn", false); //$NON-NLS-1$
        if (organizationId != null) {
            criteria.addFilter("organizationId", organizationId, SearchCriteriaFilterOperator.eq); //$NON-NLS-1$
        }
        if (entityId != null) {
            criteria.addFilter("entityId", entityId, SearchCriteriaFilterOperator.eq); //$NON-NLS-1$
        }
        if (entityVersion != null) {
            criteria.addFilter("entityVersion", entityVersion, SearchCriteriaFilterOperator.eq); //$NON-NLS-1$
        }
        if (type != null) {
            AuditEntityType entityType = null;
            if (type == OrganizationBean.class) {
                entityType = AuditEntityType.Organization;
            } else if (type == ApplicationBean.class) {
                entityType = AuditEntityType.Application;
            } else if (type == ServiceBean.class) {
                entityType = AuditEntityType.Service;
            } else if (type == PlanBean.class) {
                entityType = AuditEntityType.Plan;
            }
            if (entityType != null) {
                criteria.addFilter("entityType", entityType.name(), SearchCriteriaFilterOperator.eq); //$NON-NLS-1$
            }
        }

        return find(criteria, "auditEntry", new IUnmarshaller<AuditEntryBean>() { //$NON-NLS-1$
            @Override
            public AuditEntryBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallAuditEntry(source);
            }
        });
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#auditUser(java.lang.String, io.apiman.manager.api.beans.search.PagingBean)
     */
    @Override
    public <T> SearchResultsBean<AuditEntryBean> auditUser(String userId, PagingBean paging)
            throws StorageException {
        SearchCriteriaBean criteria = new SearchCriteriaBean();
        if (paging != null) {
            criteria.setPaging(paging);
        } else {
            criteria.setPage(1);
            criteria.setPageSize(20);
        }
        criteria.setOrder("createdOn", false); //$NON-NLS-1$
        if (userId != null) {
            criteria.addFilter("who", userId, SearchCriteriaFilterOperator.eq); //$NON-NLS-1$
        }

        return find(criteria, "auditEntry", new IUnmarshaller<AuditEntryBean>() { //$NON-NLS-1$
            @Override
            public AuditEntryBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallAuditEntry(source);
            }
        });
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getOrgs(java.util.Set)
     */
    @Override
    public List<OrganizationSummaryBean> getOrgs(Set<String> organizationIds) throws StorageException {
        List<OrganizationSummaryBean> orgs = new ArrayList<>();
        if (organizationIds == null || organizationIds.isEmpty()) {
            return orgs;
        }
        @SuppressWarnings("nls")
        QueryBuilder query = QueryBuilders.filteredQuery(
            QueryBuilders.matchAllQuery(),
            FilterBuilders.termsFilter("id", organizationIds.toArray())
        );
        @SuppressWarnings("nls")
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .sort("name.raw", SortOrder.ASC)
                .query(query)
                .size(500);
        List<Hit<Map<String,Object>,Void>> hits = listEntities("organization", builder); //$NON-NLS-1$
        List<OrganizationSummaryBean> rval = new ArrayList<>(hits.size());
        for (Hit<Map<String,Object>,Void> hit : hits) {
            OrganizationSummaryBean bean = EsMarshalling.unmarshallOrganizationSummary(hit.source);
            rval.add(bean);
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getApplicationsInOrgs(java.util.Set)
     */
    @Override
    public List<ApplicationSummaryBean> getApplicationsInOrgs(Set<String> organizationIds) throws StorageException {
        @SuppressWarnings("nls")
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .sort("organizationName.raw", SortOrder.ASC)
                .sort("name.raw", SortOrder.ASC)
                .size(500);
        TermsQueryBuilder query = QueryBuilders.termsQuery("organizationId", organizationIds.toArray(new String[organizationIds.size()])); //$NON-NLS-1$
        builder.query(query);
        List<Hit<Map<String,Object>,Void>> hits = listEntities("application", builder); //$NON-NLS-1$
        List<ApplicationSummaryBean> rval = new ArrayList<>(hits.size());
        for (Hit<Map<String,Object>,Void> hit : hits) {
            ApplicationSummaryBean bean = EsMarshalling.unmarshallApplicationSummary(hit.source);
            rval.add(bean);
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getApplicationsInOrg(java.lang.String)
     */
    @Override
    public List<ApplicationSummaryBean> getApplicationsInOrg(String organizationId) throws StorageException {
        Set<String> orgs = new HashSet<>();
        orgs.add(organizationId);
        return getApplicationsInOrgs(orgs);
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getApplicationVersions(java.lang.String, java.lang.String)
     */
    @Override
    public List<ApplicationVersionSummaryBean> getApplicationVersions(String organizationId,
            String applicationId) throws StorageException {
        @SuppressWarnings("nls")
        QueryBuilder query = QueryBuilders.filteredQuery(
            QueryBuilders.matchAllQuery(),
            FilterBuilders.andFilter(
                    FilterBuilders.termFilter("organizationId", organizationId),
                    FilterBuilders.termFilter("applicationId", applicationId))
        );
        @SuppressWarnings("nls")
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .sort("createdOn", SortOrder.DESC)
                .query(query)
                .size(500);
        List<Hit<Map<String,Object>,Void>> hits = listEntities("applicationVersion", builder); //$NON-NLS-1$
        List<ApplicationVersionSummaryBean> rval = new ArrayList<>(hits.size());
        for (Hit<Map<String,Object>,Void> hit : hits) {
            ApplicationVersionSummaryBean bean = EsMarshalling.unmarshallApplicationVersionSummary(hit.source);
            rval.add(bean);
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getApplicationContracts(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public List<ContractSummaryBean> getApplicationContracts(String organizationId, String applicationId,
            String version) throws StorageException {
        @SuppressWarnings("nls")
        QueryBuilder query = QueryBuilders.filteredQuery(
            QueryBuilders.matchAllQuery(),
            FilterBuilders.andFilter(
                FilterBuilders.termFilter("appOrganizationId", organizationId),
                FilterBuilders.termFilter("appId", applicationId),
                FilterBuilders.termFilter("appVersion", version)
            )
        );
        @SuppressWarnings("nls")
        SearchSourceBuilder builder = new SearchSourceBuilder().sort("serviceOrganizationId", SortOrder.ASC)
                .sort("serviceId", SortOrder.ASC).query(query).size(500);
        List<Hit<Map<String,Object>,Void>> hits = listEntities("contract", builder); //$NON-NLS-1$
        List<ContractSummaryBean> rval = new ArrayList<>(hits.size());
        for (Hit<Map<String,Object>,Void> hit : hits) {
            ContractSummaryBean bean = EsMarshalling.unmarshallContractSummary(hit.source);
            rval.add(bean);
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getApiRegistry(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ApiRegistryBean getApiRegistry(String organizationId, String applicationId, String version)
            throws StorageException {
        @SuppressWarnings("nls")
        QueryBuilder query = QueryBuilders.filteredQuery(
            QueryBuilders.matchAllQuery(),
            FilterBuilders.andFilter(
                FilterBuilders.termFilter("appOrganizationId", organizationId),
                FilterBuilders.termFilter("appId", applicationId),
                FilterBuilders.termFilter("appVersion", version)
            )
        );
        @SuppressWarnings("nls")
        SearchSourceBuilder builder = new SearchSourceBuilder().sort("id", SortOrder.ASC).query(query)
                .size(500);
        List<Hit<Map<String,Object>,Void>> hits = listEntities("contract", builder); //$NON-NLS-1$
        ApiRegistryBean registry = new ApiRegistryBean();
        for (Hit<Map<String,Object>,Void> hit : hits) {
            ApiEntryBean bean = EsMarshalling.unmarshallApiEntry(hit.source);
            ServiceVersionBean svb = getServiceVersion(bean.getServiceOrgId(), bean.getServiceId(), bean.getServiceVersion());
            Set<ServiceGatewayBean> gateways = svb.getGateways();
            if (gateways != null && gateways.size() > 0) {
                ServiceGatewayBean sgb = gateways.iterator().next();
                bean.setGatewayId(sgb.getGatewayId());
            }
            registry.getApis().add(bean);
        }
        return registry;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getServicesInOrgs(java.util.Set)
     */
    @Override
    public List<ServiceSummaryBean> getServicesInOrgs(Set<String> organizationIds) throws StorageException {
        @SuppressWarnings("nls")
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .sort("organizationName.raw", SortOrder.ASC)
                .sort("name.raw", SortOrder.ASC)
                .size(500);
        TermsQueryBuilder query = QueryBuilders.termsQuery("organizationId", organizationIds.toArray(new String[organizationIds.size()])); //$NON-NLS-1$
        builder.query(query);

        List<Hit<Map<String,Object>,Void>> hits = listEntities("service", builder); //$NON-NLS-1$
        List<ServiceSummaryBean> rval = new ArrayList<>(hits.size());
        for (Hit<Map<String,Object>,Void> hit : hits) {
            ServiceSummaryBean bean = EsMarshalling.unmarshallServiceSummary(hit.source);
            rval.add(bean);
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getServicesInOrg(java.lang.String)
     */
    @Override
    public List<ServiceSummaryBean> getServicesInOrg(String organizationId) throws StorageException {
        Set<String> orgs = new HashSet<>();
        orgs.add(organizationId);
        return getServicesInOrgs(orgs);
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getServiceVersions(java.lang.String, java.lang.String)
     */
    @Override
    public List<ServiceVersionSummaryBean> getServiceVersions(String organizationId, String serviceId)
            throws StorageException {
        @SuppressWarnings("nls")
        QueryBuilder query = QueryBuilders.filteredQuery(
            QueryBuilders.matchAllQuery(),
            FilterBuilders.andFilter(
                    FilterBuilders.termFilter("organizationId", organizationId),
                    FilterBuilders.termFilter("serviceId", serviceId))
        );
        @SuppressWarnings("nls")
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .sort("createdOn", SortOrder.DESC)
                .query(query)
                .size(500);
        List<Hit<Map<String,Object>,Void>> hits = listEntities("serviceVersion", builder); //$NON-NLS-1$
        List<ServiceVersionSummaryBean> rval = new ArrayList<>(hits.size());
        for (Hit<Map<String,Object>,Void> hit : hits) {
            ServiceVersionSummaryBean bean = EsMarshalling.unmarshallServiceVersionSummary(hit.source);
            rval.add(bean);
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getServiceVersionPlans(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public List<ServicePlanSummaryBean> getServiceVersionPlans(String organizationId, String serviceId,
            String version) throws StorageException {
        List<ServicePlanSummaryBean> rval = new ArrayList<>();
        ServiceVersionBean versionBean = getServiceVersion(organizationId, serviceId, version);
        if (versionBean != null) {
            Set<ServicePlanBean> plans = versionBean.getPlans();
            if (plans != null) {
                for (ServicePlanBean spb : plans) {
                    PlanBean planBean = getPlan(organizationId, spb.getPlanId());
                    ServicePlanSummaryBean plan = new ServicePlanSummaryBean();
                    plan.setPlanId(spb.getPlanId());
                    plan.setVersion(spb.getVersion());
                    plan.setPlanName(planBean.getName());
                    plan.setPlanDescription(planBean.getDescription());
                    rval.add(plan);
                }
            }
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getPlansInOrgs(java.util.Set)
     */
    @Override
    public List<PlanSummaryBean> getPlansInOrgs(Set<String> organizationIds) throws StorageException {
        @SuppressWarnings("nls")
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .sort("organizationName.raw", SortOrder.ASC)
                .sort("name.raw", SortOrder.ASC)
                .size(500);
        TermsQueryBuilder query = QueryBuilders.termsQuery("organizationId", organizationIds.toArray(new String[organizationIds.size()])); //$NON-NLS-1$
        builder.query(query);
        List<Hit<Map<String,Object>,Void>> hits = listEntities("plan", builder); //$NON-NLS-1$
        List<PlanSummaryBean> rval = new ArrayList<>(hits.size());
        for (Hit<Map<String,Object>,Void> hit : hits) {
            PlanSummaryBean bean = EsMarshalling.unmarshallPlanSummary(hit.source);
            rval.add(bean);
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getPlansInOrg(java.lang.String)
     */
    @Override
    public List<PlanSummaryBean> getPlansInOrg(String organizationId) throws StorageException {
        Set<String> orgs = new HashSet<>();
        orgs.add(organizationId);
        return getPlansInOrgs(orgs);
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getPlanVersions(java.lang.String, java.lang.String)
     */
    @Override
    public List<PlanVersionSummaryBean> getPlanVersions(String organizationId, String planId)
            throws StorageException {
        @SuppressWarnings("nls")
        QueryBuilder query = QueryBuilders.filteredQuery(
            QueryBuilders.matchAllQuery(),
            FilterBuilders.andFilter(
                    FilterBuilders.termFilter("organizationId", organizationId),
                    FilterBuilders.termFilter("planId", planId))
        );
        @SuppressWarnings("nls")
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .sort("createdOn", SortOrder.DESC)
                .query(query)
                .size(500);
        List<Hit<Map<String,Object>,Void>> hits = listEntities("planVersion", builder); //$NON-NLS-1$
        List<PlanVersionSummaryBean> rval = new ArrayList<>(hits.size());
        for (Hit<Map<String,Object>,Void> hit : hits) {
            PlanVersionSummaryBean bean = EsMarshalling.unmarshallPlanVersionSummary(hit.source);
            rval.add(bean);
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getPolicies(java.lang.String, java.lang.String, java.lang.String, io.apiman.manager.api.beans.policies.PolicyType)
     */
    @Override
    public List<PolicySummaryBean> getPolicies(String organizationId, String entityId, String version,
            PolicyType type) throws StorageException {
        try {
            String docType = getPoliciesDocType(type);
            String pid = id(organizationId, entityId, version);
            List<PolicySummaryBean> rval = new ArrayList<>();
            Map<String, Object> source = getEntity(docType, pid);
            if (source == null) {
                return rval;
            }
            PoliciesBean policies = EsMarshalling.unmarshallPolicies(source);
            if (policies == null) return rval;
            List<PolicyBean> policyBeans = policies.getPolicies();
            if (policyBeans != null) {
                for (PolicyBean policyBean : policyBeans) {
                    PolicyDefinitionBean def = getPolicyDefinition(policyBean.getDefinition().getId());
                    policyBean.setDefinition(def);
                    PolicyTemplateUtil.generatePolicyDescription(policyBean);
                    PolicySummaryBean psb = new PolicySummaryBean();
                    psb.setCreatedBy(policyBean.getCreatedBy());
                    psb.setCreatedOn(policyBean.getCreatedOn());
                    psb.setDescription(policyBean.getDescription());
                    psb.setIcon(def.getIcon());
                    psb.setId(policyBean.getId());
                    psb.setName(policyBean.getName());
                    psb.setPolicyDefinitionId(def.getId());
                    rval.add(psb);
                }
            }
            return rval;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#listPolicyDefinitions()
     */
    @Override
    public List<PolicyDefinitionSummaryBean> listPolicyDefinitions() throws StorageException {
        @SuppressWarnings("nls")
        String[] fields = {"id", "policyImpl", "name", "description", "icon", "pluginId", "formType"};
        @SuppressWarnings("nls")
        QueryBuilder query = QueryBuilders.filteredQuery(
            QueryBuilders.matchAllQuery(),
            FilterBuilders.orFilter(
                    FilterBuilders.missingFilter("deleted"),
                    FilterBuilders.termFilter("deleted", false))
        );
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .fetchSource(fields, null)
                .query(query)
                .sort("name.raw", SortOrder.ASC).size(100); //$NON-NLS-1$
        List<Hit<Map<String,Object>,Void>> hits = listEntities("policyDef", builder); //$NON-NLS-1$
        List<PolicyDefinitionSummaryBean> rval = new ArrayList<>(hits.size());
        for (Hit<Map<String,Object>,Void> hit : hits) {
            PolicyDefinitionSummaryBean bean = EsMarshalling.unmarshallPolicyDefinitionSummary(hit.source);
            rval.add(bean);
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getServiceContracts(java.lang.String, java.lang.String, java.lang.String, int, int)
     */
    @Override
    public List<ContractSummaryBean> getServiceContracts(String organizationId, String serviceId,
            String version, int page, int pageSize) throws StorageException {
        @SuppressWarnings("nls")
        QueryBuilder query = QueryBuilders.filteredQuery(
            QueryBuilders.matchAllQuery(),
            FilterBuilders.andFilter(
                FilterBuilders.termFilter("serviceOrganizationId", organizationId),
                FilterBuilders.termFilter("serviceId", serviceId),
                FilterBuilders.termFilter("serviceVersion", version)
            )
        );
        @SuppressWarnings("nls")
        SearchSourceBuilder builder = new SearchSourceBuilder().sort("appOrganizationId", SortOrder.ASC)
                .sort("appId", SortOrder.ASC).query(query).size(500);
        List<Hit<Map<String,Object>,Void>> hits = listEntities("contract", builder); //$NON-NLS-1$
        List<ContractSummaryBean> rval = new ArrayList<>(hits.size());
        for (Hit<Map<String,Object>,Void> hit : hits) {
            ContractSummaryBean bean = EsMarshalling.unmarshallContractSummary(hit.source);
            rval.add(bean);
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getMaxPolicyOrderIndex(java.lang.String, java.lang.String, java.lang.String, io.apiman.manager.api.beans.policies.PolicyType)
     */
    @Override
    public int getMaxPolicyOrderIndex(String organizationId, String entityId, String entityVersion,
            PolicyType type) throws StorageException {
        // We'll figure this out later, when adding a policy.
        return -1;
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#listPluginPolicyDefs(java.lang.Long)
     */
    @Override
    public List<PolicyDefinitionSummaryBean> listPluginPolicyDefs(Long pluginId) throws StorageException {
        @SuppressWarnings("nls")
        QueryBuilder qb = QueryBuilders.filteredQuery(
                QueryBuilders.matchAllQuery(),
                FilterBuilders.termFilter("pluginId", pluginId)
            );
        @SuppressWarnings("nls")
        String[] fields = {"id", "policyImpl", "name", "description", "icon", "pluginId", "formType"};
        SearchSourceBuilder builder = new SearchSourceBuilder()
                .fetchSource(fields, null)
                .query(qb)
                .sort("name.raw", SortOrder.ASC).size(100); //$NON-NLS-1$
        List<Hit<Map<String,Object>,Void>> hits = listEntities("policyDef", builder); //$NON-NLS-1$
        List<PolicyDefinitionSummaryBean> rval = new ArrayList<>(hits.size());
        for (Hit<Map<String,Object>,Void> hit : hits) {
            PolicyDefinitionSummaryBean bean = EsMarshalling.unmarshallPolicyDefinitionSummary(hit.source);
            rval.add(bean);
        }
        return rval;
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createUser(io.apiman.manager.api.beans.idm.UserBean)
     */
    @Override
    public void createUser(UserBean user) throws StorageException {
        indexEntity("user", user.getUsername(), EsMarshalling.marshall(user)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getUser(java.lang.String)
     */
    @Override
    public UserBean getUser(String userId) throws StorageException {
        Map<String, Object> source = getEntity("user", userId); //$NON-NLS-1$
        return EsMarshalling.unmarshallUser(source);
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#updateUser(io.apiman.manager.api.beans.idm.UserBean)
     */
    @Override
    public void updateUser(UserBean user) throws StorageException {
        updateEntity("user", user.getUsername(), EsMarshalling.marshall(user)); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#findUsers(io.apiman.manager.api.beans.search.SearchCriteriaBean)
     */
    @Override
    public SearchResultsBean<UserBean> findUsers(SearchCriteriaBean criteria) throws StorageException {
        return find(criteria, "user",  new IUnmarshaller<UserBean>() { //$NON-NLS-1$
            @Override
            public UserBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallUser(source);
            }
        });
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#findRoles(io.apiman.manager.api.beans.search.SearchCriteriaBean)
     */
    @Override
    public SearchResultsBean<RoleBean> findRoles(SearchCriteriaBean criteria) throws StorageException {
        return find(criteria, "role", new IUnmarshaller<RoleBean>() { //$NON-NLS-1$
            @Override
            public RoleBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallRole(source);
            }
        });
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#createMembership(io.apiman.manager.api.beans.idm.RoleMembershipBean)
     */
    @Override
    public void createMembership(RoleMembershipBean membership) throws StorageException {
        membership.setId(generateGuid());
        String id = id(membership.getOrganizationId(), membership.getUserId(), membership.getRoleId());
        indexEntity("roleMembership", id, EsMarshalling.marshall(membership), true); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getMembership(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public RoleMembershipBean getMembership(String userId, String roleId, String organizationId) throws StorageException {
        String id = id(organizationId, userId, roleId);
        Map<String, Object> source = getEntity("roleMembership", id); //$NON-NLS-1$
        if (source == null) {
            return null;
        } else {
            return EsMarshalling.unmarshallRoleMembership(source);
        }
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deleteMembership(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void deleteMembership(String userId, String roleId, String organizationId) throws StorageException {
        String id = id(organizationId, userId, roleId);
        deleteEntity("roleMembership", id); //$NON-NLS-1$
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#deleteMemberships(java.lang.String, java.lang.String)
     */
    @Override
    @SuppressWarnings("nls")
    public void deleteMemberships(String userId, String organizationId) throws StorageException {
        FilteredQueryBuilder query = QueryBuilders.filteredQuery(
                QueryBuilders.matchAllQuery(),
                FilterBuilders.andFilter(
                        FilterBuilders.termFilter("organizationId", organizationId),
                        FilterBuilders.termFilter("userId", userId))
        );
        String string = query.toString();
        // Workaround for bug in FilteredQueryBuilder which does not (yet) wrap
        // the JSON in a query element
        if (string.indexOf("query") < 0 || string.indexOf("query") > 7) {
            string = "{ \"query\" : " + string + "}";
        }
        DeleteByQuery deleteByQuery = new DeleteByQuery.Builder(string).addIndex(INDEX_NAME)
                .addType("roleMembership").build();
        try {
            JestResult response = esClient.execute(deleteByQuery);
            if (!response.isSucceeded()) {
                throw new StorageException(response.getErrorMessage());
            }
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getUserMemberships(java.lang.String)
     */
    @Override
    public Set<RoleMembershipBean> getUserMemberships(String userId) throws StorageException {
        try {
            @SuppressWarnings("nls")
            QueryBuilder qb = QueryBuilders.filteredQuery(
                QueryBuilders.matchAllQuery(),
                FilterBuilders.termFilter("userId", userId)
            );
            SearchSourceBuilder builder = new SearchSourceBuilder().query(qb).size(500);
            List<Hit<Map<String,Object>,Void>> hits = listEntities("roleMembership", builder); //$NON-NLS-1$
            Set<RoleMembershipBean> rval = new HashSet<>();
            for (Hit<Map<String,Object>,Void> hit : hits) {
                RoleMembershipBean roleMembership = EsMarshalling.unmarshallRoleMembership(hit.source);
                rval.add(roleMembership);
            }
            return rval;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getUserMemberships(java.lang.String, java.lang.String)
     */
    @Override
    public Set<RoleMembershipBean> getUserMemberships(String userId, String organizationId)
            throws StorageException {
        try {
            @SuppressWarnings("nls")
            QueryBuilder qb = QueryBuilders.filteredQuery(
                QueryBuilders.matchAllQuery(),
                FilterBuilders.andFilter(
                    FilterBuilders.termFilter("userId", userId),
                    FilterBuilders.termFilter("organizationId", organizationId)
                )
            );
            SearchSourceBuilder builder = new SearchSourceBuilder().query(qb).size(500);
            List<Hit<Map<String,Object>,Void>> hits = listEntities("roleMembership", builder); //$NON-NLS-1$
            Set<RoleMembershipBean> rval = new HashSet<>();
            for (Hit<Map<String,Object>,Void> hit : hits) {
                RoleMembershipBean roleMembership = EsMarshalling.unmarshallRoleMembership(hit.source);
                rval.add(roleMembership);
            }
            return rval;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getOrgMemberships(java.lang.String)
     */
    @Override
    public Set<RoleMembershipBean> getOrgMemberships(String organizationId) throws StorageException {
        try {
            @SuppressWarnings("nls")
            QueryBuilder qb = QueryBuilders.filteredQuery(
                QueryBuilders.matchAllQuery(),
                FilterBuilders.termFilter("organizationId", organizationId)
            );
            SearchSourceBuilder builder = new SearchSourceBuilder().query(qb).size(500);
            List<Hit<Map<String,Object>,Void>> hits = listEntities("roleMembership", builder); //$NON-NLS-1$
            Set<RoleMembershipBean> rval = new HashSet<>();
            for (Hit<Map<String,Object>,Void> hit : hits) {
                RoleMembershipBean roleMembership = EsMarshalling.unmarshallRoleMembership(hit.source);
                rval.add(roleMembership);
            }
            return rval;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * @see io.apiman.manager.api.core.IStorageQuery#getPermissions(java.lang.String)
     */
    @Override
    public Set<PermissionBean> getPermissions(String userId) throws StorageException {
        try {
            @SuppressWarnings("nls")
            QueryBuilder qb = QueryBuilders.filteredQuery(
                    QueryBuilders.matchAllQuery(),
                    FilterBuilders.termFilter("userId", userId)
                );
            SearchSourceBuilder builder = new SearchSourceBuilder().query(qb).size(500);
            List<Hit<Map<String,Object>,Void>> hits = listEntities("roleMembership", builder); //$NON-NLS-1$
            Set<PermissionBean> rval = new HashSet<>(hits.size());
            if (hits.size() > 0) {
                for (Hit<Map<String,Object>,Void> hit : hits) {
                    Map<String, Object> source = hit.source;
                    String roleId = String.valueOf(source.get("roleId")); //$NON-NLS-1$
                    String qualifier = String.valueOf(source.get("organizationId")); //$NON-NLS-1$
                    RoleBean role = getRole(roleId);
                    if (role != null) {
                        for (PermissionType permission : role.getPermissions()) {
                            PermissionBean p = new PermissionBean();
                            p.setName(permission);
                            p.setOrganizationId(qualifier);
                            rval.add(p);
                        }
                    }
                }
            }
            return rval;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * Indexes an entity.
     * @param type
     * @param id
     * @param entitySource
     * @throws StorageException
     */
    private void indexEntity(String type, String id, XContentBuilder sourceEntity) throws StorageException {
        indexEntity(type, id, sourceEntity, false);
    }

    /**
     * Indexes an entity.
     * @param type
     * @param id
     * @param entitySource
     * @param refresh true if the operation should wait for a refresh before it returns
     * @throws StorageException
     */
    @SuppressWarnings("nls")
    private void indexEntity(String type, String id, XContentBuilder sourceEntity, boolean refresh)
            throws StorageException {
        try {
            String json = sourceEntity.string();
            JestResult response = esClient.execute(new Index.Builder(json).refresh(refresh).index(INDEX_NAME)
                    .setParameter(Parameters.OP_TYPE, "create").type(type).id(id).build());
            if (!response.isSucceeded()) {
                throw new StorageException("Failed to index document " + id + " of type " + type + ": " + response.getErrorMessage());
            }
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * Gets an entity.  Callers must unmarshal the resulting map.
     * @param type
     * @param id
     * @throws StorageException
     */
    private Map<String, Object> getEntity(String type, String id) throws StorageException {
        try {
            JestResult response = esClient.execute(new Get.Builder(INDEX_NAME, id).type(type).build());
            if (!response.isSucceeded()) {
                return null;
            }
            return response.getSourceAsObject(Map.class);
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * Returns a list of entities.
     * @param type
     * @param searchSourceBuilder
     * @throws StorageException
     */
    private List<Hit<Map<String, Object>, Void>> listEntities(String type,
            SearchSourceBuilder searchSourceBuilder) throws StorageException {
        try {
            String query = searchSourceBuilder.toString();
            Search search = new Search.Builder(query).addIndex(INDEX_NAME).addType(type).build();
            SearchResult response = esClient.execute(search);
            @SuppressWarnings({ "rawtypes", "unchecked" })
            List<Hit<Map<String, Object>, Void>> thehits = (List) response.getHits(Map.class);
            return thehits;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * Deletes an entity.
     * @param type
     * @param id
     * @throws StorageException
     */
    private void deleteEntity(String type, String id) throws StorageException {
        try {
            JestResult response = esClient.execute(new Delete.Builder(id).index(INDEX_NAME).type(type).build());
            if (!response.isSucceeded()) {
                throw new StorageException("Document could not be deleted because it did not exist:" + response.getErrorMessage()); //$NON-NLS-1$
            }
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * Updates a single entity.
     * @param type
     * @param id
     * @param source
     * @throws StorageException
     */
    private void updateEntity(String type, String id, XContentBuilder source) throws StorageException {
        try {
            String doc = source.string();
            /* JestResult response = */esClient.execute(new Index.Builder(doc)
                    .setParameter(Parameters.OP_TYPE, "index").index(INDEX_NAME).type(type).id(id).build()); //$NON-NLS-1$
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * Finds entities using a generic search criteria bean.
     * @param criteria
     * @param type
     * @param unmarshaller
     * @throws StorageException
     */
    private <T> SearchResultsBean<T> find(SearchCriteriaBean criteria, String type,
            IUnmarshaller<T> unmarshaller) throws StorageException {
        try {
            SearchResultsBean<T> rval = new SearchResultsBean<>();

            // Set some default in the case that paging information was not included in the request.
            PagingBean paging = criteria.getPaging();
            if (paging == null) {
                paging = new PagingBean();
                paging.setPage(1);
                paging.setPageSize(20);
            }
            int page = paging.getPage();
            int pageSize = paging.getPageSize();
            int start = (page - 1) * pageSize;

            SearchSourceBuilder builder = new SearchSourceBuilder().size(pageSize).from(start).fetchSource(true);

            // Sort order
            OrderByBean orderBy = criteria.getOrderBy();
            if (orderBy != null) {
                String name = orderBy.getName();
                if (name.equals("name") || name.equals("fullName")) { //$NON-NLS-1$ //$NON-NLS-2$
                    name += ".raw"; //$NON-NLS-1$
                }
                if (orderBy.isAscending()) {
                    builder.sort(name, SortOrder.ASC);
                } else {
                    builder.sort(name, SortOrder.DESC);
                }
            }

            // Now process the filter criteria
            List<SearchCriteriaFilterBean> filters = criteria.getFilters();
            BaseQueryBuilder q = QueryBuilders.matchAllQuery();
            if (filters != null && !filters.isEmpty()) {

                AndFilterBuilder andFilter = FilterBuilders.andFilter();
                int filterCount = 0;
                for (SearchCriteriaFilterBean filter : filters) {
                    if (filter.getOperator() == SearchCriteriaFilterOperator.eq) {
                        andFilter.add(FilterBuilders.termFilter(filter.getName(), filter.getValue()));
                        filterCount++;
                    } else if (filter.getOperator() == SearchCriteriaFilterOperator.like) {
                        q = QueryBuilders.wildcardQuery(filter.getName(), filter.getValue().toLowerCase().replace('%', '*'));
                    } else if (filter.getOperator() == SearchCriteriaFilterOperator.bool_eq) {
                        andFilter.add(FilterBuilders.termFilter(filter.getName(), "true".equals(filter.getValue()))); //$NON-NLS-1$
                        filterCount++;
                    }
                    // TODO implement the other filter operators here!
                }

                if (filterCount > 0) {
                    q = QueryBuilders.filteredQuery(q, andFilter);
                }
            }
            builder.query(q);


            String query = builder.toString();
            Search search = new Search.Builder(query).addIndex(INDEX_NAME)
                    .addType(type).build();
            SearchResult response = esClient.execute(search);
            @SuppressWarnings({ "unchecked", "rawtypes" })
            List<Hit<Map<String, Object>, Void>> thehits = (List) response.getHits(Map.class);

            rval.setTotalSize(response.getTotal());
            for (Hit<Map<String,Object>,Void> hit : thehits) {
                Map<String, Object> sourceAsMap = hit.source;
                T bean = unmarshaller.unmarshal(sourceAsMap);
                rval.getBeans().add(bean);
            }
            return rval;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * Generates a (hopefully) unique ID.  Mimics JPA's auto-generated long ID column.
     */
    private static synchronized Long generateGuid() {
        StringBuilder builder = new StringBuilder();
        builder.append(System.currentTimeMillis());
        builder.append(guidCounter++);
        // Reset the counter if it gets too high.  It's always a number
        // between 100 and 999 so that the # of digits in the guid is
        // always the same.
        if (guidCounter > 999) {
            guidCounter = 100;
        }
        return Long.parseLong(builder.toString());
    }

    /**
     * Returns the policies document type to use given the policy type.
     * @param type
     */
    private static String getPoliciesDocType(PolicyType type) {
        String docType = "planPolicies"; //$NON-NLS-1$
        if (type == PolicyType.Service) {
            docType = "servicePolicies"; //$NON-NLS-1$
        } else if (type == PolicyType.Application) {
            docType = "applicationPolicies"; //$NON-NLS-1$
        }
        return docType;
    }

    /**
     * A composite ID created from an organization ID and entity ID.
     * @param organizationId
     * @param entityId
     */
    private static String id(String organizationId, String entityId) {
        return organizationId + ":" + entityId; //$NON-NLS-1$
    }

    /**
     * A composite ID created from an organization ID, entity ID, and version.
     * @param organizationId
     * @param entityId
     * @param version
     */
    private static String id(String organizationId, String entityId, String version) {
        return organizationId + ':' + entityId + ':' + version;
    }

    @Override
    public Iterator<OrganizationBean> getAllOrganizations() throws StorageException {
        return getAll("organization", new IUnmarshaller<OrganizationBean>() { //$NON-NLS-1$
            @Override
            public OrganizationBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallOrganization(source);
            }
        });
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getAllPlans(java.lang.String)
     */
    @Override
    public Iterator<PlanBean> getAllPlans(String organizationId) throws StorageException {
        return getAll("plan", new IUnmarshaller<PlanBean>() { //$NON-NLS-1$
            @Override
            public PlanBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallPlan(source);
            }
        }, matchOrgQuery(organizationId));
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getAllApplications(java.lang.String)
     */
    @Override
    public Iterator<ApplicationBean> getAllApplications(String organizationId) throws StorageException {
        return getAll("application", new IUnmarshaller<ApplicationBean>() { //$NON-NLS-1$
            @Override
            public ApplicationBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallApplication(source);
            }
        }, matchOrgQuery(organizationId));
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getAllServices(java.lang.String)
     */
    @Override
    public Iterator<ServiceBean> getAllServices(String organizationId) throws StorageException {
        return getAll("service", new IUnmarshaller<ServiceBean>() { //$NON-NLS-1$
            @Override
            public ServiceBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallService(source);
            }
        }, matchOrgQuery(organizationId));
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getAllPlanVersions(java.lang.String, java.lang.String)
     */
    @SuppressWarnings("nls")
    @Override
    public Iterator<PlanVersionBean> getAllPlanVersions(String organizationId, String planId)
            throws StorageException {
        String query = "{" +
                "  \"query\": {" +
                "    \"filtered\": { " +
                "      \"filter\": {" +
                "        \"and\" : [" +
                "          {" +
                "            \"term\": { \"organizationId\": \"" + organizationId + "\" }" +
                "          }," +
                "          {" +
                "            \"term\": { \"planId\": \"" + planId + "\" }" +
                "          }" +
                "      ]" +
                "      }" +
                "    }" +
                "  }" +
                "}";
        return getAll("planVersion", new IUnmarshaller<PlanVersionBean>() { //$NON-NLS-1$
            @Override
            public PlanVersionBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallPlanVersion(source);
            }
        }, query);
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getAllServiceVersions(java.lang.String, java.lang.String)
     */
    @SuppressWarnings("nls")
    @Override
    public Iterator<ServiceVersionBean> getAllServiceVersions(String organizationId, String serviceId)
            throws StorageException {
        String query = "{" +
                "  \"query\": {" +
                "    \"filtered\": { " +
                "      \"filter\": {" +
                "        \"and\" : [" +
                "          {" +
                "            \"term\": { \"organizationId\": \"" + organizationId + "\" }" +
                "          }," +
                "          {" +
                "            \"term\": { \"serviceId\": \"" + serviceId + "\" }" +
                "          }" +
                "      ]" +
                "      }" +
                "    }" +
                "  }" +
                "}";
        return getAll("serviceVersion", new IUnmarshaller<ServiceVersionBean>() { //$NON-NLS-1$
            @Override
            public ServiceVersionBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallServiceVersion(source);
            }
        }, query);
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getAllApplicationVersions(java.lang.String, java.lang.String)
     */
    @SuppressWarnings("nls")
    @Override
    public Iterator<ApplicationVersionBean> getAllApplicationVersions(String organizationId,
            String applicationId) throws StorageException {
        String query = "{" +
                "  \"query\": {" +
                "    \"filtered\": { " +
                "      \"filter\": {" +
                "        \"and\" : [" +
                "          {" +
                "            \"term\": { \"organizationId\": \"" + organizationId + "\" }" +
                "          }," +
                "          {" +
                "            \"term\": { \"applicationId\": \"" + applicationId + "\" }" +
                "          }" +
                "      ]" +
                "      }" +
                "    }" +
                "  }" +
                "}";
        return getAll("applicationVersion", new IUnmarshaller<ApplicationVersionBean>() { //$NON-NLS-1$
            @Override
            public ApplicationVersionBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallApplicationVersion(source);
            }
        }, query);
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getAllContracts(java.lang.String, java.lang.String, java.lang.String)
     */
    @SuppressWarnings("nls")
    @Override
    public Iterator<ContractBean> getAllContracts(String organizationId, String applicationId, String version)
            throws StorageException {
        String query = "{" +
                "  \"query\": {" +
                "    \"filtered\": {" +
                "      \"filter\": {" +
                "        \"and\" : [" +
                "          {" +
                "            \"term\": { \"appOrganizationId\": \"" + organizationId + "\" }" +
                "          }," +
                "          {" +
                "            \"term\": { \"appId\": \"" + applicationId + "\" }" +
                "          }," +
                "          {" +
                "            \"term\": { \"appVersion\": \"" + version + "\" }" +
                "          }" +
                "      ]" +
                "      }" +
                "    }" +
                "  }" +
                "}";
        return getAll("contract", new IUnmarshaller<ContractBean>() { //$NON-NLS-1$
            @Override
            public ContractBean unmarshal(Map<String, Object> source) {
                ContractBean contract = EsMarshalling.unmarshallContract(source);
                String svcOrgId = (String) source.get("serviceOrganizationId");
                String svcId = (String) source.get("serviceId");
                String svcVersion = (String) source.get("serviceVersion");
                String planId = (String) source.get("planId");
                String planVersion = (String) source.get("planVersion");

                ServiceVersionBean svb = new ServiceVersionBean();
                svb.setVersion(svcVersion);
                svb.setService(new ServiceBean());
                svb.getService().setOrganization(new OrganizationBean());
                svb.getService().setId(svcId);
                svb.getService().getOrganization().setId(svcOrgId);

                PlanVersionBean pvb = new PlanVersionBean();
                pvb.setVersion(planVersion);
                pvb.setPlan(new PlanBean());
                pvb.getPlan().setOrganization(new OrganizationBean());
                pvb.getPlan().setId(planId);
                pvb.getPlan().getOrganization().setId(svcOrgId);

                contract.setPlan(pvb);
                contract.setService(svb);
                return contract;
            }
        }, query);
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getAllPolicies(java.lang.String, java.lang.String, java.lang.String, io.apiman.manager.api.beans.policies.PolicyType)
     */
    @Override
    public Iterator<PolicyBean> getAllPolicies(String organizationId, String entityId, String version,
            PolicyType type) throws StorageException {
        try {
            String docType = getPoliciesDocType(type);
            String pid = id(organizationId, entityId, version);
            Map<String, Object> source = getEntity(docType, pid);
            PoliciesBean policies = EsMarshalling.unmarshallPolicies(source);
            if (policies == null || policies.getPolicies() == null) {
                return new ArrayList<PolicyBean>().iterator();
            }
            List<PolicyBean> policyBeans = policies.getPolicies();
            // TODO resolve the policy def, since we know we'll only have the definition ID here
            for (PolicyBean policyBean : policyBeans) {
                PolicyDefinitionBean def = getPolicyDefinition(policyBean.getDefinition().getId());
                if (def != null) {
                    policyBean.setDefinition(def);
                }
            }
            return policyBeans.iterator();
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Iterator<GatewayBean> getAllGateways() throws StorageException {
        return getAll("gateway", new IUnmarshaller<GatewayBean>() { //$NON-NLS-1$
            @Override
            public GatewayBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallGateway(source);
            }
        });
    }

    @Override
    public Iterator<UserBean> getAllUsers() throws StorageException {
        return getAll("user", new IUnmarshaller<UserBean>() { //$NON-NLS-1$
            @Override
            public UserBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallUser(source);
            }
        });
    }

    @Override
    public Iterator<RoleBean> getAllRoles() throws StorageException {
        return getAll("role", new IUnmarshaller<RoleBean>() { //$NON-NLS-1$
            @Override
            public RoleBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallRole(source);
            }
        });
    }

    @Override
    public Iterator<RoleMembershipBean> getAllMemberships(String organizationId) throws StorageException {
        return getAll("roleMembership", new IUnmarshaller<RoleMembershipBean>() { //$NON-NLS-1$
            @Override
            public RoleMembershipBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallRoleMembership(source);
            }
        }, matchOrgQuery(organizationId));
    }

    @Override
    public Iterator<AuditEntryBean> getAllAuditEntries(String organizationId) throws StorageException {
        return getAll("auditEntry", new IUnmarshaller<AuditEntryBean>() { //$NON-NLS-1$
            @Override
            public AuditEntryBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallAuditEntry(source);
            }
        }, matchOrgQuery(organizationId));
    }

    @Override
    public Iterator<PluginBean> getAllPlugins() throws StorageException {
        return getAll("plugin", new IUnmarshaller<PluginBean>() { //$NON-NLS-1$
            @Override
            public PluginBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallPlugin(source);
            }
        });
    }

    /**
     * @see io.apiman.manager.api.core.IStorage#getAllPolicyDefinitions()
     */
    @Override
    public Iterator<PolicyDefinitionBean> getAllPolicyDefinitions() throws StorageException {
        return getAll("policyDef", new IUnmarshaller<PolicyDefinitionBean>() { //$NON-NLS-1$
            @Override
            public PolicyDefinitionBean unmarshal(Map<String, Object> source) {
                return EsMarshalling.unmarshallPolicyDefinition(source);
            }
        });
    }

    /**
     * Returns an iterator over all instances of the given entity type.
     * @param entityType
     * @param unmarshaller
     * @throws StorageException
     */
    private <T> Iterator<T> getAll(String entityType, IUnmarshaller<T> unmarshaller) throws StorageException {
        String query = matchAllQuery();
        return getAll(entityType, unmarshaller, query);
    }

    /**
     * Returns an iterator over all instances of the given entity type.
     * @param entityType
     * @param unmarshaller
     * @param query
     * @throws StorageException
     */
    private <T> Iterator<T> getAll(String entityType, IUnmarshaller<T> unmarshaller, String query) throws StorageException {
        return new EntityIterator<>(entityType, unmarshaller, query);
    }

    /**
     * A simple, internal unmarshaller interface.
     * @author eric.wittmann@redhat.com
     */
    private static interface IUnmarshaller<T> {
        /**
         * Unmarshal the source map into an entity.
         * @param source the source map
         * @return the unmarshalled instance of <T>
         */
        public T unmarshal(Map<String, Object> source);
    }

    /**
     * Allows iterating over all entities of a given type.
     * @author eric.wittmann@redhat.com
     */
    @SuppressWarnings("nls")
    private class EntityIterator<T> implements Iterator<T> {

        private String query;
        private String entityType;
        private IUnmarshaller<T> unmarshaller;
        private String scrollId = null;
        private List<Hit<Map<String, Object>, Void>> hits;
        private int nextHitIdx;;

        /**
         * Constructor.
         * @param entityType
         * @param unmarshaller
         * @param query
         * @throws StorageException
         */
        public EntityIterator(String entityType, IUnmarshaller<T> unmarshaller, String query) throws StorageException {
            this.entityType = entityType;
            this.unmarshaller = unmarshaller;
            this.query = query;
            initScroll();
            this.nextHitIdx = 0;
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            if (hits == null || this.nextHitIdx >= hits.size()) {
                try {
                    fetch();
                } catch (StorageException e) {
                    throw new RuntimeException(e);
                }
                this.nextHitIdx = 0;
            }
            return hits.size() > 0;
        }

        /**
         * @see java.util.Iterator#next()
         */
        @Override
        public T next() {
            Hit<Map<String, Object>, Void> hit = hits.get(nextHitIdx++);
            return unmarshaller.unmarshal(hit.source);
        }

        /**
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
            // Not implemented.
        }

        private void initScroll() throws StorageException {
            try {
                Search search = new Search.Builder(query).addIndex(INDEX_NAME).addType(entityType)
                        .setSearchType(SearchType.SCAN).setParameter(Parameters.SCROLL, "1m").build();
                SearchResult response = esClient.execute(search);
                scrollId = response.getJsonObject().get("_scroll_id").getAsString();
            } catch (IOException e) {
                throw new StorageException(e);
            }
        }

        private void fetch() throws StorageException {
            try {
                Builder builder = new SearchScroll.Builder(scrollId, "1m")
                        .setParameter(Parameters.SIZE, 1);
                SearchScroll scroll = new SearchScroll(builder) {
                    @Override
                    public JestResult createNewElasticSearchResult(String responseBody, int statusCode,
                            String reasonPhrase, Gson gson) {
                        return createNewElasticSearchResult(new SearchResult(gson), responseBody, statusCode, reasonPhrase, gson);
                    }
                };
                SearchResult response = (SearchResult) esClient.execute(scroll);
                this.hits = (List) response.getHits(Map.class);
            } catch (IOException e) {
                throw new StorageException(e);
            }
        }

    }

    /**
     * @return an ES query to match all documents
     */
    @SuppressWarnings("nls")
    private String matchAllQuery() {
        return "{" +
                "  \"query\": {" +
                "    \"match_all\": {}" +
                "  }" +
                "}";
    }

    @SuppressWarnings("nls")
    private String matchOrgQuery(String organizationId) {
        return "{" +
                "  \"query\": {" +
                "    \"filtered\": { " +
                "      \"filter\": {" +
                "        \"term\": { \"organizationId\": \"" + organizationId + "\" }" +
                "      }" +
                "    }" +
                "  }" +
                "}";
    }

}
