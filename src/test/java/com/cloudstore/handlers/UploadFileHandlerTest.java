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
public class UploadFileHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FileRepository fileRepository;

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    private UploadFileHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new UploadFileHandler(fileRepository);
    }

    private APIGatewayProxyRequestEvent createAuthenticatedRequest(String userId, String body) {
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setBody(body);

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
    public void testHandleRequest_Success() throws Exception {
        String userId = "cust-123";
        String fileName = "inventory.pdf";
        String expectedUrl = "https://s3.amazonaws.com/bucket/cust-123/inventory.pdf?signature=abc";

        when(fileRepository.generateUploadUrl("cust-123/inventory.pdf", "application/pdf"))
                .thenReturn(expectedUrl);

        APIGatewayProxyRequestEvent request = createAuthenticatedRequest(userId,
                "{\"fileName\":\"inventory.pdf\",\"contentType\":\"application/pdf\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(200, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("inventory.pdf", responseBody.get("fileName"));
        assertEquals("cust-123/inventory.pdf", responseBody.get("fileKey"));
        assertEquals(expectedUrl, responseBody.get("uploadUrl"));

        verify(fileRepository).generateUploadUrl("cust-123/inventory.pdf", "application/pdf");
    }

    @Test
    public void testHandleRequest_PathTraversalSanitization() throws Exception {
        String userId = "cust-123";
        String maliciousFileName = "../../secret.txt";
        String expectedUrl = "https://s3.amazonaws.com/bucket/cust-123/secret.txt?signature=abc";

        when(fileRepository.generateUploadUrl("cust-123/secret.txt", null))
                .thenReturn(expectedUrl);

        APIGatewayProxyRequestEvent request = createAuthenticatedRequest(userId,
                "{\"fileName\":\"" + maliciousFileName + "\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(200, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = objectMapper.readValue(response.getBody(), Map.class);
        assertEquals("secret.txt", responseBody.get("fileName"));
        assertEquals("cust-123/secret.txt", responseBody.get("fileKey"));
        assertFalse(responseBody.get("fileKey").contains(".."));
    }

    @Test
    public void testHandleRequest_Unauthorized() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody("{\"fileName\":\"test.png\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Unauthorized"));
    }

    @Test
    public void testHandleRequest_MissingBody() {
        APIGatewayProxyRequestEvent request = createAuthenticatedRequest("cust-123", null);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Request body is required"));
    }

    @Test
    public void testHandleRequest_MissingFileName() {
        APIGatewayProxyRequestEvent request = createAuthenticatedRequest("cust-123", "{}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("fileName is required"));
    }

    @Test
    public void testHandleRequest_RepositoryError() {
        when(mockContext.getLogger()).thenReturn(mockLogger);
        when(fileRepository.generateUploadUrl(anyString(), any())).thenThrow(new RuntimeException("S3 error"));

        APIGatewayProxyRequestEvent request = createAuthenticatedRequest("cust-123", "{\"fileName\":\"test.png\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Could not generate upload URL"));
    }
}
