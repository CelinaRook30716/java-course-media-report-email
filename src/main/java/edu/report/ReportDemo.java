package edu.report;

import java.util.Arrays;

public final class ReportDemo {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        String to = System.getenv("REPORT_TO");
        if (key == null || key.trim().isEmpty() || to == null || to.trim().isEmpty()) {
            throw new IllegalStateException("Set INFRAI_API_KEY and REPORT_TO before running the demo");
        }
        ReportDeliveryService.Delivery delivery = new ReportDeliveryService(new InfraiEmailClient(key))
                .deliver(to, "Physics for Creators", Arrays.asList(
                        new ReportDeliveryService.MediaAsset("lesson-01.mp4", 4),
                        new ReportDeliveryService.MediaAsset("lesson-02.mp4", 3)));
        System.out.println("Delivered report " + delivery.reportId() + " as message " + delivery.messageId());
    }
}
