package com.cloudstore.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class S3FileRepositoryTest {

    private static final String BUCKET_NAME = "test-cloudstore-bucket";

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private S3FileRepository fileRepository;

    @BeforeEach
    public void setUp() {
        fileRepository = new S3FileRepository(s3Presigner, BUCKET_NAME);
    }

    @Test
    public void testGenerateUploadUrl() throws Exception {
        URL expectedUrl = URI.create("https://test-cloudstore-bucket.s3.amazonaws.com/user1/doc.pdf?sig=123").toURL();
        when(presignedPutObjectRequest.url()).thenReturn(expectedUrl);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedPutObjectRequest);

        String result = fileRepository.generateUploadUrl("user1/doc.pdf", "application/pdf");

        assertEquals(expectedUrl.toString(), result);

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());
        PutObjectPresignRequest captured = captor.getValue();
        assertEquals(BUCKET_NAME, captured.putObjectRequest().bucket());
        assertEquals("user1/doc.pdf", captured.putObjectRequest().key());
        assertEquals("application/pdf", captured.putObjectRequest().contentType());
    }

    @Test
    public void testGenerateDownloadUrl() throws Exception {
        URL expectedUrl = URI.create("https://test-cloudstore-bucket.s3.amazonaws.com/user1/doc.pdf?sig=456").toURL();
        when(presignedGetObjectRequest.url()).thenReturn(expectedUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);

        String result = fileRepository.generateDownloadUrl("user1/doc.pdf");

        assertEquals(expectedUrl.toString(), result);

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        GetObjectPresignRequest captured = captor.getValue();
        assertEquals(BUCKET_NAME, captured.getObjectRequest().bucket());
        assertEquals("user1/doc.pdf", captured.getObjectRequest().key());
    }
}
