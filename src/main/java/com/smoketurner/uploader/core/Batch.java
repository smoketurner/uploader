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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

public class Batch {

    private static final byte[] NEWLINE = "\n".getBytes(StandardCharsets.UTF_8);
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final AtomicInteger eventCount = new AtomicInteger(0);
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final GZIPOutputStream compressor;

    /**
     * Constructor
     *
     * @throws IOException
     */
    public Batch() throws IOException {
        compressor = new GZIPOutputStream(buffer, true);
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
}
