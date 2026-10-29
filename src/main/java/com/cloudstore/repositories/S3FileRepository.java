package com.cloudstore.repositories;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

/**
 * Amazon S3 implementation of FileRepository using pre-signed URLs.
 */
public class S3FileRepository implements FileRepository {

    private static final Duration DEFAULT_EXPIRATION = Duration.ofMinutes(15);

    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3FileRepository() {
        this(createDefaultPresigner(), System.getenv("FILES_BUCKET_NAME"));
    }

    public S3FileRepository(S3Presigner s3Presigner, String bucketName) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    private static S3Presigner createDefaultPresigner() {
        String region = System.getenv("AWS_REGION");
        if (region != null && !region.isBlank()) {
            return S3Presigner.builder().region(Region.of(region)).build();
        }
        return S3Presigner.create();
    }

    @Override
    public String generateUploadUrl(String fileKey, String contentType) {
        PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey);

        if (contentType != null && !contentType.isBlank()) {
            putObjectRequestBuilder.contentType(contentType);
        }

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(DEFAULT_EXPIRATION)
                .putObjectRequest(putObjectRequestBuilder.build())
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    @Override
    public String generateDownloadUrl(String fileKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(DEFAULT_EXPIRATION)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }
}
