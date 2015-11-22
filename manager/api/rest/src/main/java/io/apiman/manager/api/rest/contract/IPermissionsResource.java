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

import io.apiman.manager.api.beans.idm.UserPermissionsBean;
import io.apiman.manager.api.rest.contract.exceptions.NotAuthorizedException;
import io.apiman.manager.api.rest.contract.exceptions.UserNotFoundException;
import io.swagger.annotations.Api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * The Permissions API.
 * 
 * @author eric.wittmann@redhat.com
 */
@Path("permissions")
@Api
public interface IPermissionsResource {

    /**
     * This endpoint returns all of the permissions assigned to a specific user.
     * @summary Get User's Permissions
     * @servicetag admin
     * @param userId The user's ID.
     * @statuscode 200 If the permissions are successfully retrieved.
     * @return All of the user's permissions.
     * @throws UserNotFoundException when a request is sent for a user who does not exist
     * @throws NotAuthorizedException when the user is not authorized to perform this action
     */
    @GET
    @Path("{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserPermissionsBean getPermissionsForUser(@PathParam("userId") String userId)
            throws UserNotFoundException, NotAuthorizedException;

    /**
     * This endpoint returns all of the permissions assigned to the currently 
     * authenticated user.
     * @summary Get Current User's Permissions
     * @statuscode 200 If the permissions are successfully retrieved.
     * @return All of the user's permissions.
     * @throws UserNotFoundException when a request is sent for a user who does not exist
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public UserPermissionsBean getPermissionsForCurrentUser() throws UserNotFoundException;
    
}
