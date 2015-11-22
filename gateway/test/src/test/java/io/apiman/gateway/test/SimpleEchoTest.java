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
package io.apiman.gateway.test;

import io.apiman.gateway.engine.metrics.RequestMetric;
import io.apiman.gateway.test.junit.GatewayRestTestPlan;
import io.apiman.gateway.test.junit.GatewayRestTester;
import io.apiman.gateway.test.server.TestMetrics;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.runner.RunWith;

/**
 * Make sure the gateway and test echo server are working.
 *
 * @author eric.wittmann@redhat.com
 */
@SuppressWarnings("nls")
@RunWith(GatewayRestTester.class)
@GatewayRestTestPlan("test-plans/simple/simple-echo-testPlan.xml")
public class SimpleEchoTest {

    @AfterClass
    public static void after() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new Error(e);
        }
        
        List<RequestMetric> metrics = TestMetrics.getMetrics();
        Assert.assertNotNull(metrics);
        Assert.assertEquals(10, metrics.size());
        RequestMetric metric = metrics.get(1);
        Assert.assertEquals("SimpleEchoTest", metric.getServiceOrgId());
        Assert.assertEquals("echo", metric.getServiceId());
        Assert.assertEquals("1.0.0", metric.getServiceVersion());

        Assert.assertEquals("SimpleEchoTest", metric.getApplicationOrgId());
        Assert.assertEquals("test", metric.getApplicationId());
        Assert.assertEquals("1.0.0", metric.getApplicationVersion());

        Assert.assertEquals("12345", metric.getContractId());

        Assert.assertTrue("Expected the request duration to be at least 0 ms but was " + metric.getRequestDuration(),
                metric.getRequestDuration() >= 0);
        Assert.assertTrue("Expected the service duration to be at least 0 ms but was " + metric.getServiceDuration(),
                metric.getServiceDuration() >= 0);

        Assert.assertEquals(200, metric.getResponseCode());
        Assert.assertEquals("OK", metric.getResponseMessage());
    }

}
