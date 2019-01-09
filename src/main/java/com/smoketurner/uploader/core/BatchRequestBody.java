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
package com.smoketurner.uploader.core;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;

public class BatchRequestBody implements AsyncRequestBody {

  private final Batch batch;

  /**
   * Constructor
   *
   * @param batch Batch
   */
  public BatchRequestBody(final Batch batch) {
    this.batch = Objects.requireNonNull(batch);
  }

  @Override
  public Optional<Long> contentLength() {
    return Optional.of(batch.size());
  }

  @Override
  public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
    subscriber.onSubscribe(
        new Subscription() {
          @Override
          public void request(long n) {
            if (n > 0) {
              subscriber.onNext(ByteBuffer.wrap(batch.toByteArray()));
              subscriber.onComplete();
            }
          }

          @Override
          public void cancel() {}
        });
  }
}
