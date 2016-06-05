/**
 * Copyright 2016 Smoke Turner, LLC.
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
package com.smoketurner.uploader.application.core;

import static com.codahale.metrics.MetricRegistry.name;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.ws.rs.core.MediaType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.annotations.VisibleForTesting;
import com.smoketurner.uploader.application.config.AwsConfiguration;

public class Uploader {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Uploader.class);
    private static final DateTimeFormatter KEY_DATE_FORMAT = DateTimeFormat
            .forPattern("yyyy/MM/dd/HH/mm/ss");
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
        metadata.setContentType(MediaType.APPLICATION_JSON);
        metadata.setContentLength(batch.size());
        metadata.addUserMetadata("count", String.valueOf(batch.getCount()));

        final String key = generateKey();
        LOGGER.debug("S3 key: {}", key);

        final S3ProgressListener listener = new S3ProgressListener(key,
                nanoTime(), batch.getCount(), batch.size());

        try {
            final PutObjectRequest request = new PutObjectRequest(
                    configuration.getBucketName(), key, batch.getInputStream(),
                    metadata).withGeneralProgressListener(listener);
            s3.upload(request);
        } catch (AmazonServiceException e) {
            LOGGER.error("Service error uploading to S3", e);
        } catch (AmazonClientException e) {
            LOGGER.error("Client error uploading to S3", e);
        } catch (IOException e) {
            LOGGER.error("Error uploading batch", e);
        }
    }

    /**
     * Generate a key for the events
     *
     * @return a generated S3 key
     */
    public String generateKey() {
        final String key = String.format("%s/events_%s.log.gz",
                now().toString(KEY_DATE_FORMAT), nanoTime());
        if (configuration.getPrefix().isPresent()) {
            return String.format("%s/%s-%s", configuration.getPrefix().get(),
                    getHash(key), key);
        }
        return String.format("%s-%s", getHash(key), key);
    }

    /**
     * Generate a MD5 hash for a string and return the first characters,
     * otherwise an underscore character.
     *
     * @param str
     *            Key to compute the hash against
     * @return Hash character
     */
    public static String getHash(final String str) {
        try {
            final MessageDigest msg = MessageDigest.getInstance("md5");
            msg.update(str.getBytes(StandardCharsets.UTF_8), 0, str.length());
            return new BigInteger(1, msg.digest()).toString(16).substring(0, 1);
        } catch (NoSuchAlgorithmException ignore) {
            return "_";
        }
    }

    @VisibleForTesting
    public DateTime now() {
        return DateTime.now(DateTimeZone.UTC);
    }

    @VisibleForTesting
    public long nanoTime() {
        return System.nanoTime();
    }
}
