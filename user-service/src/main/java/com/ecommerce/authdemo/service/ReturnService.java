package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.CreateExchangeRequestDTO;
import com.ecommerce.authdemo.dto.CreateReturnRequestDTO;
import com.ecommerce.authdemo.entity.ReturnExchange;
import com.ecommerce.authdemo.entity.ReturnOrder;

import java.util.List;

    public interface ReturnService {

        ReturnOrder createReturnRequest(
                CreateReturnRequestDTO dto
        );

        ReturnExchange createExchangeRequest(
                CreateExchangeRequestDTO dto
        );

        List<ReturnOrder> getUserReturns();

        List<ReturnExchange> getUserExchanges();

        ReturnExchange completeExchange(
                Long exchangeId
        );

        ReturnOrder completeReturn(
                Long returnId
        );

        ReturnOrder approveReturn(
                Long returnId,
                String adminComment
        );

        ReturnOrder rejectReturn(
                Long returnId,
                String adminComment
        );

        ReturnExchange approveExchange(
                Long exchangeId,
                String adminComment
        );

        ReturnExchange rejectExchange(
                Long exchangeId,
                String adminComment
        );

    }

