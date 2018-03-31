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
package com.smoketurner.uploader.managed;

import java.util.Objects;
import javax.annotation.Nonnull;
import io.dropwizard.lifecycle.Managed;
import software.amazon.awssdk.services.s3.S3Client;

public class AmazonS3Manager implements Managed {

    private final S3Client s3;

    /**
     * Constructor
     *
     * @param s3
     *            AmazonS3 instance to manage
     */
    public AmazonS3Manager(@Nonnull final S3Client s3) {
        this.s3 = Objects.requireNonNull(s3);
    }

    @Override
    public void start() throws Exception {
        // nothing to start
    }

    @Override
    public void stop() throws Exception {
        s3.close();
    }
}
