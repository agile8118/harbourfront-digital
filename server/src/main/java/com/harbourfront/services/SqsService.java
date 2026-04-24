package com.harbourfront.services;

import com.harbourfront.Log;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Sends a JSON payload to an SQS queue so a downstream email worker can
 * notify the owner. Credentials come from the EC2 instance IAM role
 * automatically.
 */
public class SqsService {

    private final SqsAsyncClient client;
    private final String queueUrl;
    private final String ownerEmail;

    public SqsService() {
        SqsAsyncClient c = null;
        try {
            c = SqsAsyncClient.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "ca-central-1")))
                .build();
        } catch (Exception e) {
            Log.error("SQS client failed to initialize — notifications disabled: " + e.getMessage());
        }
        this.client     = c;
        this.queueUrl   = System.getenv("SQS_QUEUE_URL");
        this.ownerEmail = System.getenv().getOrDefault("OWNER_EMAIL", "");
    }

    public Future<Void> sendContactNotification(String name, String email, String message) {
        Promise<Void> promise = Promise.promise();

        if (client == null) {
            Log.error("SQS client failed to initialize — notification not sent for: " + email);
            promise.complete();
            return promise.future();
        }

        if (queueUrl == null || queueUrl.isBlank()) {
            Log.error("SQS_QUEUE_URL not configured — notification not sent for: " + email);
            promise.complete();
            return promise.future();
        }

        String body = new JsonObject()
            .put("to",      ownerEmail)
            .put("subject", "New Contact Form Submission")
            .put("name",    name)
            .put("email",   email)
            .put("message", message)
            .encode();

        SendMessageRequest req = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(body)
            .build();

        try {
            client.sendMessage(req).whenComplete((result, err) -> {
                if (err != null) promise.fail(err);
                else             promise.complete();
            });
        } catch (Exception e) {
            promise.fail(e);
        }

        return promise.future();
    }

    public void close() {
        if (client != null) client.close();
    }
}