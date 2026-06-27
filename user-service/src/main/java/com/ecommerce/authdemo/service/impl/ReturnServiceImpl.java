package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.CreateExchangeRequestDTO;
import com.ecommerce.authdemo.dto.CreateReturnRequestDTO;
import com.ecommerce.authdemo.entity.*;
import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.*;
import com.ecommerce.authdemo.service.ReturnService;
import com.ecommerce.authdemo.service.ShiprocketService;
import com.ecommerce.authdemo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

    @Service
    @RequiredArgsConstructor
    @Slf4j
    public class ReturnServiceImpl implements ReturnService {

        private final ReturnOrderRepository returnOrderRepository;
        private final ReturnExchangeRepository returnExchangeRepository;
        private final OrderRepository orderRepository;
        private final OrderItemRepository orderItemRepository;
        private final SecurityUtil securityUtil;
        private final ShiprocketService shiprocketService;
        private final ProductVariantRepository productVariantRepository;

        @Override
        @Transactional
        public ReturnOrder createReturnRequest(
                CreateReturnRequestDTO dto
        ) {

            Long userId = securityUtil.getCurrentUserId();

            Order order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Order not found"));

            if (!Objects.equals(order.getUserId(), userId)) {
                throw new OrderException("Access denied");
            }

            if (!"delivered".equalsIgnoreCase(order.getOrderStatus())) {
                throw new OrderException(
                        "Only delivered orders can be returned"
                );
            }

            orderItemRepository.findById(dto.getOrderItemId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Order item not found"
                            ));

            if (returnOrderRepository
                    .findByOrderItemId(dto.getOrderItemId())
                    .isPresent()) {

                throw new OrderException(
                        "Return already exists"
                );
            }

            ReturnOrder returnOrder = ReturnOrder.builder()

                    .orderId(dto.getOrderId())

                    .orderItemId(dto.getOrderItemId())

                    .userId(userId)

                    .reason(dto.getReason())

                    .description(dto.getDescription())

                    .solution(dto.getSolution())

                    .status("pending")

                    .build();

            returnOrder = returnOrderRepository.save(returnOrder);

            try {

                shiprocketService.createReversePickup(
                        returnOrder
                );

            } catch (Exception e) {

                log.error(
                        "Reverse pickup creation failed",
                        e
                );
            }

            return returnOrder;
        }

        @Override
        @Transactional
        public ReturnExchange createExchangeRequest(
                CreateExchangeRequestDTO dto
        ) {

            Long userId = securityUtil.getCurrentUserId();

            Order order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Order not found"
                            ));

            if (!Objects.equals(order.getUserId(), userId)) {
                throw new OrderException("Access denied");
            }

            if (!"delivered".equalsIgnoreCase(order.getOrderStatus())) {
                throw new OrderException(
                        "Only delivered orders can be exchanged"
                );
            }

            OrderItem item = orderItemRepository
                    .findById(dto.getOrderItemId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Order item not found"
                            ));

            if (returnExchangeRepository
                    .findByOrderItemId(dto.getOrderItemId())
                    .isPresent()) {

                throw new OrderException(
                        "Exchange already exists"
                );
            }

            ReturnExchange exchange = ReturnExchange.builder()

                    .orderId(dto.getOrderId())

                    .orderItemId(dto.getOrderItemId())

                    .userId(userId)

                    .productId(item.getProductId())

                    .variantId(item.getVariantId())

                    .exchangeColor(dto.getExchangeColor())

                    .exchangeSize(dto.getExchangeSize())

                    .reason(dto.getReason())

                    .description(dto.getDescription())

                    .status("pending")

                    .build();

            exchange = returnExchangeRepository.save(exchange);

            try {

                shiprocketService.createExchangePickup(
                        exchange
                );

            } catch (Exception e) {

                log.error(
                        "Exchange pickup creation failed",
                        e
                );
            }

            return exchange;
        }

        @Override
        public List<ReturnOrder> getUserReturns() {

            return returnOrderRepository.findByUserId(
                    securityUtil.getCurrentUserId()
            );
        }

        @Override
        public List<ReturnExchange> getUserExchanges() {

            return returnExchangeRepository.findByUserId(
                    securityUtil.getCurrentUserId()
            );
        }

        @Override
        @Transactional
        public ReturnOrder completeReturn(
                Long returnId
        ) {

            ReturnOrder returnOrder =
                    returnOrderRepository.findById(returnId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Return request not found"
                                    ));

            if ("completed".equalsIgnoreCase(
                    returnOrder.getStatus()
            )) {

                throw new OrderException(
                        "Return already completed"
                );
            }

            OrderItem item =
                    orderItemRepository.findById(
                            returnOrder.getOrderItemId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Order item not found"
                            ));

            ProductVariant variant =
                    productVariantRepository.findById(
                            item.getVariantId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Variant not found"
                            ));

            // =========================
            // RESTORE STOCK
            // =========================

            variant.setStock(
                    variant.getStock()
                            + item.getQuantity()
            );

            productVariantRepository.save(variant);

            returnOrder.setStatus("completed");

            returnOrder.setProcessedAt(
                    java.time.LocalDateTime.now()
            );

            returnOrder =
                    returnOrderRepository.save(returnOrder);

            log.info(
                    "Return completed id={}",
                    returnId
            );

            return returnOrder;
        }


        @Override
        @Transactional
        public ReturnExchange completeExchange(
                Long exchangeId
        ) {

            ReturnExchange exchange =
                    returnExchangeRepository.findById(exchangeId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Exchange request not found"
                                    ));

            if ("completed".equalsIgnoreCase(
                    exchange.getStatus()
            )) {

                throw new OrderException(
                        "Exchange already completed"
                );
            }

            OrderItem oldItem =
                    orderItemRepository.findById(
                            exchange.getOrderItemId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Order item not found"
                            ));

            ProductVariant oldVariant =
                    productVariantRepository.findById(
                            oldItem.getVariantId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Old variant not found"
                            ));

            // =========================
            // RESTORE OLD STOCK
            // =========================

            oldVariant.setStock(
                    oldVariant.getStock()
                            + oldItem.getQuantity()
            );

            productVariantRepository.save(oldVariant);

            // =========================
            // NEW VARIANT
            // =========================

            ProductVariant newVariant =
                    productVariantRepository.findById(
                            exchange.getVariantId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Replacement variant not found"
                            ));

            if (newVariant.getStock()
                    < oldItem.getQuantity()) {

                throw new OrderException(
                        "Replacement product out of stock"
                );
            }

            // =========================
            // DEDUCT NEW STOCK
            // =========================

            newVariant.setStock(
                    newVariant.getStock()
                            - oldItem.getQuantity()
            );

            productVariantRepository.save(newVariant);

            // =========================
            // CREATE REPLACEMENT ITEM
            // =========================

            OrderItem replacementItem =
                    OrderItem.builder()

                            .orderId(oldItem.getOrderId())

                            .productId(oldItem.getProductId())

                            .variantId(newVariant.getId())

                            .quantity(oldItem.getQuantity())

                            .price(oldItem.getPrice())

                            .total(oldItem.getTotal())

                            .status("replacement")

                            .sellerId(oldItem.getSellerId())

                            .productImagePath(
                                    oldItem.getProductImagePath()
                            )

                            .build();

            orderItemRepository.save(replacementItem);

            exchange.setStatus("completed");

            exchange.setProcessedAt(
                    java.time.LocalDateTime.now()
            );

            exchange =
                    returnExchangeRepository.save(exchange);

            log.info(
                    "Exchange completed id={}",
                    exchangeId
            );

            return exchange;
        }

        @Override
        @Transactional
        public ReturnOrder approveReturn(
                Long returnId,
                String adminComment
        ) {

            ReturnOrder returnOrder =
                    returnOrderRepository.findById(returnId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Return request not found"
                                    ));

            if ("approved".equalsIgnoreCase(
                    returnOrder.getStatus()
            )) {

                throw new OrderException(
                        "Return already approved"
                );
            }

            returnOrder.setStatus("approved");

            returnOrder.setAdminComment(
                    adminComment
            );

            returnOrder.setProcessedAt(
                    java.time.LocalDateTime.now()
            );

            returnOrder =
                    returnOrderRepository.save(returnOrder);

            try {

                shiprocketService.createReversePickup(
                        returnOrder
                );

            } catch (Exception e) {

                log.error(
                        "Reverse pickup creation failed",
                        e
                );
            }

            return returnOrder;
        }

        @Override
        @Transactional
        public ReturnOrder rejectReturn(
                Long returnId,
                String adminComment
        ) {

            ReturnOrder returnOrder =
                    returnOrderRepository.findById(returnId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Return request not found"
                                    ));

            returnOrder.setStatus("rejected");

            returnOrder.setAdminComment(
                    adminComment
            );

            returnOrder.setProcessedAt(
                    java.time.LocalDateTime.now()
            );

            return returnOrderRepository.save(
                    returnOrder
            );
        }
        @Override
        @Transactional
        public ReturnExchange approveExchange(
                Long exchangeId,
                String adminComment
        ) {

            ReturnExchange exchange =
                    returnExchangeRepository.findById(exchangeId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Exchange request not found"
                                    ));

            if ("approved".equalsIgnoreCase(
                    exchange.getStatus()
            )) {

                throw new OrderException(
                        "Exchange already approved"
                );
            }

            exchange.setStatus("approved");

            exchange.setAdminComment(
                    adminComment
            );

            exchange.setProcessedAt(
                    java.time.LocalDateTime.now()
            );

            exchange =
                    returnExchangeRepository.save(exchange);

            try {

                shiprocketService.createExchangePickup(
                        exchange
                );

            } catch (Exception e) {

                log.error(
                        "Exchange pickup creation failed",
                        e
                );
            }

            return exchange;
        }

        @Override
        @Transactional
        public ReturnExchange rejectExchange(
                Long exchangeId,
                String adminComment
        ) {

            ReturnExchange exchange =
                    returnExchangeRepository.findById(exchangeId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Exchange request not found"
                                    ));

            exchange.setStatus("rejected");

            exchange.setAdminComment(
                    adminComment
            );

            exchange.setProcessedAt(
                    java.time.LocalDateTime.now()
            );

            return returnExchangeRepository.save(
                    exchange
            );
        }


    }

