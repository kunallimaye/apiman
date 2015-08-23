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
package io.apiman.gateway.platforms.vertx3.verticles;

import io.apiman.gateway.platforms.vertx3.common.verticles.VerticleType;
import io.apiman.gateway.platforms.vertx3.http.HttpExecutor;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;

/**
 * A HTTP gateway verticle
 *
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class HttpGatewayVerticle extends ApimanVerticleBase {
    public static final VerticleType VERTICLE_TYPE = VerticleType.HTTP;

    @Override
    public void start() {
        super.start();

        HttpServerOptions standardOptions = new HttpServerOptions()
            .setHost(apimanConfig.getHostname());

        vertx.createHttpServer(standardOptions)
            .requestHandler(this::requestHandler)
            .listen(apimanConfig.getPort(VERTICLE_TYPE));
    }

    public void requestHandler(HttpServerRequest req) {
        new HttpExecutor(vertx, log, false).handle(req);
    }

    @Override
    public VerticleType verticleType() {
        return VERTICLE_TYPE;
    }
}