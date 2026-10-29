package com.cloudstore.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cloudstore.repositories.FileRepository;
import com.cloudstore.repositories.S3FileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Lambda handler for generating S3 pre-signed retrieval / download URLs for authenticated customers.
 */
public class GetFileHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileRepository fileRepository;

    public GetFileHandler() {
        this.fileRepository = new S3FileRepository();
    }

    public GetFileHandler(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        try {
            String userId = getUserId(input);
            if (userId == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(401)
                        .withHeaders(headers)
                        .withBody("{ \"error\": \"Unauthorized\" }");
            }

            String fileName = null;
            if (input != null && input.getPathParameters() != null) {
                fileName = input.getPathParameters().get("fileName");
            }
            if ((fileName == null || fileName.isBlank()) && input != null && input.getQueryStringParameters() != null) {
                fileName = input.getQueryStringParameters().get("fileName");
            }

            if (fileName == null || fileName.isBlank()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withHeaders(headers)
                        .withBody("{ \"error\": \"fileName is required\" }");
            }

            // Sanitize filename to prevent directory traversal
            String cleanFileName = Paths.get(fileName).getFileName().toString();
            if (cleanFileName.isEmpty() || cleanFileName.equals(".") || cleanFileName.equals("..")) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withHeaders(headers)
                        .withBody("{ \"error\": \"Invalid fileName\" }");
            }

            // Secure, user-isolated file key
            String fileKey = userId + "/" + cleanFileName;

            String downloadUrl = fileRepository.generateDownloadUrl(fileKey);

            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("fileName", cleanFileName);
            responseBody.put("fileKey", fileKey);
            responseBody.put("downloadUrl", downloadUrl);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(objectMapper.writeValueAsString(responseBody));

        } catch (Exception e) {
            if (context != null && context.getLogger() != null) {
                context.getLogger().log("Error generating download URL: " + e.getMessage());
            }
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody("{ \"error\": \"Could not retrieve file\" }");
        }
    }

    private String getUserId(APIGatewayProxyRequestEvent input) {
        try {
            if (input == null || input.getRequestContext() == null) {
                return null;
            }
            Map<String, Object> authorizer = input.getRequestContext().getAuthorizer();
            if (authorizer == null) return null;
            @SuppressWarnings("unchecked")
            Map<String, String> claims = (Map<String, String>) authorizer.get("claims");
            if (claims == null) return null;
            return claims.get("sub");
        } catch (Exception e) {
            return null;
        }
    }
}
