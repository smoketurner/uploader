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
package com.smoketurner.uploader.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
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
import com.smoketurner.uploader.core.Batch;
import com.smoketurner.uploader.core.Uploader;
import com.smoketurner.uploader.handler.AuthHandler;

@Path("/v1/batch")
public class BatchResource {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(BatchResource.class);
    private final Uploader uploader;

    /**
     * Constructor
     *
     * @param uploader
     *            Uploader
     */
    public BatchResource(@Nonnull final Uploader uploader) {
        this.uploader = Objects.requireNonNull(uploader);
    }

    @POST
    @Consumes(MediaType.WILDCARD)
    public Response upload(@Context SecurityContext context,
            InputStream input) {

        final Optional<String> customerId = AuthHandler
                .getCustomerId(context.getUserPrincipal());
        if (!customerId.isPresent()) {
            throw new WebApplicationException("No customerId found in request");
        }

        final Batch batch;
        try {
            batch = Batch.create(customerId.get());
        } catch (IOException e) {
            LOGGER.error("Unable to create batch", e);
            throw new WebApplicationException("Unable to create batch", e);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            reader.lines().map(line -> line.getBytes(StandardCharsets.UTF_8))
                    .forEach(line -> {
                        try {
                            batch.add(line);
                        } catch (IOException e) {
                            LOGGER.error("Unable to process line", e);
                        }
                    });
        } catch (IOException e) {
            LOGGER.error("Unable to read input", e);
            throw new WebApplicationException("Unable to read input", e);
        }

        uploader.upload(batch);

        return Response.accepted().build();
    }
}
