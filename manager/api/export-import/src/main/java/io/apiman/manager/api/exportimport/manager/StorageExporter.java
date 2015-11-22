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

package io.apiman.manager.api.exportimport.manager;

import io.apiman.manager.api.beans.apps.ApplicationBean;
import io.apiman.manager.api.beans.apps.ApplicationVersionBean;
import io.apiman.manager.api.beans.audit.AuditEntryBean;
import io.apiman.manager.api.beans.contracts.ContractBean;
import io.apiman.manager.api.beans.gateways.GatewayBean;
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
import io.apiman.manager.api.beans.services.ServiceBean;
import io.apiman.manager.api.beans.services.ServiceVersionBean;
import io.apiman.manager.api.config.Version;
import io.apiman.manager.api.core.IStorage;
import io.apiman.manager.api.core.exceptions.StorageException;
import io.apiman.manager.api.core.logging.ApimanLogger;
import io.apiman.manager.api.core.logging.IApimanLogger;
import io.apiman.manager.api.exportimport.beans.MetadataBean;
import io.apiman.manager.api.exportimport.i18n.Messages;
import io.apiman.manager.api.exportimport.write.IExportWriter;

import java.util.Iterator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.joda.time.DateTime;

@ApplicationScoped
public class StorageExporter {
    @Inject @ApimanLogger(StorageExporter.class)
    private IApimanLogger logger;
    @Inject
    private Version version;
    @Inject
    private IStorage storage;
    
    private IExportWriter writer;

    /**
     * Constructor.
     */
    public StorageExporter() {
    }
    
    /**
     * @param writer
     */
    public void init(IExportWriter writer) {
        this.writer = writer;
    }

    /**
     * Called begin the export process.
     */
    public void export() {
        logger.info("----------------------------"); //$NON-NLS-1$
        logger.info(Messages.i18n.format("StorageExporter.StartingExport")); //$NON-NLS-1$
        try {
            storage.beginTx();
            try {
                exportMetadata();
                exportUsers();
                exportGateways();
                exportPlugins();
                exportRoles();
                exportPolicyDefs();

                exportOrgs();
            } finally {
                storage.rollbackTx();
                try { writer.close(); } catch (Exception e) { }
            }
            logger.info(Messages.i18n.format("StorageExporter.ExportComplete")); //$NON-NLS-1$
            logger.info("------------------------------------------"); //$NON-NLS-1$
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    private void exportOrgs() {
        try {
          writer.startOrgs();

          Iterator<OrganizationBean> iter = storage.getAllOrganizations();

          while (iter.hasNext()) {
              OrganizationBean bean = iter.next();
              writer.startOrg(bean);
              logger.info(Messages.i18n.format("StorageExporter.ExportingOrgs") + bean); //$NON-NLS-1$

              exportMemberships(bean.getId());
              exportPlans(bean.getId());
              exportServices(bean.getId());
              exportApplications(bean.getId());
              exportAudits(bean.getId());
              
              writer.endOrg();
          }

          writer.endOrgs();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void exportPlans(String orgId) {
        try {
            writer.startPlans();
            Iterator<PlanBean> planIter = storage.getAllPlans(orgId);
            while (planIter.hasNext()) {
                PlanBean planBean = planIter.next();
                writer.startPlan(planBean);
                writer.startPlanVersions();
                Iterator<PlanVersionBean> versionIter = storage.getAllPlanVersions(orgId, planBean.getId());
                while (versionIter.hasNext()) {
                    PlanVersionBean versionBean = versionIter.next();
                    logger.info(Messages.i18n.format("StorageExporter.ExportingPlanVersion") + versionBean); //$NON-NLS-1$
                    writer.startPlanVersion(versionBean);
                    writer.startPlanPolicies();
                    Iterator<PolicyBean> policyIter = storage.getAllPolicies(orgId, planBean.getId(), versionBean.getVersion(), PolicyType.Plan);
                    while (policyIter.hasNext()) {
                        PolicyBean policyBean = policyIter.next();
                        logger.info(Messages.i18n.format("StorageExporter.ExportingPlanPolicy") + policyBean); //$NON-NLS-1$
                        writer.writePlanPolicy(policyBean);
                    }
                    writer.endPlanPolicies();
                    writer.endPlanVersion();
                }
                writer.endPlanVersions();
                writer.endPlan();
            }
            writer.endPlans();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void exportServices(String orgId) {
        try {
            writer.startServices();
            Iterator<ServiceBean> serviceIter = storage.getAllServices(orgId);
            while (serviceIter.hasNext()) {
                ServiceBean serviceBean = serviceIter.next();
                writer.startService(serviceBean);
                writer.startServiceVersions();
                Iterator<ServiceVersionBean> versionIter = storage.getAllServiceVersions(orgId, serviceBean.getId());
                while (versionIter.hasNext()) {
                    ServiceVersionBean versionBean = versionIter.next();
                    logger.info(Messages.i18n.format("StorageExporter.ExportingServiceVersion") + versionBean); //$NON-NLS-1$
                    writer.startServiceVersion(versionBean);
                    writer.startServicePolicies();
                    Iterator<PolicyBean> policyIter = storage.getAllPolicies(orgId, serviceBean.getId(), versionBean.getVersion(), PolicyType.Service);
                    while (policyIter.hasNext()) {
                        PolicyBean policyBean = policyIter.next();
                        logger.info(Messages.i18n.format("StorageExporter.ExportingServicePolicy") + policyBean); //$NON-NLS-1$
                        writer.writeServicePolicy(policyBean);
                    }
                    writer.endServicePolicies();
                    writer.endServiceVersion();
                }
                writer.endServiceVersions();
                writer.endService();
            }
            writer.endServices();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void exportApplications(String orgId) {
        try {
            writer.startApplications();
            Iterator<ApplicationBean> applicationIter = storage.getAllApplications(orgId);
            while (applicationIter.hasNext()) {
                ApplicationBean applicationBean = applicationIter.next();
                logger.info(Messages.i18n.format("StorageExporter.ExportingApp") + applicationBean); //$NON-NLS-1$
                writer.startApplication(applicationBean);
                writer.startApplicationVersions();
                Iterator<ApplicationVersionBean> versionIter = storage.getAllApplicationVersions(orgId, applicationBean.getId());
                while (versionIter.hasNext()) {
                    ApplicationVersionBean versionBean = versionIter.next();
                    logger.info(Messages.i18n.format("StorageExporter.ExportingAppVersion") + versionBean); //$NON-NLS-1$
                    writer.startApplicationVersion(versionBean);
                    
                    // Policies
                    writer.startApplicationPolicies();
                    Iterator<PolicyBean> policyIter = storage.getAllPolicies(orgId, applicationBean.getId(), versionBean.getVersion(), PolicyType.Application);
                    while (policyIter.hasNext()) {
                        PolicyBean policyBean = policyIter.next();
                        logger.info(Messages.i18n.format("StorageExporter.ExportingAppPolicy") + policyBean); //$NON-NLS-1$
                        writer.writeApplicationPolicy(policyBean);
                    }
                    writer.endApplicationPolicies();
                    
                    // Contracts
                    writer.startApplicationContracts();
                    Iterator<ContractBean> contractIter = storage.getAllContracts(orgId, applicationBean.getId(), versionBean.getVersion());
                    while (contractIter.hasNext()) {
                        ContractBean contractBean = contractIter.next();
                        contractBean.setApplication(null);
                        contractBean.setService(minifyService(contractBean.getService()));
                        contractBean.setPlan(minifyPlan(contractBean.getPlan()));
                        logger.info(Messages.i18n.format("StorageExporter.ExportingAppContract") + contractBean); //$NON-NLS-1$
                        writer.writeApplicationContract(contractBean);
                    }
                    writer.endApplicationContracts();
                    
                    writer.endApplicationVersion();
                }
                writer.endApplicationVersions();
                writer.endApplication();
            }
            writer.endApplications();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void exportMemberships(String orgId) {
        try {
            Iterator<RoleMembershipBean> iter = storage.getAllMemberships(orgId);

            writer.startMemberships();

            while(iter.hasNext()) {
                RoleMembershipBean bean = iter.next();
                logger.info(Messages.i18n.format("StorageExporter.ExportingMembership") + bean); //$NON-NLS-1$
                writer.writeMembership(bean);
            }

            writer.endMemberships();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void exportAudits(String orgId) {
        try {
            Iterator<AuditEntryBean> iter = storage.getAllAuditEntries(orgId);

            writer.startAudits();

            while(iter.hasNext()) {
                AuditEntryBean bean = iter.next();
                logger.info(Messages.i18n.format("StorageExporter.ExportingAuditEntry") + bean); //$NON-NLS-1$
                writer.writeAudit(bean);
            }

            writer.endAudits();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void exportMetadata() {
        logger.info(Messages.i18n.format("StorageExporter.ExportingMetaData")); //$NON-NLS-1$
        MetadataBean metadata = new MetadataBean();
        metadata.setApimanVersion(version.getVersionString());
        metadata.setExportedOn(new DateTime());
        writer.writeMetadata(metadata);
    }

    private void exportGateways() {
        try {
            Iterator<GatewayBean> iter = storage.getAllGateways();

            writer.startGateways();

            while(iter.hasNext()) {
                GatewayBean bean = iter.next();
                logger.info(Messages.i18n.format("StorageExporter.ExportingGateway") + bean); //$NON-NLS-1$
                writer.writeGateway(bean);
            }

            writer.endGateways();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void exportPlugins() {
        try {
            Iterator<PluginBean> iter = storage.getAllPlugins();

            writer.startPlugins();

            while(iter.hasNext()) {
                PluginBean bean = iter.next();
                logger.info(Messages.i18n.format("StorageExporter.ExportingPlugin") + bean); //$NON-NLS-1$
                writer.writePlugin(bean);
            }

            writer.endPlugins();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void exportUsers() {
        try {
            Iterator<UserBean> iter;

            iter = storage.getAllUsers();

            writer.startUsers();

            while (iter.hasNext()) {
                UserBean bean = iter.next();
                logger.info(Messages.i18n.format("StorageExporter.ExportingUser") + bean); //$NON-NLS-1$
                writer.writeUser(bean);
            }

            writer.endUsers();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void exportRoles() {
        try {
            Iterator<RoleBean> iter;
            
            iter = storage.getAllRoles();
            
            writer.startRoles();

            while(iter.hasNext()) {
                RoleBean bean = iter.next();
                logger.info(Messages.i18n.format("StorageExporter.ExportingRole") + bean); //$NON-NLS-1$
                writer.writeRole(bean);
            }

            writer.endRoles();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void exportPolicyDefs() {
        try {
            Iterator<PolicyDefinitionBean> iter;
            
            iter = storage.getAllPolicyDefinitions();
            
            writer.startPolicyDefs();

            while(iter.hasNext()) {
                PolicyDefinitionBean bean = iter.next();
                logger.info(Messages.i18n.format("StorageExporter.ExportingPolicyDefinition") + bean); //$NON-NLS-1$
                writer.writePolicyDef(bean);
            }

            writer.endPolicyDefs();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param plan
     */
    private PlanVersionBean minifyPlan(PlanVersionBean plan) {
        PlanVersionBean rval = new PlanVersionBean();
        rval.setVersion(plan.getVersion());
        rval.setPlan(new PlanBean());
        rval.getPlan().setId(plan.getPlan().getId());
        rval.getPlan().setOrganization(new OrganizationBean());
        rval.getPlan().getOrganization().setId(plan.getPlan().getOrganization().getId());
        return rval;
    }

    /**
     * @param service
     */
    private ServiceVersionBean minifyService(ServiceVersionBean service) {
        ServiceVersionBean rval = new ServiceVersionBean();
        rval.setVersion(service.getVersion());
        rval.setService(new ServiceBean());
        rval.getService().setId(service.getService().getId());
        rval.getService().setOrganization(new OrganizationBean());
        rval.getService().getOrganization().setId(service.getService().getOrganization().getId());
        return rval;
    }

}