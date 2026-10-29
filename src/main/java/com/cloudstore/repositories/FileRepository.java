package com.cloudstore.repositories;

/**
 * Repository interface for managing customer file storage in S3.
 */
public interface FileRepository {

    /**
     * Generates a pre-signed URL to upload a file.
     *
     * @param fileKey the S3 object key
     * @param contentType the optional MIME type of the file (can be null or blank)
     * @return the pre-signed upload URL as a string
     */
    String generateUploadUrl(String fileKey, String contentType);

    /**
     * Generates a pre-signed URL to retrieve / download a file.
     *
     * @param fileKey the S3 object key
     * @return the pre-signed download URL as a string
     */
    String generateDownloadUrl(String fileKey);
}
