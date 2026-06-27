package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.EarningsResponse;
import com.ecommerce.sellerbackend.dto.OrderPayoutAmountDto;
import com.ecommerce.sellerbackend.dto.PayoutRequestBody;
import com.ecommerce.sellerbackend.dto.PayoutRequestResponse;
import com.ecommerce.sellerbackend.dto.PayoutTransactionResponse;

import java.util.List;

public interface EarningsService {
    EarningsResponse getEarnings(Long sellerId);

    List<PayoutTransactionResponse> getPayouts(Long sellerId);

    PayoutRequestResponse requestPayout(Long sellerId, PayoutRequestBody body);

    OrderPayoutAmountDto lookupOrderPayoutAmount(Long sellerId, String orderKey);
}
