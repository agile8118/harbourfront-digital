package com.harbourfront.services;

import com.harbourfront.Log;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Publishes a plain-text notification to an SNS topic. The topic has an
 * Email subscription for the owner, so SNS delivers the message directly.
 * Credentials come from the EC2 instance IAM role automatically.
 */
public class SnsService {

    private final SnsAsyncClient client;
    private final String topicArn;

    public SnsService() {
        SnsAsyncClient c = null;
        try {
            c = SnsAsyncClient.builder()
                    .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-2")))
                    .build();
        } catch (Exception e) {
            Log.error("SNS client failed to initialize — notifications disabled: " + e.getMessage());
        }
        this.client = c;
        this.topicArn = System.getenv("SNS_TOPIC_ARN");
    }

    public Future<Void> sendContactNotification(String name, String email, String message) {
        Promise<Void> promise = Promise.promise();

        if ("true".equals(System.getenv("SKIP_SNS"))) {
            Log.info("SKIP_SNS=true — notification not sent for: " + email);
            promise.complete();
            return promise.future();
        }

        if (client == null) {
            Log.error("SNS client failed to initialize — notification not sent for: " + email);
            promise.fail("SNS client failed to initialize");
            return promise.future();
        }

        if (topicArn == null || topicArn.isBlank()) {
            Log.error("SNS_TOPIC_ARN not configured — notification not sent for: " + email);
            promise.fail("SNS_TOPIC_ARN not configured");
            return promise.future();
        }

        String body = "New contact form submission\n\n"
                + "Name: " + name + "\n"
                + "Email: " + email + "\n"
                + "Message:\n" + message;

        PublishRequest req = PublishRequest.builder()
                .topicArn(topicArn)
                .subject("New Contact Form Submission")
                .message(body)
                .build();

        try {
            client.publish(req).whenComplete((result, err) -> {
                if (err != null)
                    promise.fail(err);
                else
                    promise.complete();
            });
        } catch (Exception e) {
            promise.fail(e);
        }

        return promise.future();
    }

    public void close() {
        if (client != null)
            client.close();
    }
}