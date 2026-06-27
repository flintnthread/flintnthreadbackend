package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.InvoiceRequest;
import com.ecommerce.authdemo.dto.InvoiceResponse;
import com.ecommerce.authdemo.dto.OrderResponseDTO;
import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.service.InvoiceService;
import com.ecommerce.authdemo.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getByOrderId(
            @RequestParam Integer orderId) {
        List<InvoiceResponse> data = invoiceService.getByOrderId(orderId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Invoices fetched successfully", data));
    }

    @GetMapping("/by-number/{invoiceNumber}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getByInvoiceNumber(
            @PathVariable String invoiceNumber) {
        InvoiceResponse data = invoiceService.getByInvoiceNumber(invoiceNumber);
        return ResponseEntity.ok(new ApiResponse<>(true, "Invoice fetched successfully", data));
    }

    /** Invoice QR scan — public, no login (product-scoped order line). */
    @GetMapping("/qr-order")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getQrOrderLine(
            @RequestParam Long orderId,
            @RequestParam String orderNumber,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer lineIndex,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) String productIds) {
        if (orderId == null || orderId <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Valid order ID is required", null));
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Order number is required", null));
        }
        try {
            OrderResponseDTO order = orderService.getPublicOrderDetails(
                    orderId, orderNumber, productId, lineIndex, sellerId, productIds
            );
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Order line fetched successfully", order)
            );
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (OrderException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to load order line", null));
        }
    }

    /** Invoice QR scan — public HTML page (no Expo router). */
    @GetMapping(value = "/view-html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> viewHtmlByScope(
            @RequestParam(name = "order_id", required = false) Integer orderIdSnake,
            @RequestParam(name = "orderId", required = false) Integer orderIdCamel,
            @RequestParam(name = "seller_id", required = false) Long sellerIdSnake,
            @RequestParam(name = "sellerId", required = false) Long sellerIdCamel,
            @RequestParam(name = "product_id", required = false) Long productIdSnake,
            @RequestParam(name = "productId", required = false) Long productIdCamel,
            @RequestParam(name = "line_index", required = false) Integer lineIndexSnake,
            @RequestParam(name = "lineIndex", required = false) Integer lineIndexCamel
    ) {
        Integer orderId = orderIdSnake != null ? orderIdSnake : orderIdCamel;
        Long sellerId = sellerIdSnake != null ? sellerIdSnake : sellerIdCamel;
        Long productId = productIdSnake != null ? productIdSnake : productIdCamel;
        Integer lineIndex = lineIndexSnake != null ? lineIndexSnake : lineIndexCamel;

        if (orderId == null || orderId <= 0) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><p>Invalid order.</p></body></html>");
        }
        try {
            String html = invoiceService.getPublicViewHtml(orderId, sellerId, productId, lineIndex);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .body(html);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><p>Invoice not found.</p></body></html>");
        } catch (OrderException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><p>" + e.getMessage() + "</p></body></html>");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><p>Could not load invoice.</p></body></html>");
        }
    }

    @GetMapping(value = "/{id}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getInvoiceHtml(@PathVariable Integer id) {
        String html = invoiceService.getInvoiceHtml(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                .body(html);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(
            @Valid @RequestBody InvoiceRequest request) {
        InvoiceResponse data = invoiceService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Invoice created successfully", data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody InvoiceRequest request) {
        InvoiceResponse data = invoiceService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Invoice updated successfully", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Integer id) {
        invoiceService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Invoice deleted successfully", "OK"));
    }
}
