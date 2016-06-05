package com.smoketurner.uploader.application.health;

import java.util.Objects;
import javax.annotation.Nonnull;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Owner;
import com.codahale.metrics.health.HealthCheck;

public class AmazonS3HealthCheck extends HealthCheck {

    private final AmazonS3Client s3;

    /**
     * Constructor
     *
     * @param s3
     *            Amazon S3 client
     */
    public AmazonS3HealthCheck(@Nonnull final AmazonS3Client s3) {
        this.s3 = Objects.requireNonNull(s3);
    }

    @Override
    protected Result check() throws Exception {
        final Owner owner;
        try {
            owner = s3.getS3AccountOwner();
        } catch (AmazonServiceException e) {
            return Result.unhealthy(e);
        } catch (AmazonClientException e) {
            return Result.unhealthy(e);
        }
        return Result.healthy(owner.toString());
    }
}
