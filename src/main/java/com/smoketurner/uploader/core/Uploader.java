/**
 * Copyright 2017 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.uploader.core;

import static com.codahale.metrics.MetricRegistry.name;
import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.event.ProgressListener.ExceptionReporter;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.annotations.VisibleForTesting;
import com.smoketurner.uploader.config.AwsConfiguration;

public class Uploader {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Uploader.class);
    private final AwsConfiguration configuration;

    // metrics
    private final Histogram batchSize;
    private final Histogram batchCount;

    // this isn't set in the constructor so we can configure the executor
    private TransferManager s3;

    /**
     * Constructor
     *
     * @param configuration
     *            AWS configuration
     */
    public Uploader(@Nonnull final AwsConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration);

        final MetricRegistry registry = SharedMetricRegistries
                .getOrCreate("default");

        this.batchSize = registry.histogram(name(Uploader.class, "batch-size"));
        this.batchCount = registry
                .histogram(name(Uploader.class, "batch-count"));
    }

    /**
     * Set the {@link TransferManager} to use for uploading to S3
     *
     * @param s3
     *            Transfer Manager
     */
    public void setTransferManager(final TransferManager s3) {
        this.s3 = Objects.requireNonNull(s3);
    }

    /**
     * Upload a batch to S3 using the TransferManager
     * 
     * @param batch
     *            Batch to upload
     */
    public void upload(@Nonnull final Batch batch) {
        batchSize.update(batch.size());
        batchCount.update(batch.getCount());

        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentEncoding("gzip");
        metadata.setContentType(MediaType.TEXT_PLAIN);
        metadata.setContentLength(batch.size());
        metadata.addUserMetadata("count", String.valueOf(batch.getCount()));
        if (batch.getCustomerId().isPresent()) {
            metadata.addUserMetadata("customer_id",
                    batch.getCustomerId().get());
        }

        String key = batch.getKey();
        if (configuration.getPrefix().isPresent()) {
            key = configuration.getPrefix().get() + "/" + key;
        }

        LOGGER.debug("Customer: {}, S3 key: {}",
                batch.getCustomerId().orElse(null), key);

        final S3ProgressListener listener = new S3ProgressListener(key,
                nanoTime(), batch.getCount(), batch.size());

        final ExceptionReporter reporter = ExceptionReporter.wrap(listener);

        try {
            final PutObjectRequest request = new PutObjectRequest(
                    configuration.getBucketName(), key, batch.getInputStream(),
                    metadata).withGeneralProgressListener(reporter);
            s3.upload(request);
        } catch (AmazonServiceException e) {
            LOGGER.error("Service error uploading to S3", e);
        } catch (AmazonClientException e) {
            LOGGER.error("Client error uploading to S3", e);
        } catch (IOException e) {
            LOGGER.error("Error uploading batch", e);
        }
    }

    @VisibleForTesting
    public long nanoTime() {
        return System.nanoTime();
    }
}
