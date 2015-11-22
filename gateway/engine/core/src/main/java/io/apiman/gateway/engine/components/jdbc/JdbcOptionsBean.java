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
package io.apiman.gateway.engine.components.jdbc;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JDBC Options. Based loosely upon HikariCP's simplest options.
 *
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class JdbcOptionsBean {

    private String jdbcUrl;
    private String username;
    private String password;
    private Boolean autoCommit;

    private Long connectionTimeout;
    private Long idleTimeout;
    private Long maxLifetime;
    private Long minimumIdle;
    private Long maximumPoolSize;

    private String poolName;

    // Set any uncommon properties using the map
    private Map<String, Object> dsProperties = new LinkedHashMap<>();

    /**
     * Default empty constructor
     */
    public JdbcOptionsBean() {}

    /**
     * @param jdbcUrl the jdbc database url
     * @param username the url
     * @param password the password
     * @param autoCommit the autocommit status
     * @param connectionTimeout the connection timeout
     * @param idleTimeout the idle timeout
     * @param maxLifetime the max lifetime
     * @param minimumIdle the minimum idle time
     * @param maximumPoolSize the maximum pool size
     * @param poolName the pool name (if not provided, will be generated)
     * @param dsProperties other datasource properties
     */
    public JdbcOptionsBean(String jdbcUrl, String username, String password, Boolean autoCommit,
            Long connectionTimeout, Long idleTimeout, Long maxLifetime, Long minimumIdle,
            Long maximumPoolSize, String poolName, Map<String, Object> dsProperties) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.autoCommit = autoCommit;
        this.connectionTimeout = connectionTimeout;
        this.idleTimeout = idleTimeout;
        this.maxLifetime = maxLifetime;
        this.minimumIdle = minimumIdle;
        this.maximumPoolSize = maximumPoolSize;
        this.poolName = poolName;
        this.dsProperties = dsProperties;
    }

    /**
     * @return the jdbcUrl
     */
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * @param jdbcUrl the jdbcUrl to set
     */
    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the autoCommit
     */
    public Boolean isAutoCommit() {
        return autoCommit;
    }

    /**
     * @param autoCommit the autoCommit to set
     */
    public void setAutoCommit(Boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    /**
     * @return the connectionTimeout
     */
    public Long getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @param connectionTimeout the connectionTimeout to set
     */
    public void setConnectionTimeout(Long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * @return the idleTimeout
     */
    public Long getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * @param idleTimeout the idleTimeout to set
     */
    public void setIdleTimeout(Long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    /**
     * @return the maxLifetime
     */
    public Long getMaxLifetime() {
        return maxLifetime;
    }

    /**
     * @param maxLifetime the maxLifetime to set
     */
    public void setMaxLifetime(Long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    /**
     * @return the minimumIdle
     */
    public Long getMinimumIdle() {
        return minimumIdle;
    }

    /**
     * @param minimumIdle the minimumIdle to set
     */
    public void setMinimumIdle(Long minimumIdle) {
        this.minimumIdle = minimumIdle;
    }

    /**
     * @return the maximumPoolSize
     */
    public Long getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * @param maximumPoolSize the maximumPoolSize to set
     */
    public void setMaximumPoolSize(Long maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    /**
     * Set any uncommon properties using the DataSource Properties Map
     *
     * @return the dsProperties
     */
    public Map<String, Object> getDsProperties() {
        return dsProperties;
    }

    /**
     * Set any uncommon properties using the DataSource Properties Map
     *
     * @param dsProperties the dsProperties to set
     */
    public void setDsProperties(Map<String, Object> dsProperties) {
        this.dsProperties = dsProperties;
    }

    /**
     * @return the poolName
     */
    public String getPoolName() {
        return poolName;
    }

    /**
     * @param poolName the poolName to set
     */
    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

}
