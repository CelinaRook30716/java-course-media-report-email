package edu.report;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public final class ReportDeliveryService {
    public static final class MediaAsset {
        private final String title;
        private final int completedLessons;

        public MediaAsset(String title, int completedLessons) {
            this.title = title;
            this.completedLessons = completedLessons;
        }

        public String title() {
            return title;
        }

        public int completedLessons() {
            return completedLessons;
        }
    }

    public static final class Delivery {
        private final String reportId;
        private final String messageId;

        public Delivery(String reportId, String messageId) {
            this.reportId = reportId;
            this.messageId = messageId;
        }

        public String reportId() {
            return reportId;
        }

        public String messageId() {
            return messageId;
        }
    }

    private final EmailSender email;

    public ReportDeliveryService(EmailSender email) {
        this.email = email;
    }

    public Delivery deliver(String creatorEmail, String courseName, List<MediaAsset> assets)
            throws IOException, InterruptedException {
        if (assets.isEmpty()) throw new IllegalArgumentException("A report needs at least one processed asset");
        String reportId = UUID.randomUUID().toString();
        int lessons = assets.stream().mapToInt(MediaAsset::completedLessons).sum();
        String body = "Course report " + courseName + "\nProcessed assets: " + assets.size()
                + "\nCompleted lessons: " + lessons + "\nReport id: " + reportId;
        String messageId = email.send(creatorEmail, "Course media report: " + courseName, body, reportId);
        return new Delivery(reportId, messageId);
    }
}
