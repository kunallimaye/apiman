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
package io.apiman.gateway.engine.metrics;

import io.apiman.gateway.engine.IMetrics;

import java.io.Serializable;
import java.util.Date;

/**
 * Metric information about a single request processed by the API Gateway.
 * This information is then reported to the {@link IMetrics} subsystem.
 *
 * @author eric.wittmann@redhat.com
 */
public class RequestMetric implements Serializable {

    private static final long serialVersionUID = 7085676761317470403L;

    private Date requestStart;
    private Date requestEnd;
    private long requestDuration = -1;
    private Date serviceStart;
    private Date serviceEnd;
    private long serviceDuration = 1;
    private String url;
    private String resource;
    private String method;
    private String serviceOrgId;
    private String serviceId;
    private String serviceVersion;
    private String planId;
    private String applicationOrgId;
    private String applicationId;
    private String applicationVersion;
    private String contractId;
    private String user;
    private int responseCode;
    private String responseMessage;
    private boolean failure;
    private int failureCode;
    private String failureReason;
    private boolean error;
    private String errorMessage;
    private long bytesUploaded;
    private long bytesDownloaded;

    /**
     * Constructor.
     */
    public RequestMetric() {
    }

    /**
     * @return the serviceDuration
     */
    public long getServiceDuration() {
        return serviceDuration;
    }

    /**
     * @param serviceDuration the serviceDuration to set
     */
    public void setServiceDuration(long serviceDuration) {
        this.serviceDuration = serviceDuration;
    }

    /**
     * @return the resource
     */
    public String getResource() {
        return resource;
    }

    /**
     * @param resource the resource to set
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * @param method the method to set
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * @return the serviceOrgId
     */
    public String getServiceOrgId() {
        return serviceOrgId;
    }

    /**
     * @param serviceOrgId the serviceOrgId to set
     */
    public void setServiceOrgId(String serviceOrgId) {
        this.serviceOrgId = serviceOrgId;
    }

    /**
     * @return the serviceId
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * @param serviceId the serviceId to set
     */
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * @return the serviceVersion
     */
    public String getServiceVersion() {
        return serviceVersion;
    }

    /**
     * @param serviceVersion the serviceVersion to set
     */
    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    /**
     * @return the applicationOrgId
     */
    public String getApplicationOrgId() {
        return applicationOrgId;
    }

    /**
     * @param applicationOrgId the applicationOrgId to set
     */
    public void setApplicationOrgId(String applicationOrgId) {
        this.applicationOrgId = applicationOrgId;
    }

    /**
     * @return the applicationId
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * @param applicationId the applicationId to set
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * @return the applicationVersion
     */
    public String getApplicationVersion() {
        return applicationVersion;
    }

    /**
     * @param applicationVersion the applicationVersion to set
     */
    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    /**
     * @return the contractId
     */
    public String getContractId() {
        return contractId;
    }

    /**
     * @param contractId the contractId to set
     */
    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    /**
     * @return the responseCode
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * @param responseCode the responseCode to set
     */
    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * @return the responseMessage
     */
    public String getResponseMessage() {
        return responseMessage;
    }

    /**
     * @param responseMessage the responseMessage to set
     */
    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    /**
     * @return the failureCode
     */
    public int getFailureCode() {
        return failureCode;
    }

    /**
     * @param failureCode the failureCode to set
     */
    public void setFailureCode(int failureCode) {
        this.failureCode = failureCode;
    }

    /**
     * @return the failureReason
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * @param failureReason the failureReason to set
     */
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    /**
     * @return the requestStart
     */
    public Date getRequestStart() {
        return requestStart;
    }

    /**
     * @param requestStart the requestStart to set
     */
    public void setRequestStart(Date requestStart) {
        this.requestStart = requestStart;
    }

    /**
     * @return the requestEnd
     */
    public Date getRequestEnd() {
        return requestEnd;
    }

    /**
     * @param requestEnd the requestEnd to set
     */
    public void setRequestEnd(Date requestEnd) {
        this.requestEnd = requestEnd;
        this.requestDuration = requestEnd.getTime() - requestStart.getTime();
    }

    /**
     * @return the requestDuration
     */
    public long getRequestDuration() {
        return requestDuration;
    }

    /**
     * @param requestDuration the requestDuration to set
     */
    public void setRequestDuration(long requestDuration) {
        this.requestDuration = requestDuration;
    }

    /**
     * @return the serviceStart
     */
    public Date getServiceStart() {
        return serviceStart;
    }

    /**
     * @param serviceStart the serviceStart to set
     */
    public void setServiceStart(Date serviceStart) {
        this.serviceStart = serviceStart;
    }

    /**
     * @return the serviceEnd
     */
    public Date getServiceEnd() {
        return serviceEnd;
    }

    /**
     * @param serviceEnd the serviceEnd to set
     */
    public void setServiceEnd(Date serviceEnd) {
        this.serviceEnd = serviceEnd;
        this.serviceDuration = serviceEnd.getTime() - serviceStart.getTime();
    }

    /**
     * @return the error
     */
    public boolean isError() {
        return error;
    }

    /**
     * @param error the error to set
     */
    public void setError(boolean error) {
        this.error = error;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage the errorMessage to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the failure
     */
    public boolean isFailure() {
        return failure;
    }

    /**
     * @param failure the failure to set
     */
    public void setFailure(boolean failure) {
        this.failure = failure;
    }

    /**
     * @return the planId
     */
    public String getPlanId() {
        return planId;
    }

    /**
     * @param planId the planId to set
     */
    public void setPlanId(String planId) {
        this.planId = planId;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the bytesUploaded
     */
    public long getBytesUploaded() {
        return bytesUploaded;
    }

    /**
     * @param bytesUploaded the bytesUploaded to set
     */
    public void setBytesUploaded(long bytesUploaded) {
        this.bytesUploaded = bytesUploaded;
    }

    /**
     * @return the bytesDownloaded
     */
    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

    /**
     * @param bytesDownloaded the bytesDownloaded to set
     */
    public void setBytesDownloaded(long bytesDownloaded) {
        this.bytesDownloaded = bytesDownloaded;
    }

}
