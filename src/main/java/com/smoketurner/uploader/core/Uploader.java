/**
 * Copyright 2018 Smoke Turner, LLC.
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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.smoketurner.uploader.config.AwsConfiguration;
import io.dropwizard.util.Duration;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

public class Uploader {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Uploader.class);
    private static final long NANOS_IN_MILLIS = Duration.milliseconds(1)
            .toNanoseconds();

    private final S3AsyncClient s3;
    private final AwsConfiguration configuration;

    // metrics
    private final Histogram batchSize;
    private final Histogram batchCount;
    private final Timer uploadTime;
    private final Counter successCounter;
    private final Counter failedCounter;

    private Supplier<Long> currentTimeProvider = System::nanoTime;

    /**
     * Constructor
     *
     * @param s3
     *            S3 client
     * @param configuration
     *            AWS configuration
     */
    public Uploader(@Nonnull final S3AsyncClient s3,
            @Nonnull final AwsConfiguration configuration) {
        this.s3 = Objects.requireNonNull(s3);
        this.configuration = Objects.requireNonNull(configuration);

        final MetricRegistry registry = SharedMetricRegistries.getDefault();

        this.batchSize = registry.histogram(name(Uploader.class, "batch-size"));
        this.batchCount = registry
                .histogram(name(Uploader.class, "batch-count"));
        this.uploadTime = registry.timer(name(Uploader.class, "upload-time"));
        this.successCounter = registry
                .counter(name(Uploader.class, "upload-success"));
        this.failedCounter = registry
                .counter(name(Uploader.class, "upload-failed"));
    }

    /**
     * Upload a batch to S3
     * 
     * @param batch
     *            Batch to upload
     */
    public void upload(@Nonnull final Batch batch) {
        batchSize.update(batch.size());
        batchCount.update(batch.getCount());

        final ImmutableMap.Builder<String, String> builder = ImmutableMap
                .<String, String>builder()
                .put("count", String.valueOf(batch.getCount()));
        batch.getCustomerId().ifPresent(id -> builder.put("customer_id", id));
        final Map<String, String> metadata = builder.build();

        final String key;
        if (configuration.getPrefix().isPresent()) {
            key = configuration.getPrefix().get() + "/" + batch.getKey();
        } else {
            key = batch.getKey();
        }

        LOGGER.debug("Customer: {}, S3 key: {}",
                batch.getCustomerId().orElse(null), key);

        final PutObjectRequest request = PutObjectRequest.builder()
                .bucket(configuration.getBucketName()).key(key)
                .metadata(metadata).contentLength(batch.size())
                .contentType(MediaType.TEXT_PLAIN).contentEncoding("gzip")
                .serverSideEncryption(ServerSideEncryption.AES256).build();

        final long start = currentTimeProvider.get();

        final CompletableFuture<PutObjectResponse> future = s3
                .putObject(request, new BatchRequestProvider(batch));
        future.whenComplete((resp, err) -> {
            if (resp != null) {
                final long took = currentTimeProvider.get() - start;

                uploadTime.update(took, TimeUnit.NANOSECONDS);
                successCounter.inc();

                LOGGER.info(
                        "Finished uploading \"{}\" ({} events, {} bytes) in {}ms",
                        key, batch.getCount(), batch.size(),
                        (took / NANOS_IN_MILLIS));
            } else {
                failedCounter.inc();
                LOGGER.error(String.format("Failed to upload \"%s\"", key),
                        err);
            }
        });

    }

    @VisibleForTesting
    void setCurrentTimeProvider(Supplier<Long> provider) {
        this.currentTimeProvider = provider;
    }
}
