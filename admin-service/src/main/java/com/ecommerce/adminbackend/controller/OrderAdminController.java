package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.dto.common.NoteRequest;
import com.ecommerce.adminbackend.service.OrderAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private static final Logger log = LogFactory.getLogger(OrderAdminController.class);

    private final OrderAdminService orderAdminService;

    @GetMapping
    public PageResponse<Map<String, Object>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderAdminService.listOrders(status, paymentStatus, paymentMethod, search, sort, sellerId, page, size);
    }

    @GetMapping("/stats")
    public Map<String, Object> getOrderStats() {
        return orderAdminService.getOrderStats();
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public String exportOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort) {
        return orderAdminService.exportOrdersCsv(status, paymentStatus, paymentMethod, search, sort);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOrder(@PathVariable Long id) {
        return orderAdminService.getOrder(id);
    }

    @PatchMapping("/{id}/gst-status")
    public Map<String, Object> updateGstStatus(@PathVariable Long id, @RequestBody NoteRequest request) {
        return orderAdminService.updateGstStatus(id, request != null ? request.getGstStatus() : null);
    }

    @PatchMapping("/{id}/status")
    public Map<String, Object> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody NoteRequest request) {
        return orderAdminService.updateOrderStatus(
                id,
                request != null ? request.getStatus() : null,
                request != null ? request.getNote() : null,
                com.ecommerce.adminbackend.security.AdminSecurityUtils.currentAdminId(),
                request != null && Boolean.TRUE.equals(request.getNotifyCustomer()));
    }

    @GetMapping("/{id}/invoice")
    public Map<String, Object> generateInvoice(@PathVariable Long id) {
        return orderAdminService.generateInvoice(id);
    }

    @GetMapping("/{id}/invoice/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        byte[] pdf = orderAdminService.generateInvoicePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/shipping-label")
    public Map<String, Object> generateShippingLabel(@PathVariable Long id) {
        return orderAdminService.generateShippingLabel(id);
    }

    @GetMapping("/{id}/shipping-label/pdf")
    public ResponseEntity<byte[]> downloadShippingLabelPdf(@PathVariable Long id) {
        byte[] pdf = orderAdminService.generateShippingLabelPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shipping-label-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/shiprocket/push")
    public Map<String, Object> pushToShiprocket(@PathVariable Long id) {
        return orderAdminService.pushToShiprocket(id);
    }

    @PostMapping("/{id}/shiprocket/sync")
    public Map<String, Object> syncFromShiprocket(@PathVariable Long id) {
        return orderAdminService.syncFromShiprocket(id);
    }
}
