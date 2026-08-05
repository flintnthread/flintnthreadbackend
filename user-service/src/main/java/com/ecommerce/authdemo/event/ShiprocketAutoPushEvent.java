package com.ecommerce.authdemo.event;

/**
 * Fired when a paid/COD order should be pushed to Shiprocket after the DB commit.
 * Handled asynchronously so checkout never waits on Shiprocket.
 */
public record ShiprocketAutoPushEvent(Long orderId, String orderNumber) {}
