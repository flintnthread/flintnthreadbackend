package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.logging.LogFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class DashboardQueryRepository {

    private static final Logger log = LogFactory.getLogger(DashboardQueryRepository.class);

    private static final String PENDING_STATUSES =
            "'pending','placed','new','awaiting_payment','awaiting_processing','sent_to_seller'";
    private static final String PROCESSING_STATUSES =
            "'confirmed','processing','packed','awb_assigned','pickup_scheduled','accepted'";
    private static final String SHIPPED_STATUSES =
            "'shipped','in_transit','picked_up','out_for_delivery','ready_to_ship'";
    private static final String DELIVERED_STATUSES = "'delivered','completed'";
    private static final String CANCELLED_STATUSES = "'cancelled','canceled','rejected'";
    private static final String RETURNED_STATUSES =
            "'returned','return','refunded','rto_initiated','rto_delivered','replacement'";

    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Long> orderStatusBuckets() {
        log.debug("Querying order status buckets");
        String sql = """
                SELECT
                  COALESCE(SUM(CASE WHEN LOWER(order_status) IN (%s) THEN 1 ELSE 0 END), 0),
                  COALESCE(SUM(CASE WHEN LOWER(order_status) IN (%s) THEN 1 ELSE 0 END), 0),
                  COALESCE(SUM(CASE WHEN LOWER(order_status) IN (%s) THEN 1 ELSE 0 END), 0),
                  COALESCE(SUM(CASE WHEN LOWER(order_status) IN (%s) THEN 1 ELSE 0 END), 0),
                  COALESCE(SUM(CASE WHEN LOWER(order_status) IN (%s) THEN 1 ELSE 0 END), 0),
                  COALESCE(SUM(CASE WHEN LOWER(order_status) IN (%s) THEN 1 ELSE 0 END), 0)
                FROM orders
                """.formatted(
                PENDING_STATUSES,
                PROCESSING_STATUSES,
                SHIPPED_STATUSES,
                DELIVERED_STATUSES,
                CANCELLED_STATUSES,
                RETURNED_STATUSES);

        Object[] row = (Object[]) entityManager.createNativeQuery(sql).getSingleResult();
        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put("pendingOrders", toLong(row[0]));
        buckets.put("processingOrders", toLong(row[1]));
        buckets.put("shippedOrders", toLong(row[2]));
        buckets.put("deliveredOrders", toLong(row[3]));
        buckets.put("cancelledOrders", toLong(row[4]));
        buckets.put("returnedOrders", toLong(row[5]));
        buckets.put("processingCount", buckets.get("processingOrders"));
        buckets.put("returnedCount", buckets.get("returnedOrders"));
        return buckets;
    }

    public Map<String, Object> paymentSummary() {
        String sql = """
                SELECT
                  COALESCE(SUM(CASE
                    WHEN LOWER(COALESCE(payment_method, '')) LIKE '%cod%'
                      OR LOWER(COALESCE(payment_method, '')) LIKE '%cash%'
                    THEN 1 ELSE 0 END), 0),
                  COALESCE(SUM(CASE
                    WHEN LOWER(COALESCE(payment_method, '')) LIKE '%cod%'
                      OR LOWER(COALESCE(payment_method, '')) LIKE '%cash%'
                    THEN total_amount ELSE 0 END), 0),
                  COALESCE(SUM(CASE
                    WHEN LOWER(COALESCE(payment_method, '')) NOT LIKE '%cod%'
                      AND LOWER(COALESCE(payment_method, '')) NOT LIKE '%cash%'
                    THEN 1 ELSE 0 END), 0),
                  COALESCE(SUM(CASE
                    WHEN LOWER(COALESCE(payment_method, '')) NOT LIKE '%cod%'
                      AND LOWER(COALESCE(payment_method, '')) NOT LIKE '%cash%'
                    THEN total_amount ELSE 0 END), 0),
                  COALESCE(SUM(CASE
                    WHEN LOWER(COALESCE(payment_status, '')) IN ('pending', 'awaiting_payment', 'failed')
                    THEN 1 ELSE 0 END), 0),
                  COALESCE(SUM(CASE
                    WHEN LOWER(COALESCE(payment_status, '')) IN ('pending', 'awaiting_payment', 'failed')
                    THEN total_amount ELSE 0 END), 0),
                  COALESCE(SUM(CASE
                    WHEN LOWER(order_status) IN ("""
                + RETURNED_STATUSES + """
                    )
                      OR LOWER(COALESCE(payment_status, '')) LIKE '%refund%'
                    THEN 1 ELSE 0 END), 0),
                  COALESCE(SUM(CASE
                    WHEN LOWER(order_status) IN ("""
                + RETURNED_STATUSES + """
                    )
                      OR LOWER(COALESCE(payment_status, '')) LIKE '%refund%'
                    THEN total_amount ELSE 0 END), 0),
                  COALESCE(SUM(COALESCE(discount_amount, 0) + COALESCE(referral_discount_amount, 0)), 0)
                FROM orders
                """;

        Object[] row = (Object[]) entityManager.createNativeQuery(sql).getSingleResult();

        long codOrders = toLong(row[0]);
        long onlineOrders = toLong(row[2]);
        long paymentTotal = codOrders + onlineOrders;
        int onlinePercent = paymentTotal > 0 ? (int) Math.round(onlineOrders * 100.0 / paymentTotal) : 0;
        int codPercent = paymentTotal > 0 ? 100 - onlinePercent : 0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("codOrders", codOrders);
        summary.put("codAmount", row[1]);
        summary.put("onlineOrders", onlineOrders);
        summary.put("onlineAmount", row[3]);
        summary.put("pendingPayments", toLong(row[4]));
        summary.put("pendingPaymentAmount", row[5]);
        summary.put("refundedOrders", toLong(row[6]));
        summary.put("refundedAmount", row[7]);
        summary.put("totalDiscountGiven", row[8]);
        summary.put("onlinePercent", onlinePercent);
        summary.put("codPercent", codPercent);
        return summary;
    }

    public Map<String, Object> refundSummary(long totalOrders, long returnedOrders) {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                SELECT
                  COALESCE(SUM(CASE
                    WHEN LOWER(order_status) IN ('returned', 'return', 'rto_initiated')
                      AND LOWER(COALESCE(payment_status, '')) NOT LIKE '%refund%'
                    THEN 1 ELSE 0 END), 0),
                  COALESCE(SUM(CASE
                    WHEN LOWER(order_status) IN ('refunded', 'rto_delivered')
                      OR LOWER(COALESCE(payment_status, '')) LIKE '%refund%'
                    THEN 1 ELSE 0 END), 0),
                  COALESCE(SUM(CASE
                    WHEN LOWER(order_status) IN ('cancelled', 'canceled', 'rejected')
                      AND LOWER(COALESCE(payment_status, '')) LIKE '%refund%failed%'
                    THEN 1 ELSE 0 END), 0)
                FROM orders
                """).getSingleResult();

        double returnRate = totalOrders > 0 ? (returnedOrders * 100.0 / totalOrders) : 0.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("pendingRefunds", toLong(row[0]));
        summary.put("approvedRefunds", toLong(row[1]));
        summary.put("rejectedRefunds", toLong(row[2]));
        summary.put("returnedOrderRate", Math.round(returnRate * 10.0) / 10.0);
        return summary;
    }

    public long countNewCustomersSince(LocalDateTime since) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM users
                WHERE created_at >= :since
                """)
                .setParameter("since", since)
                .getSingleResult();
        return count.longValue();
    }

    public long countActiveCustomers(LocalDateTime since) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(DISTINCT user_id)
                FROM orders
                WHERE user_id IS NOT NULL
                  AND created_at >= :since
                """)
                .setParameter("since", since)
                .getSingleResult();
        return count.longValue();
    }

    public long countSellersWithSales() {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(DISTINCT seller_id)
                FROM order_items
                WHERE seller_id IS NOT NULL
                """).getSingleResult();
        return count.longValue();
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof BigDecimal bd) {
            return bd.longValue();
        }
        return ((Number) value).longValue();
    }
}
