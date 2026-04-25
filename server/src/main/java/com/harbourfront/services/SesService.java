package com.harbourfront.services;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient;
import software.amazon.awssdk.services.sesv2.model.*;

public class SesService {

    private final SesV2AsyncClient client;
    private final String senderEmail;
    private final String appBaseUrl;


    public SesService() {
        this.senderEmail = System.getenv().getOrDefault("SES_SENDER_EMAIL", "");
        this.appBaseUrl = System.getenv().getOrDefault("APP_BASE_URL", "");
        this.client = SesV2AsyncClient.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-2")))
                .credentialsProvider(InstanceProfileCredentialsProvider.create())
                .build();
    }

    public Future<Void> sendConfirmation(String toEmail, String token) {
        Promise<Void> promise = Promise.promise();
        String confirmUrl = appBaseUrl + "/api/newsletter/confirm?token=" + token;

        String html = "<div style=\"font-family:sans-serif;max-width:520px;margin:0 auto;"
                + "color:#e8e8f0;background:#0d0d0f;padding:40px 32px;border-radius:12px;\">"
                + "<h2 style=\"margin:0 0 16px;letter-spacing:-0.03em;\">Confirm your subscription</h2>"
                + "<p style=\"color:#6e6e82;line-height:1.6;\">You're one step away. Click below to confirm "
                + "your email and start receiving insights on digital strategy, design, and Toronto's tech scene.</p>"
                + "<a href=\"" + confirmUrl + "\" style=\"display:inline-block;margin-top:28px;padding:13px 28px;"
                + "background:#6c63ff;color:#fff;border-radius:8px;text-decoration:none;font-weight:600;\">"
                + "Confirm subscription</a>"
                + "<p style=\"margin-top:32px;font-size:0.8rem;color:#6e6e82;\">"
                + "If you didn't subscribe, you can ignore this email.</p></div>";

        SendEmailRequest req = SendEmailRequest.builder()
                .fromEmailAddress(senderEmail)
                .destination(Destination.builder().toAddresses(toEmail).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data("Confirm your subscription").charset("UTF-8").build())
                                .body(Body.builder()
                                        .html(Content.builder().data(html).charset("UTF-8").build())
                                        .build())
                                .build())
                        .build())
                .build();

        client.sendEmail(req).whenComplete((res, err) -> {
            if (err != null) promise.fail(err);
            else promise.complete();
        });

        return promise.future();
    }

    public void close() {
        client.close();
    }
}
