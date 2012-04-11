/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.example;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

/**
 * @version
 */
public class DataFormatConcurrentTest extends CamelTestSupport {

    private int size = 2000;

    @Test
    public void testUnmarshallConcurrent() throws Exception {
        int counter = 10000;
        final String payload = "<purchaseOrder name='Wine' amount='123.45' price='2.22'/>";
        final CountDownLatch latch = new CountDownLatch(counter);
        template.setDefaultEndpointUri("direct:unmarshal");

        ExecutorService pool = Executors.newFixedThreadPool(20);
        //long start = System.currentTimeMillis();
        for (int i = 0; i < counter; i++) {
            pool.execute(new Runnable() {
                public void run() {
                    template.sendBody(payload);
                    latch.countDown();
                }
            });
        }

        // should finish on fast machines in less than 3 seconds
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        //long end = System.currentTimeMillis();
        //System.out.println("took " + (end - start) + "ms");
    }

    @Test
    public void testUnmarshallFallbackConcurrent() throws Exception {
        int counter = 10000;
        final String payload = "<purchaseOrder name='Wine' amount='123.45' price='2.22'/>";
        final CountDownLatch latch = new CountDownLatch(counter);
        template.setDefaultEndpointUri("direct:unmarshalFallback");

        ExecutorService pool = Executors.newFixedThreadPool(20);
        //long start = System.currentTimeMillis();
        for (int i = 0; i < counter; i++) {
            pool.execute(new Runnable() {
                public void run() {
                    template.sendBody(payload);
                    latch.countDown();
                }
            });
        }

        // should finish on fast machines in less than 3 seconds
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        //long end = System.currentTimeMillis();
        //System.out.println("took " + (end - start) + "ms");
    }

    @Test
    public void testMarshallFallbackConcurrent() throws Exception {
        int counter = 10000;
        final PurchaseOrder order = new PurchaseOrder();
        order.setName("Wine");
        order.setAmount(123.45);
        order.setPrice(2.22);
        final CountDownLatch latch = new CountDownLatch(counter);
        template.setDefaultEndpointUri("direct:marshalFallback");

        ExecutorService pool = Executors.newFixedThreadPool(20);
        //long start = System.currentTimeMillis();
        for (int i = 0; i < counter; i++) {
            pool.execute(new Runnable() {
                public void run() {
                    template.sendBody(order);
                    latch.countDown();
                }
            });
        }

        // should finish on fast machines in less than 3 seconds
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        //long end = System.currentTimeMillis();
        //System.out.println("took " + (end - start) + "ms");
    }

    @Test
    public void testMarshallConcurrent() throws Exception {
        int counter = 10000;
        final PurchaseOrder order = new PurchaseOrder();
        order.setName("Wine");
        order.setAmount(123.45);
        order.setPrice(2.22);
        final CountDownLatch latch = new CountDownLatch(counter);
        template.setDefaultEndpointUri("direct:marshal");

        ExecutorService pool = Executors.newFixedThreadPool(20);
        //long start = System.currentTimeMillis();
        for (int i = 0; i < counter; i++) {
            pool.execute(new Runnable() {
                public void run() {
                    template.sendBody(order);
                    latch.countDown();
                }
            });
        }

        // should finish on fast machines in less than 3 seconds
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        //long end = System.currentTimeMillis();
        //System.out.println("took " + (end - start) + "ms");
    }

    @Test
    public void testSendConcurrent() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(size);

        // wait for seda consumer to start up properly
        Thread.sleep(1000);

        ExecutorService executor = Executors.newCachedThreadPool();
        for (int i = 0; i < size; i++) {

            // sleep a little so we interleave with the marshaller
            try {
                Thread.sleep(1, 500);
            } catch (InterruptedException e) {
                // ignore
            }

            executor.execute(new Runnable() {
                public void run() {
                    PurchaseOrder bean = new PurchaseOrder();
                    bean.setName("Beer");
                    bean.setAmount(23);
                    bean.setPrice(2.5);

                    template.sendBody("seda:start?size=" + size + "&concurrentConsumers=5", bean);
                }
            });
        }

        assertMockEndpointsSatisfied();
    }

    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                DataFormat jaxb = new JaxbDataFormat("org.apache.camel.example");

                // use seda that supports concurrent consumers for concurrency
                from("seda:start?size=" + size + "&concurrentConsumers=5").marshal(jaxb).convertBodyTo(String.class).to("mock:result");

                from("direct:unmarshal")
                        .unmarshal(jaxb)
                        .to("mock:result");

                from("direct:marshal")
                        .marshal(jaxb)
                        .to("mock:result");

                from("direct:unmarshalFallback")
                        .convertBodyTo(PurchaseOrder.class)
                        .to("mock:result");

                from("direct:marshalFallback")
                        .convertBodyTo(String.class)
                        .to("mock:result");
            }
        };
    }

}