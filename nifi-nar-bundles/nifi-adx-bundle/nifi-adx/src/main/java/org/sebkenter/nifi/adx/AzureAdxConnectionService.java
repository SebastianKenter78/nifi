/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sebkenter.nifi.adx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.microsoft.azure.kusto.data.ConnectionStringBuilder;
import com.microsoft.azure.kusto.ingest.IngestClient;
import com.microsoft.azure.kusto.ingest.IngestClientFactory;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.InitializationException;

@Tags({ "Azure", "ADX", "Kusto", "ingest", "azure"})
@CapabilityDescription("Sends batches of flowfile content to an azure adx cluster.")
public class AzureAdxConnectionService extends AbstractControllerService implements AdxConnectionService {

    public static final PropertyDescriptor INGEST_URL = new PropertyDescriptor
            .Builder().name("INGEST_URL")
            .displayName("Ingest URL")
            .description("URL of the ingestion endpoint of the azure data explorer cluster.")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .addValidator(StandardValidators.URL_VALIDATOR)
            .build();

    public static final PropertyDescriptor APP_ID = new PropertyDescriptor
            .Builder().name("APP_ID")
            .displayName("Application ID")
            .description("Azure application ID for accessing the ADX-Cluster")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor APP_KEY = new PropertyDescriptor
            .Builder().name("APP_KEY")
            .displayName("Application KEY")
            .description("Azure application ID Key for accessing the ADX-Cluster")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor APP_TENANT = new PropertyDescriptor
            .Builder().name("APP_TENANT")
            .displayName("Application Tenant")
            .description("Azure application tenant for accessing the ADX-Cluster")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    private static final List<PropertyDescriptor> properties;

    static {
        final List<PropertyDescriptor> props = new ArrayList<>();
        props.add(INGEST_URL);
        props.add(APP_ID);
        props.add(APP_KEY);
        props.add(APP_TENANT);
        properties = Collections.unmodifiableList(props);
    }

    private IngestClient _ingestClient;

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    /**
     * @param context
     *            the configuration context
     * @throws InitializationException
     *             if unable to create a database connection
     */
    @OnEnabled
    public void onEnabled(final ConfigurationContext context) throws InitializationException {
        ComponentLog log = getLogger();

        log.info("Starting Azure ADX Connection Service...");

        final String ingestUrl = context.getProperty(INGEST_URL).evaluateAttributeExpressions().getValue();
        final String app_id = context.getProperty(APP_ID).evaluateAttributeExpressions().getValue();
        final String app_key = context.getProperty(APP_KEY).evaluateAttributeExpressions().getValue();
        final String app_tenant = context.getProperty(APP_TENANT).evaluateAttributeExpressions().getValue();

        if(this._ingestClient != null) {
            onStopped();
        }

        this._ingestClient = createAdxClient(ingestUrl, app_id, app_key, app_tenant);
    }

    @OnStopped
    public final void onStopped() {
        if (this._ingestClient != null) {
            try {
                this._ingestClient.close();
            } catch(IOException e) {
                getLogger().error("Closing Azure ADX Client failed with: " + e.getMessage(), e);
            } finally {
                this._ingestClient = null;
            }
        }
    }

    @OnDisabled
    public void shutdown() {

    }

    @Override
    public void execute() throws ProcessException {

    }

    protected IngestClient createAdxClient(String ingestUrl, String appId, String appKey, String appTenant)
    {
        IngestClient client;

        try {
            ConnectionStringBuilder csb =
                    ConnectionStringBuilder.createWithAadApplicationCredentials(ingestUrl, appId, appKey, appTenant);

            try {
                client = IngestClientFactory.createClient(csb);
            } catch (Exception e) {
                throw new ProcessException(e);
            }

        } catch (Exception e) {
            throw new ProcessException(e);
        }

        return client;
    }
    @Override
    public IngestClient getAdxClient() {
        return this._ingestClient;
    }

}
