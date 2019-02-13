/*
 * Copyright © 2019 Smoke Turner, LLC (github@smoketurner.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.uploader.resources;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.smoketurner.uploader.core.Batch;
import com.smoketurner.uploader.core.Uploader;
import com.smoketurner.uploader.handler.AuthHandler;
import io.dropwizard.util.Size;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/v1/batch")
public class BatchResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(BatchResource.class);
  private final Uploader uploader;
  private final long maxUploadBytes;
  private final AtomicReference<Batch> curBatch = new AtomicReference<>();

  // metrics
  private final Meter eventMeter;

  /**
   * Constructor
   *
   * @param uploader Uploader
   * @param maxUploadSize Maximum size of AWS S3 file to upload
   */
  public BatchResource(final Uploader uploader, final Size maxUploadSize) {
    this.uploader = Objects.requireNonNull(uploader);
    this.maxUploadBytes = maxUploadSize.toBytes();

    final MetricRegistry registry = SharedMetricRegistries.getDefault();
    this.eventMeter = registry.meter(MetricRegistry.name(BatchResource.class, "event-rate"));
  }

  @POST
  @Consumes(MediaType.WILDCARD)
  public Response upload(@Context SecurityContext context, InputStream input) {

    final Optional<String> customerId = AuthHandler.getCustomerId(context.getUserPrincipal());
    if (!customerId.isPresent()) {
      throw new WebApplicationException("No customerId found in request");
    }

    final String custId = customerId.get();

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

      reader
          .lines()
          .map(line -> line.getBytes(StandardCharsets.UTF_8))
          .forEach(line -> processLine(custId, line));
    } catch (IOException e) {
      LOGGER.error("Unable to read input", e);
      throw new WebApplicationException("Unable to read input", e);
    }

    // Check for any remaining items in the batch
    final Batch batch = curBatch.get();
    if (batch != null && !batch.isEmpty()) {
      batch.finish();
      uploader.upload(batch);
      curBatch.set(null);
    }

    return Response.accepted().build();
  }

  /**
   * Process a line of input and add it to the batch
   *
   * @param customerId Customer ID
   * @param line Line of input
   */
  private void processLine(final String customerId, final byte[] line) {
    eventMeter.mark();

    try {
      final Batch batch = getBatch(customerId);

      batch.add(line);

      if (batch.size() > maxUploadBytes) {
        LOGGER.debug(
            "Batch size {} bytes exceeds max upload size of {} bytes",
            batch.size(),
            maxUploadBytes);

        batch.finish();
        uploader.upload(batch);
        curBatch.set(newBatch(customerId));
      }
    } catch (IOException e) {
      LOGGER.error("Unable to process line", e);
    }
  }

  /**
   * Get the latest batch or create a new batch
   *
   * @param customerId Customer ID
   * @return an existing batch or a new batch
   * @throws IOException if unable to create a batch
   */
  private Batch getBatch(final String customerId) throws IOException {
    final Batch batch = curBatch.get();
    if (batch != null) {
      return batch;
    }

    final Batch newBatch = newBatch(customerId);
    if (curBatch.compareAndSet(null, newBatch)) {
      return newBatch;
    }
    return getBatch(customerId);
  }

  /**
   * Create a new batch
   *
   * @param customerId Customer ID
   * @return new batch
   * @throws IOException if unable to create the batch
   */
  private Batch newBatch(final String customerId) throws IOException {
    LOGGER.debug("Creating new batch for: {}", customerId);
    return Batch.builder(customerId).withSize(maxUploadBytes).build();
  }
}
