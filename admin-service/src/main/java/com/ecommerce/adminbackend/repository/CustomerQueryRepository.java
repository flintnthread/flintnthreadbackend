package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.logging.LogFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CustomerQueryRepository {

    private static final Logger log = LogFactory.getLogger(CustomerQueryRepository.class);

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<Object[]> listCustomers(String search, int offset, int limit) {
        return entityManager.createNativeQuery("""
                SELECT MIN(id) AS customer_id, shipping_email, shipping_name, shipping_phone,
                       shipping_city, shipping_state, shipping_country,
                       COUNT(*) AS order_count,
                       COALESCE(SUM(total_amount), 0) AS total_spent,
                       MAX(created_at) AS last_order_at
                FROM orders
                WHERE shipping_email IS NOT NULL AND shipping_email != ''
                  AND (:search IS NULL OR :search = '' OR
                       LOWER(shipping_email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       LOWER(shipping_name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       shipping_phone LIKE CONCAT('%', :search, '%'))
                GROUP BY shipping_email, shipping_name, shipping_phone,
                         shipping_city, shipping_state, shipping_country
                ORDER BY last_order_at DESC
                LIMIT :limit OFFSET :offset
                """)
                .setParameter("search", search)
                .setParameter("limit", limit)
                .setParameter("offset", offset)
                .getResultList();
    }

    public long countCustomers(String search) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM (
                    SELECT shipping_email
                    FROM orders
                    WHERE shipping_email IS NOT NULL AND shipping_email != ''
                      AND (:search IS NULL OR :search = '' OR
                           LOWER(shipping_email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                           LOWER(shipping_name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                           shipping_phone LIKE CONCAT('%', :search, '%'))
                    GROUP BY shipping_email, shipping_name, shipping_phone,
                             shipping_city, shipping_state, shipping_country
                ) t
                """)
                .setParameter("search", search)
                .getSingleResult();
        return count.longValue();
    }

    @SuppressWarnings("unchecked")
    public Optional<Object[]> findCustomerById(Long customerId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT MIN(o.id) AS customer_id,
                       MAX(o.shipping_email) AS shipping_email,
                       (SELECT o2.shipping_name FROM orders o2 WHERE o2.id = :customerId LIMIT 1) AS shipping_name,
                       (SELECT o2.shipping_phone FROM orders o2 WHERE o2.id = :customerId LIMIT 1) AS shipping_phone,
                       (SELECT o2.shipping_address1 FROM orders o2 WHERE o2.id = :customerId LIMIT 1) AS shipping_address1,
                       (SELECT o2.shipping_address2 FROM orders o2 WHERE o2.id = :customerId LIMIT 1) AS shipping_address2,
                       (SELECT o2.shipping_city FROM orders o2 WHERE o2.id = :customerId LIMIT 1) AS shipping_city,
                       (SELECT o2.shipping_state FROM orders o2 WHERE o2.id = :customerId LIMIT 1) AS shipping_state,
                       (SELECT o2.shipping_country FROM orders o2 WHERE o2.id = :customerId LIMIT 1) AS shipping_country,
                       (SELECT o2.shipping_pincode FROM orders o2 WHERE o2.id = :customerId LIMIT 1) AS shipping_pincode,
                       COUNT(*) AS order_count,
                       COALESCE(SUM(o.total_amount), 0) AS total_spent,
                       MIN(o.created_at) AS first_order_at,
                       MAX(o.created_at) AS last_order_at
                FROM orders o
                WHERE LOWER(o.shipping_email) = (
                    SELECT LOWER(shipping_email) FROM orders WHERE id = :customerId LIMIT 1
                )
                """)
                .setParameter("customerId", customerId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @SuppressWarnings("unchecked")
    public Optional<Object[]> findCustomerByEmail(String email) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT MIN(id) AS customer_id, shipping_email, shipping_name, shipping_phone,
                       shipping_address1, shipping_address2, shipping_city,
                       shipping_state, shipping_country, shipping_pincode,
                       COUNT(*) AS order_count,
                       COALESCE(SUM(total_amount), 0) AS total_spent,
                       MIN(created_at) AS first_order_at,
                       MAX(created_at) AS last_order_at
                FROM orders
                WHERE LOWER(shipping_email) = LOWER(:email)
                GROUP BY shipping_email, shipping_name, shipping_phone,
                         shipping_address1, shipping_address2, shipping_city,
                         shipping_state, shipping_country, shipping_pincode
                """)
                .setParameter("email", email)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public long countDistinctCustomers() {
        log.debug("Counting distinct customers");
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(DISTINCT shipping_email) FROM orders
                WHERE shipping_email IS NOT NULL AND shipping_email != ''
                """).getSingleResult();
        return count.longValue();
    }

    public Number sumCustomerRevenue() {
        return (Number) entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(total_amount), 0) FROM orders
                WHERE shipping_email IS NOT NULL AND shipping_email != ''
                """).getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> listOrdersByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT o.id,
                       o.order_number,
                       o.created_at,
                       o.total_amount,
                       o.payment_method,
                       o.payment_status,
                       o.order_status,
                       COALESCE((
                           SELECT SUM(oi.quantity) FROM order_items oi WHERE oi.order_id = o.id
                       ), 0) AS item_count
                FROM orders o
                WHERE LOWER(o.shipping_email) = (
                    SELECT LOWER(shipping_email) FROM orders WHERE id = :customerId LIMIT 1
                )
                ORDER BY o.created_at DESC
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> monthlySpendingByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT MONTH(o.created_at) AS month_num,
                       COALESCE(SUM(o.total_amount), 0) AS amount
                FROM orders o
                WHERE LOWER(o.shipping_email) = (
                    SELECT LOWER(shipping_email) FROM orders WHERE id = :customerId LIMIT 1
                )
                  AND YEAR(o.created_at) = YEAR(CURDATE())
                GROUP BY MONTH(o.created_at)
                ORDER BY MONTH(o.created_at)
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    private static final String CUSTOMER_EMAIL_WHERE =
            " WHERE LOWER(o.shipping_email) = (SELECT LOWER(shipping_email) FROM orders WHERE id = :customerId LIMIT 1)";

    @SuppressWarnings("unchecked")
    public List<Object[]> orderStatusCountsByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT LOWER(COALESCE(o.order_status, 'pending')) AS status, COUNT(*) AS cnt
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                GROUP BY LOWER(COALESCE(o.order_status, 'pending'))
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> monthlyOrdersByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT MONTH(o.created_at) AS month_num, COUNT(*) AS order_count
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND YEAR(o.created_at) = YEAR(CURDATE())
                GROUP BY MONTH(o.created_at)
                ORDER BY MONTH(o.created_at)
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> paymentMethodsByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT COALESCE(NULLIF(o.payment_method, ''), 'Unknown') AS method, COUNT(*) AS cnt
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                GROUP BY COALESCE(NULLIF(o.payment_method, ''), 'Unknown')
                ORDER BY cnt DESC
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> topCategoriesByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT COALESCE(c.category_name, sc.subcategory_name, 'Uncategorized') AS category_name,
                       COALESCE(SUM(oi.quantity), 0) AS qty
                FROM orders o
                JOIN order_items oi ON oi.order_id = o.id
                LEFT JOIN products p ON p.id = oi.product_id
                LEFT JOIN categories c ON c.id = p.category_id
                LEFT JOIN subcategories sc ON sc.id = p.subcategory_id
                """ + CUSTOMER_EMAIL_WHERE + """
                GROUP BY COALESCE(c.category_name, sc.subcategory_name, 'Uncategorized')
                ORDER BY qty DESC
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> topBrandsByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT COALESCE(NULLIF(oi.seller_name, ''), 'Unknown Seller') AS brand_name,
                       COALESCE(SUM(oi.quantity), 0) AS qty
                FROM orders o
                JOIN order_items oi ON oi.order_id = o.id
                """ + CUSTOMER_EMAIL_WHERE + """
                GROUP BY COALESCE(NULLIF(oi.seller_name, ''), 'Unknown Seller')
                ORDER BY qty DESC
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> weeklyOrdersByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT week_start, week_count
                FROM (
                    SELECT DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY) AS week_start,
                           COUNT(*) AS week_count
                    FROM orders o
                    """ + CUSTOMER_EMAIL_WHERE + """
                      AND o.created_at >= DATE_SUB(CURDATE(), INTERVAL 8 WEEK)
                    GROUP BY DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY)
                ) w
                ORDER BY week_start
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> purchaseHourBucketsByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT HOUR(o.created_at) AS hour_bucket, COUNT(*) AS cnt
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                GROUP BY HOUR(o.created_at)
                ORDER BY hour_bucket
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> ordersByWeekdayByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT DAYOFWEEK(o.created_at) AS day_num, COUNT(*) AS cnt
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                GROUP BY DAYOFWEEK(o.created_at)
                ORDER BY cnt DESC
                LIMIT 1
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    public Number avgBasketSizeByCustomerId(Long customerId) {
        return (Number) entityManager.createNativeQuery("""
                SELECT COALESCE(AVG(item_count), 0)
                FROM (
                    SELECT o.id, COALESCE(SUM(oi.quantity), 0) AS item_count
                    FROM orders o
                    LEFT JOIN order_items oi ON oi.order_id = o.id
                    """ + CUSTOMER_EMAIL_WHERE + """
                    GROUP BY o.id
                ) t
                """)
                .setParameter("customerId", customerId)
                .getSingleResult();
    }

    public Number avgDeliveryDaysByCustomerId(Long customerId) {
        return (Number) entityManager.createNativeQuery("""
                SELECT COALESCE(AVG(TIMESTAMPDIFF(DAY, o.created_at, o.shiprocket_synced_at)), 0)
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND o.shiprocket_synced_at IS NOT NULL
                  AND LOWER(COALESCE(o.order_status, '')) LIKE '%deliver%'
                """)
                .setParameter("customerId", customerId)
                .getSingleResult();
    }

    public Number longestOrderGapDaysByCustomerId(Long customerId) {
        return (Number) entityManager.createNativeQuery("""
                SELECT COALESCE(MAX(gap_days), 0)
                FROM (
                    SELECT TIMESTAMPDIFF(
                        DAY,
                        LAG(o.created_at) OVER (ORDER BY o.created_at),
                        o.created_at
                    ) AS gap_days
                    FROM orders o
                    """ + CUSTOMER_EMAIL_WHERE + """
                ) gaps
                """)
                .setParameter("customerId", customerId)
                .getSingleResult();
    }

    public long currentOrderStreakByCustomerId(Long customerId) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND o.created_at >= DATE_SUB(CURDATE(), INTERVAL 90 DAY)
                """)
                .setParameter("customerId", customerId)
                .getSingleResult();
        return count.longValue();
    }

    public Number refundTotalByCustomerId(Long customerId) {
        return (Number) entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(o.total_amount), 0)
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND LOWER(COALESCE(o.order_status, '')) IN ('cancelled', 'returned', 'refunded', 'rto')
                """)
                .setParameter("customerId", customerId)
                .getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> returnReasonsByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT
                    CASE
                        WHEN LOWER(COALESCE(o.order_status, '')) LIKE '%cancel%' THEN 'Order cancelled'
                        WHEN LOWER(COALESCE(o.order_status, '')) LIKE '%return%' THEN 'Item returned'
                        WHEN LOWER(COALESCE(o.order_status, '')) LIKE '%rto%' THEN 'RTO'
                        ELSE 'Refund processed'
                    END AS reason,
                    COUNT(*) AS cnt
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND LOWER(COALESCE(o.order_status, '')) IN ('cancelled', 'returned', 'refunded', 'rto')
                GROUP BY reason
                ORDER BY cnt DESC
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    public long countOrdersForCustomer(Long customerId) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM orders o """ + CUSTOMER_EMAIL_WHERE + """
                """)
                .setParameter("customerId", customerId)
                .getSingleResult();
        return count.longValue();
    }

    public long paidOrdersCountByCustomerId(Long customerId) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND LOWER(COALESCE(o.payment_status, '')) IN ('paid', 'success', 'captured', 'completed')
                """)
                .setParameter("customerId", customerId)
                .getSingleResult();
        return count.longValue();
    }

    public long failedPaymentsCountByCustomerId(Long customerId) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND LOWER(COALESCE(o.payment_status, '')) IN ('failed', 'cancelled', 'declined')
                """)
                .setParameter("customerId", customerId)
                .getSingleResult();
        return count.longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> refundHistoryByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT o.created_at, o.total_amount,
                       CASE
                           WHEN LOWER(COALESCE(o.order_status, '')) LIKE '%cancel%' THEN 'Order cancelled'
                           WHEN LOWER(COALESCE(o.order_status, '')) LIKE '%return%' THEN 'Item returned'
                           ELSE 'Refund processed'
                       END AS reason
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND LOWER(COALESCE(o.order_status, '')) IN ('cancelled', 'returned', 'refunded', 'rto')
                ORDER BY o.created_at DESC
                LIMIT 10
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> distinctCitiesByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT o.shipping_city, COUNT(*) AS cnt
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND o.shipping_city IS NOT NULL AND o.shipping_city != ''
                GROUP BY o.shipping_city
                ORDER BY cnt DESC
                LIMIT 5
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public Optional<Object[]> reviewSummaryByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT COUNT(*) AS submitted,
                       COALESCE(AVG(r.rating), 0) AS avg_rating,
                       SUM(CASE WHEN r.image_path IS NOT NULL AND r.image_path != '' THEN 1 ELSE 0 END) AS photo_reviews
                FROM product_reviews r
                WHERE LOWER(r.email) = LOWER(:email)
                """)
                .setParameter("email", email)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @SuppressWarnings("unchecked")
    public Optional<Object[]> supportSummaryByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT COUNT(*) AS total,
                       SUM(CASE WHEN LOWER(st.status) IN ('resolved', 'closed') THEN 1 ELSE 0 END) AS resolved,
                       AVG(TIMESTAMPDIFF(HOUR, st.created_at, st.updated_at)) AS avg_hours,
                       (
                           SELECT s.subject FROM support_tickets s
                           JOIN users u ON u.id = s.customer_id
                           WHERE LOWER(u.email) = LOWER(:email)
                           ORDER BY s.created_at DESC LIMIT 1
                       ) AS latest_subject
                FROM support_tickets st
                JOIN users u ON u.id = st.customer_id
                WHERE LOWER(u.email) = LOWER(:email)
                """)
                .setParameter("email", email)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Number totalSavingsByCustomerId(Long customerId) {
        return (Number) entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(
                    COALESCE(o.wallet_deduction, 0) +
                    COALESCE(o.discount_amount, 0) +
                    COALESCE(o.referral_discount_amount, 0)
                ), 0)
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                """)
                .setParameter("customerId", customerId)
                .getSingleResult();
    }

    public long couponUsageCountByCustomerId(Long customerId) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND COALESCE(o.discount_amount, 0) > 0
                """)
                .setParameter("customerId", customerId)
                .getSingleResult();
        return count.longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> recentOrdersWithProductByCustomerId(Long customerId) {
        return entityManager.createNativeQuery("""
                SELECT o.id,
                       o.order_number,
                       COALESCE(
                           NULLIF(TRIM((
                               SELECT COALESCE(
                                   NULLIF(TRIM(oi.product_name), ''),
                                   NULLIF(TRIM(p.name), ''),
                                   CASE
                                       WHEN oi.product_id IS NOT NULL THEN CONCAT('Product #', oi.product_id)
                                   END
                               )
                               FROM order_items oi
                               LEFT JOIN products p ON p.id = oi.product_id
                               WHERE oi.order_id = o.id
                               ORDER BY oi.id
                               LIMIT 1
                           )), ''),
                           'Product'
                       ) AS product_name,
                       o.created_at,
                       o.total_amount,
                       o.order_status,
                       o.payment_method
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                ORDER BY o.created_at DESC
                LIMIT 6
                """)
                .setParameter("customerId", customerId)
                .getResultList();
    }

    public Number spendInLastDays(Long customerId, int days) {
        return (Number) entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(o.total_amount), 0)
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND o.created_at >= DATE_SUB(CURDATE(), INTERVAL :days DAY)
                """)
                .setParameter("customerId", customerId)
                .setParameter("days", days)
                .getSingleResult();
    }

    public Number spendBetweenDays(Long customerId, int startDaysAgo, int endDaysAgo) {
        return (Number) entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(o.total_amount), 0)
                FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND o.created_at >= DATE_SUB(CURDATE(), INTERVAL :startDays DAY)
                  AND o.created_at < DATE_SUB(CURDATE(), INTERVAL :endDays DAY)
                """)
                .setParameter("customerId", customerId)
                .setParameter("startDays", startDaysAgo)
                .setParameter("endDays", endDaysAgo)
                .getSingleResult();
    }

    public long ordersInLastDays(Long customerId, int days) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND o.created_at >= DATE_SUB(CURDATE(), INTERVAL :days DAY)
                """)
                .setParameter("customerId", customerId)
                .setParameter("days", days)
                .getSingleResult();
        return count.longValue();
    }

    public long ordersBetweenDays(Long customerId, int startDaysAgo, int endDaysAgo) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM orders o
                """ + CUSTOMER_EMAIL_WHERE + """
                  AND o.created_at >= DATE_SUB(CURDATE(), INTERVAL :startDays DAY)
                  AND o.created_at < DATE_SUB(CURDATE(), INTERVAL :endDays DAY)
                """)
                .setParameter("customerId", customerId)
                .setParameter("startDays", startDaysAgo)
                .setParameter("endDays", endDaysAgo)
                .getSingleResult();
        return count.longValue();
    }
}
