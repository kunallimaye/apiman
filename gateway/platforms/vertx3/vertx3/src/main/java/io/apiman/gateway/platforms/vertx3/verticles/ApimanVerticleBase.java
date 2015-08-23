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
package io.apiman.gateway.platforms.vertx3.verticles;

import java.util.UUID;

import io.apiman.common.util.SimpleStringUtils;
import io.apiman.gateway.platforms.vertx3.common.config.VertxEngineConfig;
import io.apiman.gateway.platforms.vertx3.common.verticles.VerticleType;
import io.apiman.gateway.platforms.vertx3.i18n.Messages;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
/**
 * Standard base for all apiman verticles.
 *
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
@SuppressWarnings("nls")
public abstract class ApimanVerticleBase extends AbstractVerticle {

    protected VertxEngineConfig apimanConfig;
    protected String uuid = SimpleStringUtils.join(".", UUID.randomUUID().toString(), verticleType().name());
    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void start() {
        apimanConfig = getEngineConfig();

        log.info(Messages.getString("ApimanVerticleBase.starting_verticle") + this.getClass().getName() + "\n" +
                Messages.getString("ApimanVerticleBase.type") + verticleType() + "\n" +
                Messages.getString("ApimanVerticleBase.uuid") + uuid + "\n");
    }

    /**
     * Maps to config.
     * @return Verticle's type
     */
    public abstract VerticleType verticleType();

    // Override this for verticle specific config & testing.
    protected VertxEngineConfig getEngineConfig() {
        if (config().isEmpty()) {
            throw new IllegalStateException("No configuration provided!");
        }
        return new VertxEngineConfig(config());
    }

    protected String getUuid() {
        return uuid;
    }

    protected Logger getLogger() {
        return log;
    }
}
