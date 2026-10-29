package com.cloudstore.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cloudstore.repositories.FileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GetFileHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FileRepository fileRepository;

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    private GetFileHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new GetFileHandler(fileRepository);
    }

    private APIGatewayProxyRequestEvent createAuthenticatedRequest(String userId) {
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();

        if (userId != null) {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", userId);
            Map<String, Object> authorizer = new HashMap<>();
            authorizer.put("claims", claims);
            APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
            context.setAuthorizer(authorizer);
            input.setRequestContext(context);
        }

        return input;
    }

    @Test
    public void testHandleRequest_SuccessWithPathParameters() throws Exception {
        String userId = "cust-123";
        String fileName = "lease-agreement.pdf";
        String expectedUrl = "https://s3.amazonaws.com/bucket/cust-123/lease-agreement.pdf?signature=xyz";

        when(fileRepository.generateDownloadUrl("cust-123/lease-agreement.pdf"))
                .thenReturn(expectedUrl);

        APIGatewayProxyRequestEvent request = createAuthenticatedRequest(userId);
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("fileName", fileName);
        request.setPathParameters(pathParams);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(200, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("lease-agreement.pdf", responseBody.get("fileName"));
        assertEquals("cust-123/lease-agreement.pdf", responseBody.get("fileKey"));
        assertEquals(expectedUrl, responseBody.get("downloadUrl"));

        verify(fileRepository).generateDownloadUrl("cust-123/lease-agreement.pdf");
    }

    @Test
    public void testHandleRequest_SuccessWithQueryParameters() throws Exception {
        String userId = "cust-123";
        String fileName = "photo.jpg";
        String expectedUrl = "https://s3.amazonaws.com/bucket/cust-123/photo.jpg?signature=xyz";

        when(fileRepository.generateDownloadUrl("cust-123/photo.jpg"))
                .thenReturn(expectedUrl);

        APIGatewayProxyRequestEvent request = createAuthenticatedRequest(userId);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("fileName", fileName);
        request.setQueryStringParameters(queryParams);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(200, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("photo.jpg", responseBody.get("fileName"));
        assertEquals("cust-123/photo.jpg", responseBody.get("fileKey"));
        assertEquals(expectedUrl, responseBody.get("downloadUrl"));
    }

    @Test
    public void testHandleRequest_PathTraversalSanitization() throws Exception {
        String userId = "cust-123";
        String maliciousFileName = "../other-user/secret.pdf";
        String expectedUrl = "https://s3.amazonaws.com/bucket/cust-123/secret.pdf?signature=xyz";

        when(fileRepository.generateDownloadUrl("cust-123/secret.pdf"))
                .thenReturn(expectedUrl);

        APIGatewayProxyRequestEvent request = createAuthenticatedRequest(userId);
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("fileName", maliciousFileName);
        request.setPathParameters(pathParams);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(200, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("secret.pdf", responseBody.get("fileName"));
        assertEquals("cust-123/secret.pdf", responseBody.get("fileKey"));
        assertFalse(responseBody.get("fileKey").contains(".."));
    }

    @Test
    public void testHandleRequest_Unauthorized() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("fileName", "test.pdf");
        request.setPathParameters(pathParams);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Unauthorized"));
    }

    @Test
    public void testHandleRequest_MissingFileName() {
        APIGatewayProxyRequestEvent request = createAuthenticatedRequest("cust-123");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("fileName is required"));
    }

    @Test
    public void testHandleRequest_RepositoryError() {
        when(mockContext.getLogger()).thenReturn(mockLogger);
        when(fileRepository.generateDownloadUrl(anyString())).thenThrow(new RuntimeException("S3 error"));

        APIGatewayProxyRequestEvent request = createAuthenticatedRequest("cust-123");
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("fileName", "test.pdf");
        request.setPathParameters(pathParams);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Could not retrieve file"));
    }
}
