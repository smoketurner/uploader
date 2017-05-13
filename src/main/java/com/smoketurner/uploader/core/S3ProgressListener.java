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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

public class S3ProgressListener implements ProgressListener {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(S3ProgressListener.class);
    private final String key;
    private final long start;
    private final int count;
    private final int size;

    // metrics
    private final Timer uploadTime;
    private final Counter successCounter;
    private final Counter failedCounter;

    /**
     * Constructor
     *
     * @param key
     *            S3 key
     * @param start
     *            Start time in nanoseconds
     * @param count
     *            Number of events in the upload
     * @param size
     *            Size of the upload
     */
    public S3ProgressListener(@Nonnull final String key, final long start,
            final int count, final int size) {
        this.key = Objects.requireNonNull(key);
        this.start = start;
        this.count = count;
        this.size = size;

        final MetricRegistry registry = SharedMetricRegistries.getDefault();
        this.uploadTime = registry
                .timer(name(S3ProgressListener.class, "upload-time"));
        this.successCounter = registry
                .counter(name(S3ProgressListener.class, "upload-success"));
        this.failedCounter = registry
                .counter(name(S3ProgressListener.class, "upload-failed"));
    }

    @Override
    public void progressChanged(ProgressEvent progressEvent) {
        switch (progressEvent.getEventType()) {
        case CLIENT_REQUEST_FAILED_EVENT:
            LOGGER.debug("Failed API request for upload: {}", key);
            break;
        case CLIENT_REQUEST_RETRY_EVENT:
            LOGGER.debug("Retrying API request for upload: {}", key);
            break;
        case CLIENT_REQUEST_STARTED_EVENT:
            LOGGER.trace("Starting API request for upload: {}", key);
            break;
        case CLIENT_REQUEST_SUCCESS_EVENT:
            LOGGER.trace("Finished API request for upload: {}", key);
            break;
        case TRANSFER_CANCELED_EVENT:
            LOGGER.warn("Cancelled upload {}", key);
            break;
        case TRANSFER_COMPLETED_EVENT:
            final long took = System.nanoTime() - start;

            uploadTime.update(took, TimeUnit.NANOSECONDS);
            successCounter.inc();

            LOGGER.info(
                    "Finished uploading \"{}\" ({} events, {} bytes) in {}ms",
                    key, count, size, (took / 1000000));
            break;
        case TRANSFER_FAILED_EVENT:
            LOGGER.error("Failed to upload: {}", key);
            failedCounter.inc();
            break;
        case TRANSFER_PART_COMPLETED_EVENT:
            LOGGER.trace("Completed part for \"{}\" bytes: {}", key,
                    progressEvent.getBytes());
            break;
        case TRANSFER_PART_FAILED_EVENT:
            LOGGER.debug("Failed part for \"{}\" bytes: {}", key,
                    progressEvent.getBytes());
            break;
        case TRANSFER_PART_STARTED_EVENT:
            LOGGER.trace("Started part for \"{}\" bytes: {}", key,
                    progressEvent.getBytes());
            break;
        case TRANSFER_PREPARING_EVENT:
            LOGGER.debug("Preparing upload \"{}\" total size: {} bytes", key,
                    size);
            break;
        case TRANSFER_STARTED_EVENT:
            LOGGER.info("Started uploading: {}", key);
            break;
        case HTTP_REQUEST_COMPLETED_EVENT:
        case HTTP_REQUEST_CONTENT_RESET_EVENT:
        case HTTP_REQUEST_STARTED_EVENT:
        case HTTP_RESPONSE_COMPLETED_EVENT:
        case HTTP_RESPONSE_CONTENT_RESET_EVENT:
        case HTTP_RESPONSE_STARTED_EVENT:
        case REQUEST_BYTE_TRANSFER_EVENT:
        case REQUEST_CONTENT_LENGTH_EVENT:
        case RESPONSE_BYTE_DISCARD_EVENT:
        case RESPONSE_BYTE_TRANSFER_EVENT:
        case RESPONSE_CONTENT_LENGTH_EVENT:
        default:
            LOGGER.trace("Unknown progress event: {}", progressEvent);
            break;
        }
    }
}
