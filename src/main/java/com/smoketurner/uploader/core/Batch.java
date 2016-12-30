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
package com.smoketurner.uploader.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;

public final class Batch {

    private static final byte[] NEWLINE = "\n".getBytes(StandardCharsets.UTF_8);
    private static final DateTimeFormatter KEY_DATE_FORMAT = DateTimeFormat
            .forPattern("yyyy/MM/dd/HH/mm/ss");
    private final AtomicInteger eventCount = new AtomicInteger(0);
    private final AtomicBoolean finished = new AtomicBoolean(false);

    private final ByteArrayOutputStream buffer;
    private final Optional<String> customerId;
    private final GZIPOutputStream compressor;
    private final DateTime createdAt;

    /**
     * Constructor
     *
     * @param customerId
     *            Customer ID (may be null)
     * @param size
     *            Initial buffer size in bytes
     * @param createdAt
     *            Creation timestamp
     * @throws IOException
     */
    private Batch(@Nullable final String customerId, final int size,
            final DateTime createdAt) throws IOException {
        this.customerId = Optional.ofNullable(customerId);
        this.createdAt = Objects.requireNonNull(createdAt);
        buffer = new ByteArrayOutputStream(size);
        compressor = new GZIPOutputStream(buffer, true);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String customerId;
        private int size = 32;
        private DateTime createdAt = DateTime.now(DateTimeZone.UTC);

        public Builder withCustomerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder withSize(long size) {
            this.size = Ints.checkedCast(size);
            return this;
        }

        public Builder withCreatedAt(DateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Batch build() throws IOException {
            return new Batch(customerId, size, createdAt);
        }
    }

    public void add(final byte[] event) throws IOException {
        if (finished.get()) {
            throw new IllegalStateException(
                    "Unable to add event to finished batch");
        }
        if (eventCount.get() > 0) {
            compressor.write(NEWLINE);
        }
        compressor.write(event);
        eventCount.incrementAndGet();
        compressor.flush();
    }

    public void finish() throws IOException {
        if (finished.compareAndSet(false, true)) {
            compressor.close();
            buffer.close();
        }
    }

    public Optional<String> getCustomerId() {
        return customerId;
    }

    public String getKey() {
        final String datePart = createdAt.toString(KEY_DATE_FORMAT);
        final String key = String.format("%s/events_%s.log.gz", datePart,
                createdAt.getMillis());
        return String.format("%s/%s-%s", customerId.orElse("none"),
                getHash(key, 1), key);
    }

    public int getCount() {
        return eventCount.get();
    }

    public int size() {
        return buffer.size();
    }

    public boolean isEmpty() {
        return eventCount.get() == 0;
    }

    public boolean isFinished() {
        return finished.get();
    }

    public InputStream getInputStream() throws IOException {
        finish();
        return new ByteArrayInputStream(buffer.toByteArray());
    }

    /**
     * Generate a MD5 hash for a string and return the first characters,
     * otherwise an underscore character.
     *
     * @param str
     *            Key to compute the hash against
     * @param length
     *            Length of hash to return
     * @return Hash character
     */
    public static String getHash(final String str, final int length) {
        try {
            final MessageDigest msg = MessageDigest.getInstance("md5");
            msg.update(str.getBytes(StandardCharsets.UTF_8), 0, str.length());
            return new BigInteger(1, msg.digest()).toString(16).substring(0,
                    length);
        } catch (NoSuchAlgorithmException ignore) {
            return Strings.repeat("_", length);
        }
    }
}
