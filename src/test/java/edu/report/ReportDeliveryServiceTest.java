package edu.report;

import java.util.Arrays;

public final class ReportDeliveryServiceTest {
    public static void main(String[] args) throws Exception {
        ReportDeliveryService service = new ReportDeliveryService((to, subject, body, requestId) -> {
            throw new AssertionError("empty processing jobs must not send email");
        });
        try {
            service.deliver("creator@example.com", "Algebra 1", Arrays.<ReportDeliveryService.MediaAsset>asList());
            throw new AssertionError("expected the delivery decision to reject an empty report");
        } catch (IllegalArgumentException expected) {
            System.out.println("PASS: empty processing jobs are held before creator delivery");
        }
    }
}
