package com.smoketurner.uploader.health;

import java.util.Objects;
import javax.annotation.Nonnull;
import com.codahale.metrics.health.HealthCheck;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

public class AmazonS3HealthCheck extends HealthCheck {

    private final S3Client s3;
    private final String bucket;

    /**
     * Constructor
     *
     * @param s3
     *            Amazon S3 client
     * @param bucker
     *            S3 bucket name
     */
    public AmazonS3HealthCheck(@Nonnull final S3Client s3,
            @Nonnull final String bucket) {
        this.s3 = Objects.requireNonNull(s3);
        this.bucket = Objects.requireNonNull(bucket);
    }

    @Override
    protected Result check() throws Exception {
        final HeadBucketRequest request = HeadBucketRequest.builder()
                .bucket(bucket).build();
        s3.headBucket(request);
        return Result.healthy();
    }
}
