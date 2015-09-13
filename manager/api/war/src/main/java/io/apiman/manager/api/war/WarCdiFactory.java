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
package io.apiman.manager.api.war;

import java.lang.reflect.Constructor;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;

import io.apiman.common.plugin.Plugin;
import io.apiman.common.plugin.PluginClassLoader;
import io.apiman.common.plugin.PluginCoordinates;
import io.apiman.common.util.ReflectionUtils;
import io.apiman.manager.api.core.IApiKeyGenerator;
import io.apiman.manager.api.core.IIdmStorage;
import io.apiman.manager.api.core.IMetricsAccessor;
import io.apiman.manager.api.core.IPluginRegistry;
import io.apiman.manager.api.core.IServiceCatalog;
import io.apiman.manager.api.core.IStorage;
import io.apiman.manager.api.core.IStorageQuery;
import io.apiman.manager.api.core.UuidApiKeyGenerator;
import io.apiman.manager.api.core.i18n.Messages;
import io.apiman.manager.api.core.logging.ApimanLogger;
import io.apiman.manager.api.core.logging.IApimanDelegateLogger;
import io.apiman.manager.api.core.logging.IApimanLogger;
import io.apiman.manager.api.core.logging.JsonLoggerImpl;
import io.apiman.manager.api.core.logging.StandardLoggerImpl;
import io.apiman.manager.api.core.noop.NoOpMetricsAccessor;
import io.apiman.manager.api.es.ESMetricsAccessor;
import io.apiman.manager.api.es.EsStorage;
import io.apiman.manager.api.jpa.JpaStorage;
import io.apiman.manager.api.jpa.roles.JpaIdmStorage;
import io.apiman.manager.api.security.ISecurityContext;
import io.apiman.manager.api.security.impl.DefaultSecurityContext;
import io.apiman.manager.api.security.impl.KeycloakSecurityContext;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.client.config.HttpClientConfig.Builder;

/**
 * Attempt to create producer methods for CDI beans.
 *
 * @author eric.wittmann@redhat.com
 */
@ApplicationScoped
public class WarCdiFactory {

    private static JestClient sStorageESClient;
    private static JestClient sMetricsESClient;
    private static EsStorage sESStorage;

    @Produces @ApimanLogger
    public static IApimanLogger provideLogger(WarApiManagerConfig config, InjectionPoint injectionPoint) {
        try {
            ApimanLogger logger = injectionPoint.getAnnotated().getAnnotation(ApimanLogger.class);
            Class<?> klazz = logger.value();
            return getDelegate(config).newInstance().createLogger(klazz);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(String.format(
                    Messages.i18n.format("LoggerFactory.InstantiationFailed")), e); //$NON-NLS-1$
        }
    }

    @Produces @ApplicationScoped
    public static ISecurityContext provideSecurityContext(WarApiManagerConfig config,
            @New DefaultSecurityContext defaultSC, @New KeycloakSecurityContext keycloakSC) {
        if ("default".equals(config.getSecurityContextType())) { //$NON-NLS-1$
            return defaultSC;
        } else if ("keycloak".equals(config.getSecurityContextType())) { //$NON-NLS-1$
            return keycloakSC;
        } else {
            throw new RuntimeException("Unknown security context type: " + config.getSecurityContextType()); //$NON-NLS-1$
        }
    }

    @Produces @ApplicationScoped
    public static IStorage provideStorage(WarApiManagerConfig config, @New JpaStorage jpaStorage,
            @New EsStorage esStorage, IPluginRegistry pluginRegistry) {
        IStorage storage = null;
        if ("jpa".equals(config.getStorageType())) { //$NON-NLS-1$
            storage = jpaStorage;
        } else if ("es".equals(config.getStorageType())) { //$NON-NLS-1$
            storage = initES(config, esStorage);
        } else {
            try {
                storage = createCustomComponent(IStorage.class, config.getStorageType(),
                        config.getStorageProperties(), pluginRegistry);
            } catch (Throwable t) {
                throw new RuntimeException("Error or unknown storage type: " + config.getStorageType(), t); //$NON-NLS-1$
            }
        }
        return storage;
    }

    @Produces @ApplicationScoped
    public static IStorageQuery provideStorageQuery(WarApiManagerConfig config, @New JpaStorage jpaStorage,
            @New EsStorage esStorage, IPluginRegistry pluginRegistry) {
        if ("jpa".equals(config.getStorageType())) { //$NON-NLS-1$
            return jpaStorage;
        } else if ("es".equals(config.getStorageType())) { //$NON-NLS-1$
            return initES(config, esStorage);
        } else {
            try {
                return createCustomComponent(IStorageQuery.class, config.getStorageQueryType(),
                        config.getStorageQueryProperties(), pluginRegistry);
            } catch (Throwable t) {
                throw new RuntimeException("Error or unknown storage query type: " + config.getStorageType(), t); //$NON-NLS-1$
            }
        }
    }

    @Produces @ApplicationScoped
    public static IMetricsAccessor provideMetricsAccessor(WarApiManagerConfig config,
            @New NoOpMetricsAccessor noopMetrics, @New ESMetricsAccessor esMetrics, IPluginRegistry pluginRegistry) {
        IMetricsAccessor metrics = null;
        if ("es".equals(config.getMetricsType())) { //$NON-NLS-1$
            metrics = esMetrics;
        } else {
            try {
                metrics = createCustomComponent(IMetricsAccessor.class, config.getMetricsType(),
                        config.getMetricsProperties(), pluginRegistry);
            } catch (Throwable t) {
                System.err.println("Unknown apiman metrics accessor type: " + config.getMetricsType()); //$NON-NLS-1$
                metrics = noopMetrics;
            }
        }
        return metrics;
    }

    @Produces @ApplicationScoped
    public static IApiKeyGenerator provideApiKeyGenerator(@New UuidApiKeyGenerator uuidApiKeyGen) {
        return uuidApiKeyGen;
    }

    @Produces @ApplicationScoped
    public static IServiceCatalog provideServiceCatalog(WarApiManagerConfig config, IPluginRegistry pluginRegistry) {
        try {
            return createCustomComponent(IServiceCatalog.class, config.getServiceCatalogType(),
                    config.getServiceCatalogProperties(), pluginRegistry);
        } catch (Throwable t) {
            throw new RuntimeException("Error or unknown service catalog type: " + config.getServiceCatalogType(), t); //$NON-NLS-1$
        }
    }

    @Produces @ApplicationScoped
    public static IIdmStorage provideIdmStorage(WarApiManagerConfig config, @New JpaIdmStorage jpaIdmStorage,
            @New EsStorage esStorage, IPluginRegistry pluginRegistry) {
        if ("jpa".equals(config.getStorageType())) { //$NON-NLS-1$
            return jpaIdmStorage;
        } else if ("es".equals(config.getStorageType())) { //$NON-NLS-1$
            return initES(config, esStorage);
        } else {
            try {
                return createCustomComponent(IIdmStorage.class, config.getIdmStorageType(),
                        config.getIdmStorageProperties(), pluginRegistry);
            } catch (Throwable t) {
                throw new RuntimeException("Error or unknown IDM storage type: " + config.getIdmStorageType(), t); //$NON-NLS-1$
            }
        }
    }

    @Produces @ApplicationScoped @Named("storage")
    public static JestClient provideStorageESClient(WarApiManagerConfig config) {
        if ("es".equals(config.getStorageType())) { //$NON-NLS-1$
            if (sStorageESClient == null) {
                sStorageESClient = createStorageJestClient(config);
            }
        }
        return sStorageESClient;
    }

    @Produces @ApplicationScoped @Named("metrics")
    public static JestClient provideMetricsESClient(WarApiManagerConfig config) {
        if ("es".equals(config.getMetricsType())) { //$NON-NLS-1$
            if (sMetricsESClient == null) {
                sMetricsESClient = createMetricsJestClient(config);
            }
        }
        return sMetricsESClient;
    }

    /**
     * @param config
     * @return create a new test ES client
     */
    private static JestClient createStorageJestClient(WarApiManagerConfig config) {
        StringBuilder builder = new StringBuilder();
        builder.append(config.getStorageESProtocol());
        builder.append("://"); //$NON-NLS-1$
        builder.append(config.getStorageESHost());
        builder.append(":"); //$NON-NLS-1$
        builder.append(config.getStorageESPort());
        String connectionUrl = builder.toString();
        JestClientFactory factory = new JestClientFactory();
        Builder httpConfig = new HttpClientConfig.Builder(connectionUrl).multiThreaded(true);
        String username = config.getStorageESUsername();
        String password = config.getStorageESPassword();
        if (username != null) {
            httpConfig.defaultCredentials(username, password);
        }
        factory.setHttpClientConfig(httpConfig.build());
        return factory.getObject();
    }

    /**
     * @param config
     * @return create a new test ES client
     */
    private static JestClient createMetricsJestClient(WarApiManagerConfig config) {
        StringBuilder builder = new StringBuilder();
        builder.append(config.getMetricsESProtocol());
        builder.append("://"); //$NON-NLS-1$
        builder.append(config.getMetricsESHost());
        builder.append(":"); //$NON-NLS-1$
        builder.append(config.getMetricsESPort());
        String connectionUrl = builder.toString();
        JestClientFactory factory = new JestClientFactory();
        Builder httpConfig = new HttpClientConfig.Builder(connectionUrl).multiThreaded(true);
        String username = config.getMetricsESUsername();
        String password = config.getMetricsESPassword();
        if (username != null) {
            httpConfig.defaultCredentials(username, password);
        }
        factory.setHttpClientConfig(httpConfig.build());
        return factory.getObject();
    }

    /**
     * Initializes the ES storage (if required).
     * @param config
     * @param esStorage
     */
    private static EsStorage initES(WarApiManagerConfig config, EsStorage esStorage) {
        if (sESStorage == null) {
            sESStorage = esStorage;
            if (config.isInitializeStorageES()) {
                sESStorage.initialize();
            }
        }
        return sESStorage;
    }

    /**
     * Creates a custom component from information found in the properties file.
     * @param componentType
     * @param componentSpec
     * @param configProperties
     * @param pluginRegistry
     */
    private static <T> T createCustomComponent(Class<T> componentType, String componentSpec,
            Map<String, String> configProperties, IPluginRegistry pluginRegistry) throws Exception {
        if (componentSpec == null) {
            throw new IllegalArgumentException("Null component type."); //$NON-NLS-1$
        }

        if (componentSpec.startsWith("class:")) { //$NON-NLS-1$
            Class<?> c = ReflectionUtils.loadClass(componentSpec.substring("class:".length())); //$NON-NLS-1$
            return createCustomComponent(componentType, c, configProperties);
        } else if (componentSpec.startsWith("plugin:")) { //$NON-NLS-1$
            PluginCoordinates coordinates = PluginCoordinates.fromPolicySpec(componentSpec);
            if (coordinates == null) {
                throw new IllegalArgumentException("Invalid plugin component spec: " + componentSpec); //$NON-NLS-1$
            }
            int ssidx = componentSpec.indexOf('/');
            if (ssidx == -1) {
                throw new IllegalArgumentException("Invalid plugin component spec: " + componentSpec); //$NON-NLS-1$
            }
            String classname = componentSpec.substring(ssidx + 1);
            Plugin plugin = pluginRegistry.loadPlugin(coordinates);
            PluginClassLoader classLoader = plugin.getLoader();
            Class<?> class1 = classLoader.loadClass(classname);
            return createCustomComponent(componentType, class1, configProperties);
        } else {
            Class<?> c = ReflectionUtils.loadClass(componentSpec);
            return createCustomComponent(componentType, c, configProperties);
        }
    }

    /**
     * Creates a custom component from a loaded class.
     * @param componentType
     * @param componentClass
     * @param configProperties
     */
    @SuppressWarnings("unchecked")
    private static <T> T createCustomComponent(Class<T> componentType, Class<?> componentClass,
            Map<String, String> configProperties) throws Exception {
        if (componentClass == null) {
            throw new IllegalArgumentException("Invalid component spec (class not found)."); //$NON-NLS-1$
        }
        try {
            Constructor<?> constructor = componentClass.getConstructor(Map.class);
            return (T) constructor.newInstance(configProperties);
        } catch (Exception e) {
        }
        return (T) componentClass.getConstructor().newInstance();
    }

    private static Class<? extends IApimanDelegateLogger> getDelegate(WarApiManagerConfig config) {
        if(config.getLoggerName() == null || StringUtils.isEmpty(config.getLoggerName())) {
            System.err.println(Messages.i18n.format("LoggerFactory.NoLoggerSpecified")); //$NON-NLS-1$
            return StandardLoggerImpl.class;
        }

        switch(config.getLoggerName().toLowerCase()) {
            case "json": //$NON-NLS-1$
                return JsonLoggerImpl.class;
            case "standard": //$NON-NLS-1$
                return StandardLoggerImpl.class;
            default:
                return loadByFQDN(config.getLoggerName());
        }

    }

    @SuppressWarnings("unchecked")
    private static Class<? extends IApimanDelegateLogger> loadByFQDN(String fqdn) {
        try {
            return (Class<? extends IApimanDelegateLogger>) Class.forName(fqdn);
        } catch (ClassNotFoundException e) {
            System.err.println(String.format(Messages.i18n.format("LoggerFactory.LoggerNotFoundOnClasspath"), //$NON-NLS-1$
                    fqdn));
            return StandardLoggerImpl.class;
        }
    }
}
