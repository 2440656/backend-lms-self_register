package com.cognizant.lms.userservice;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.cognizant.lms.userservice.service.UserAuditLogServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class StreamLambdaHandler implements RequestStreamHandler {
    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    static {
        try {
            handler =
                    SpringBootLambdaContainerHandler.getAwsProxyHandler(LmsUserServiceApplication.class);
        } catch (ContainerInitializationException e) {
            // if we fail here. We re-throw the exception to force another cold start
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        log.info("StreamLambdaHandler invoked inputStream  : " + inputStream);
        String inputString = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines().reduce("", (acc, line) -> acc + line);
        log.info("Input String: {}", inputString);

        JsonNode rootNode = objectMapper.readTree(inputString);
        log.info("rootNode JSON: {}", rootNode.toString());

        if (rootNode.has("Records") && rootNode.get("Records").isArray()
                && rootNode.get("Records").size() > 0
                && rootNode.get("Records").get(0).has("eventSource")
                && "aws:dynamodb".equals(rootNode.get("Records").get(0).get("eventSource").asText())) {
            log.info("Detected DynamoDB event");
            UserAuditLogServiceImpl.handleDynamoDBEvent(rootNode);
            log.info("DynamoDB event processed successfully");
        } else {
            log.info("Detected API Gateway event");
            InputStream apiInput = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));
            handler.proxyStream(apiInput, outputStream, context);
        }
    }


}