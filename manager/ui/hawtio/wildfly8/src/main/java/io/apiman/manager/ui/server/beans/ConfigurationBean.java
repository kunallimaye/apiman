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
package io.apiman.manager.ui.server.beans;

import io.apiman.manager.ui.server.servlets.ConfigurationServlet;

import java.io.Serializable;

/**
 * Encapsulates initial app configuration data sent from the server (via the
 * {@link ConfigurationServlet} servlet) to the client.
 * 
 * @author eric.wittmann@redhat.com
 */
public class ConfigurationBean implements Serializable {

    private static final long serialVersionUID = -6342457151615532102L;

    private AppConfigurationBean apiman;
    private UserConfigurationBean user;
    private UiConfigurationBean ui;
    private ApiConfigurationBean api;

    /**
     * Constructor.
     */
    public ConfigurationBean() {
    }

    /**
     * @return the apiman
     */
    public AppConfigurationBean getApiman() {
        return apiman;
    }

    /**
     * @param apiman
     *            the apiman to set
     */
    public void setApiman(AppConfigurationBean apiman) {
        this.apiman = apiman;
    }

    /**
     * @return the user
     */
    public UserConfigurationBean getUser() {
        return user;
    }

    /**
     * @param user
     *            the user to set
     */
    public void setUser(UserConfigurationBean user) {
        this.user = user;
    }

    /**
     * @return the api
     */
    public ApiConfigurationBean getApi() {
        return api;
    }

    /**
     * @param api
     *            the api to set
     */
    public void setApi(ApiConfigurationBean api) {
        this.api = api;
    }

    /**
     * @return the ui
     */
    public UiConfigurationBean getUi() {
        return ui;
    }

    /**
     * @param ui the ui to set
     */
    public void setUi(UiConfigurationBean ui) {
        this.ui = ui;
    }
}
