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

import java.io.IOException;
import java.util.Map;

import io.apiman.gateway.engine.async.IAsyncResult;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.Application;
import io.apiman.gateway.engine.beans.Service;
import io.apiman.gateway.engine.es.beans.DataVersionBean;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Get;
import io.searchbox.core.Index;

/**
 * Extends the {@link ESRegistry} to provide multi-node caching.  This caching solution
 * will work in a cluster, although it is a rather naive implementation.  The approach
 * taken is that whenever the ES index is modified, a "last modified" record is set in
 * elasticsearch.  The registry utilizes a thread to periodically poll the ES store to
 * check if the data has been changed.  If the data *has* been changed, then the cache
 * is invalidated.
 *
 * @author eric.wittmann@redhat.com
 */
public class PollCachingESRegistry extends CachingESRegistry {

    private static final int DEFAULT_POLLING_INTERVAL = 10;

    private int pollIntervalSeconds;
    private boolean polling = false;
    private Object poller = new Object();
    private String dataVersion = null;

    /**
     * Constructor.
     */
    public PollCachingESRegistry(Map<String, String> config) {
        super(config);
        String intervalVal = config.get("cache-polling-interval"); //$NON-NLS-1$
        if (intervalVal != null) {
            pollIntervalSeconds = new Integer(intervalVal) * 1000;
        } else {
            pollIntervalSeconds = DEFAULT_POLLING_INTERVAL * 1000;
        }
        startCacheInvalidator();
    }

    /**
     * @see io.apiman.gateway.engine.es.CachingESRegistry#publishService(io.apiman.gateway.engine.beans.Service, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void publishService(Service service, final IAsyncResultHandler<Void> handler) {
        super.publishService(service, new IAsyncResultHandler<Void>() {
            @Override
            public void handle(IAsyncResult<Void> result) {
                if (result.isSuccess()) {
                    updateDataVersion();
                }
                handler.handle(result);
            }
        });
    }

    /**
     * @see io.apiman.gateway.engine.es.CachingESRegistry#retireService(io.apiman.gateway.engine.beans.Service, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void retireService(Service service, final IAsyncResultHandler<Void> handler) {
        super.retireService(service, new IAsyncResultHandler<Void>() {
            @Override
            public void handle(IAsyncResult<Void> result) {
                if (result.isSuccess()) {
                    updateDataVersion();
                }
                handler.handle(result);
            }
        });
    }

    /**
     * @see io.apiman.gateway.engine.es.CachingESRegistry#registerApplication(io.apiman.gateway.engine.beans.Application, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void registerApplication(Application application, final IAsyncResultHandler<Void> handler) {
        super.registerApplication(application, new IAsyncResultHandler<Void>() {
            /**
             * @see io.apiman.gateway.engine.async.IAsyncHandler#handle(java.lang.Object)
             */
            @Override
            public void handle(IAsyncResult<Void> result) {
                if (result.isSuccess()) {
                    updateDataVersion();
                }
                handler.handle(result);
            }
        });
    }

    /**
     * @see io.apiman.gateway.engine.es.CachingESRegistry#unregisterApplication(io.apiman.gateway.engine.beans.Application, io.apiman.gateway.engine.async.IAsyncResultHandler)
     */
    @Override
    public void unregisterApplication(Application application, final IAsyncResultHandler<Void> handler) {
        super.unregisterApplication(application, new IAsyncResultHandler<Void>() {
            @Override
            public void handle(IAsyncResult<Void> result) {
                if (result.isSuccess()) {
                    updateDataVersion();
                }
                handler.handle(result);
            }
        });
    }

    /**
     * Stores a "dataversion" record in the ES store.  There is only a single one of these.  The
     * return value of the add will include the version number of the entity.  This version
     * number is what we use to determine whether our cache is stale.
     */
    protected void updateDataVersion() {
        DataVersionBean dv = new DataVersionBean();
        dv.setUpdatedOn(System.currentTimeMillis());
        Index index = new Index.Builder(dv).refresh(false)
                .index(getIndexName())
                .type("dataVersion").id("instance").build(); //$NON-NLS-1$ //$NON-NLS-2$
        getClient().executeAsync(index, new JestResultHandler<JestResult>() {
            @Override
            public void completed(JestResult result) {
                dataVersion = null;
            }
            @Override
            public void failed(Exception e) {
                dataVersion = null;
            }
        });
    }

    /**
     * Starts up a thread that polls the ES store for updates.
     */
    protected void startCacheInvalidator() {
        polling = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (polling) {
                    try {
                        synchronized (poller) {
                            poller.wait(pollIntervalSeconds);
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                    checkCacheVersion();
                }
            }
        });
        thread.setDaemon(true);
        thread.setName("PollCachingESInvalidator"); //$NON-NLS-1$
        thread.start();
    }

    /**
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        polling = false;
        super.finalize();
    }

    /**
     * Checks the ES store to see if the 'dataVersion' entry has been updated with a newer
     * version #.  If it has, then we need to invalidate our cache.
     */
    protected void checkCacheVersion() {
        // Be very aggressive in invalidating the cache.
        boolean invalidate = true;
        try {
            Get get = new Get.Builder(getIndexName(), "instance").type("dataVersion").build(); //$NON-NLS-1$ //$NON-NLS-2$
            JestResult result = getClient().execute(get);
            if (result.isSucceeded()) {
                String latestDV = result.getJsonObject().get("_version").getAsString(); //$NON-NLS-1$
                if (latestDV != null && dataVersion != null && latestDV.equals(dataVersion)) {
                    invalidate = false;
                } else {
                    dataVersion = latestDV;
                }
            }
        } catch (IOException e) {
            // TODO need to use the gateway logger to log this!
            e.printStackTrace();
        }
        if (invalidate) {
            invalidateCache();
        }
    }

}
