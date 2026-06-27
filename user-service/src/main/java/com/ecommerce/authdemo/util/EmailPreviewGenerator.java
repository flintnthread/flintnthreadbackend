package com.ecommerce.authdemo.util;

import java.nio.file.Files;
import java.nio.file.Path;

/** Run locally to export HTML previews: mvn -q compile exec:java -Dexec.mainClass=com.ecommerce.authdemo.util.EmailPreviewGenerator */
public final class EmailPreviewGenerator {

    private EmailPreviewGenerator() {
    }

    public static void main(String[] args) throws Exception {
        Path dir = Path.of("email-previews");
        Files.createDirectories(dir);

        Files.writeString(dir.resolve("otp-email-preview.html"), EmailHtmlTemplates.buildOtpEmailHtml("123456"));
        Files.writeString(
                dir.resolve("welcome-email-preview.html"),
                EmailHtmlTemplates.buildWelcomeEmailHtml("Sandhya Gudisa", "Sandhya", "REFSAND001426")
        );

        System.out.println("Preview files written to: " + dir.toAbsolutePath());
    }
}
