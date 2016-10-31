/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.hc.core5.http.examples;

import java.io.ByteArrayInputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ConnectionReuseStrategy;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.impl.HttpProcessors;
import org.apache.hc.core5.http.impl.DefaultConnectionReuseStrategy;
import org.apache.hc.core5.http.impl.io.DefaultBHttpClientConnection;
import org.apache.hc.core5.http.impl.io.HttpRequestExecutor;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.apache.hc.core5.http.protocol.HttpProcessor;

/**
 * Elemental example for executing multiple POST requests sequentially.
 */
public class ElementalHttpPost {

    public static void main(String[] args) throws Exception {
        HttpProcessor httpproc = HttpProcessors.client();
        HttpRequestExecutor httpexecutor = new HttpRequestExecutor();

        HttpCoreContext coreContext = HttpCoreContext.create();
        HttpHost host = new HttpHost("localhost", 8080);

        DefaultBHttpClientConnection conn = new DefaultBHttpClientConnection(8 * 1024);
        ConnectionReuseStrategy connStrategy = DefaultConnectionReuseStrategy.INSTANCE;

        try {

            HttpEntity[] requestBodies = {
                    new StringEntity(
                            "This is the first test request",
                            ContentType.create("text/plain", StandardCharsets.UTF_8)),
                    new ByteArrayEntity(
                            "This is the second test request".getBytes(StandardCharsets.UTF_8),
                            ContentType.APPLICATION_OCTET_STREAM),
                    new InputStreamEntity(
                            new ByteArrayInputStream(
                                    "This is the third test request (will be chunked)"
                                    .getBytes(StandardCharsets.UTF_8)),
                            ContentType.APPLICATION_OCTET_STREAM)
            };

            for (int i = 0; i < requestBodies.length; i++) {
                if (!conn.isOpen()) {
                    Socket socket = new Socket(host.getHostName(), host.getPort());
                    conn.bind(socket);
                }
                ClassicHttpRequest request = new BasicClassicHttpRequest("POST", host,
                        "/servlets-examples/servlet/RequestInfoExample");
                request.setEntity(requestBodies[i]);
                System.out.println(">> Request URI: " + request.getUri());

                httpexecutor.preProcess(request, httpproc, coreContext);
                ClassicHttpResponse response = httpexecutor.execute(request, conn, coreContext);
                httpexecutor.postProcess(response, httpproc, coreContext);

                System.out.println("<< Response: " + response.getCode());
                System.out.println(EntityUtils.toString(response.getEntity()));
                System.out.println("==============");
                if (!connStrategy.keepAlive(request, response, coreContext)) {
                    conn.close();
                } else {
                    System.out.println("Connection kept alive...");
                }
            }
        } finally {
            conn.close();
        }
    }

}