package com.ecommerce.authdemo.event;

/** Fired after an order row is committed so invoice generation cannot roll back checkout. */
public record OrderPlacedEvent(Integer orderId) {}
