package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.RefundRequestDTO;
import com.ecommerce.authdemo.dto.RefundResponseDTO;

import com.ecommerce.authdemo.entity.Order;
import com.ecommerce.authdemo.entity.RefundTransaction;
import com.ecommerce.authdemo.entity.ReturnOrder;

import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;

import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.repository.RefundTransactionRepository;
import com.ecommerce.authdemo.repository.ReturnOrderRepository;

import com.ecommerce.authdemo.service.RefundService;
import com.ecommerce.authdemo.service.WalletService;

import com.ecommerce.authdemo.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundServiceImpl implements RefundService {

    private final RefundTransactionRepository refundTransactionRepository;
    private final OrderRepository orderRepository;
    private final ReturnOrderRepository returnOrderRepository;
    private final WalletService walletService;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public RefundResponseDTO processRefund(RefundRequestDTO dto) {

        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        ReturnOrder returnOrder = returnOrderRepository.findById(dto.getReturnId())
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found"));

        Long currentUserId = securityUtil.getCurrentUserId();
        if (!Objects.equals(order.getUserId(), currentUserId)) {
            throw new OrderException("Access denied");
        }

        if ("refunded".equalsIgnoreCase(returnOrder.getStatus())) {
            throw new OrderException("Refund already processed");
        }

        RefundTransaction refundTransaction;
        boolean walletCredited = false;

        if (shouldCreditWalletOnReturn(order)) {
            BigDecimal refundAmount = BigDecimal.valueOf(dto.getRefundAmount());
            walletCredited = walletService.creditOrderReturnRefund(
                    Math.toIntExact(order.getUserId()),
                    order.getId(),
                    returnOrder.getId(),
                    refundAmount,
                    order.getOrderNumber()
            );

            if (!walletCredited) {
                throw new OrderException(
                        "Return refund already added to FNT Wallet or amount is invalid"
                );
            }

            refundTransaction = RefundTransaction.builder()
                    .orderId(order.getId())
                    .returnId(returnOrder.getId())
                    .userId(order.getUserId())
                    .paymentMethod(order.getPaymentMethod())
                    .refundType("fnt_wallet")
                    .refundAmount(dto.getRefundAmount())
                    .refundStatus("processed")
                    .remarks(dto.getRemarks())
                    .build();
        } else if (isCodPaymentMethod(order.getPaymentMethod())) {
            refundTransaction = RefundTransaction.builder()
                    .orderId(order.getId())
                    .returnId(returnOrder.getId())
                    .userId(order.getUserId())
                    .paymentMethod(order.getPaymentMethod())
                    .refundType("manual")
                    .refundAmount(dto.getRefundAmount())
                    .refundStatus("pending_manual_refund")
                    .remarks(dto.getRemarks())
                    .build();
        } else {
            throw new OrderException(
                    "Return refund to FNT Wallet is only available for prepaid paid orders"
            );
        }

        refundTransaction = refundTransactionRepository.save(refundTransaction);

        returnOrder.setStatus("refunded");
        returnOrderRepository.save(returnOrder);

        order.setOrderStatus("returned");
        orderRepository.save(order);

        log.info(
                "Return refund processed orderId={} returnId={} walletCredited={}",
                order.getId(),
                returnOrder.getId(),
                walletCredited
        );

        return toResponse(refundTransaction, walletCredited);
    }

    @Override
    public List<RefundResponseDTO> getUserRefunds() {
        return refundTransactionRepository
                .findByUserId(securityUtil.getCurrentUserId())
                .stream()
                .map(row -> toResponse(row, "fnt_wallet".equalsIgnoreCase(row.getRefundType())))
                .toList();
    }

    @Override
    public List<RefundResponseDTO> getOrderRefunds(Long orderId) {
        return refundTransactionRepository
                .findByOrderId(orderId)
                .stream()
                .map(row -> toResponse(row, "fnt_wallet".equalsIgnoreCase(row.getRefundType())))
                .toList();
    }

    private boolean shouldCreditWalletOnReturn(Order order) {
        if (isCodPaymentMethod(order.getPaymentMethod())) {
            return false;
        }
        String paymentStatus = order.getPaymentStatus() != null
                ? order.getPaymentStatus().trim().toLowerCase()
                : "";
        return "paid".equals(paymentStatus)
                || "success".equals(paymentStatus)
                || "completed".equals(paymentStatus);
    }

    private boolean isCodPaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return false;
        }
        String pm = paymentMethod.trim().toLowerCase();
        return pm.contains("cod")
                || pm.contains("cash")
                || pm.equals("cash_on_delivery");
    }

    private RefundResponseDTO toResponse(
            RefundTransaction entity,
            boolean walletCredited
    ) {
        return RefundResponseDTO.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .returnId(entity.getReturnId())
                .refundAmount(entity.getRefundAmount())
                .refundStatus(entity.getRefundStatus())
                .refundType(entity.getRefundType())
                .razorpayRefundId(entity.getRazorpayRefundId())
                .remarks(entity.getRemarks())
                .createdAt(entity.getCreatedAt())
                .walletCredited(walletCredited)
                .walletCreditAmount(walletCredited ? entity.getRefundAmount() : null)
                .build();
    }
}
