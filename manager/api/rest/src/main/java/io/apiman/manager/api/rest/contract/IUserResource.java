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

package io.apiman.manager.api.rest.contract;

import io.apiman.manager.api.beans.audit.AuditEntryBean;
import io.apiman.manager.api.beans.idm.UpdateUserBean;
import io.apiman.manager.api.beans.idm.UserBean;
import io.apiman.manager.api.beans.search.SearchCriteriaBean;
import io.apiman.manager.api.beans.search.SearchResultsBean;
import io.apiman.manager.api.beans.summary.ApplicationSummaryBean;
import io.apiman.manager.api.beans.summary.OrganizationSummaryBean;
import io.apiman.manager.api.beans.summary.ServiceSummaryBean;
import io.apiman.manager.api.rest.contract.exceptions.InvalidSearchCriteriaException;
import io.apiman.manager.api.rest.contract.exceptions.NotAuthorizedException;
import io.apiman.manager.api.rest.contract.exceptions.UserNotFoundException;
import io.swagger.annotations.Api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * The User API.
 * 
 * @author eric.wittmann@redhat.com
 */
@Path("users")
@Api
public interface IUserResource {

    /**
     * Use this endpoint to get information about a specific user by the User ID.
     * @summary Get User by ID
     * @param userId The user ID.
     * @statuscode 200 If the user exists and information is returned.
     * @return Full user information.
     * @throws UserNotFoundException when specified user not found
     */
    @GET
    @Path("{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserBean get(@PathParam("userId") String userId) throws UserNotFoundException;

    /**
     * Use this endpoint to update the information about a user.  This will fail
     * unless the authenticated user is an admin or identical to the user being
     * updated.
     * @summary Update a User by ID
     * @param userId The user ID.
     * @param user Updated user information.
     * @statuscode 204 If the user information is successfully updated.
     * @throws UserNotFoundException when specified user not found
     * @throws NotAuthorizedException when not authorized to invoke this method
     */
    @PUT
    @Path("{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@PathParam("userId") String userId, UpdateUserBean user) throws UserNotFoundException, NotAuthorizedException;

    /**
     * Use this endpoint to search for users.  The search criteria is
     * provided in the body of the request, including filters, order-by, and paging
     * information.
     * @summary Search for Users
     * @param criteria The search criteria.
     * @statuscode 200 If the search is successful.
     * @return The search results (a page of organizations).
     * @throws InvalidSearchCriteriaException when provided criteria are invalid
     */
    @POST
    @Path("search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResultsBean<UserBean> search(SearchCriteriaBean criteria) throws InvalidSearchCriteriaException;

    /**
     * This endpoint returns the list of organizations that the user is a member of.  The
     * user is a member of an organization if she has at least one role for the org.
     * @summary List User Organizations
     * @param userId The user ID.
     * @statuscode 200 If the organization list is successfully returned.
     * @return List of organizations.
     */
    @GET
    @Path("{userId}/organizations")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OrganizationSummaryBean> getOrganizations(@PathParam("userId") String userId);

    /**
     * This endpoint returns all applications that the user has permission to edit.
     * @summary List User Applications
     * @param userId The user ID.
     * @statuscode 200 If the application list is successfully returned.
     * @return List of applications.
     */
    @GET
    @Path("{userId}/applications")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ApplicationSummaryBean> getApplications(@PathParam("userId") String userId);

    /**
     * This endpoint returns all services that the user has permission to edit.
     * @summary List User Services
     * @param userId The user ID.
     * @statuscode 200 If the service list is successfully returned.
     * @return List of services.
     */
    @GET
    @Path("{userId}/services")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ServiceSummaryBean> getServices(@PathParam("userId") String userId);

    /**
     * Use this endpoint to get information about the user's audit history.  This
     * returns audit entries corresponding to each of the actions taken by the
     * user.  For example, when a user creates a new Organization, an audit entry
     * is recorded and would be included in the result of this endpoint.
     * @summary Get User Activity
     * @param userId The user ID.
     * @param page The page of the results to return.
     * @param pageSize The number of results per page to return.
     * @statuscode 200 If the activity is successfully returned.
     * @return List of audit entries.
     */
    @GET
    @Path("{userId}/activity")
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResultsBean<AuditEntryBean> getActivity(@PathParam("userId") String userId,
            @QueryParam("page") int page, @QueryParam("count") int pageSize);
    
}
