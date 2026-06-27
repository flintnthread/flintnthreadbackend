package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.CreateExchangeRequestDTO;
import com.ecommerce.authdemo.dto.CreateReturnRequestDTO;
import com.ecommerce.authdemo.entity.ReturnExchange;
import com.ecommerce.authdemo.entity.ReturnOrder;
import com.ecommerce.authdemo.service.ReturnService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    // =====================================
    // CREATE RETURN REQUEST
    // =====================================

    @PostMapping("/returns")
    public ReturnOrder createReturn(

            @RequestBody
            CreateReturnRequestDTO dto
    ) {

        return returnService.createReturnRequest(
                dto
        );
    }

    // =====================================
    // CREATE EXCHANGE REQUEST
    // =====================================

    @PostMapping("/exchanges")
    public ReturnExchange createExchange(

            @RequestBody
            CreateExchangeRequestDTO dto
    ) {

        return returnService.createExchangeRequest(
                dto
        );
    }

    // =====================================
    // USER RETURNS
    // =====================================

    @GetMapping("/returns")
    public List<ReturnOrder> getReturns() {

        return returnService.getUserReturns();
    }

    // =====================================
    // USER EXCHANGES
    // =====================================

    @GetMapping("/exchanges")
    public List<ReturnExchange> getExchanges() {

        return returnService.getUserExchanges();
    }

    // =====================================
    // COMPLETE RETURN
    // =====================================

    @PostMapping("/returns/{returnId}/complete")
    public ReturnOrder completeReturn(

            @PathVariable Long returnId
    ) {

        return returnService.completeReturn(
                returnId
        );
    }

    // =====================================
    // COMPLETE EXCHANGE
    // =====================================

    @PostMapping("/exchanges/{exchangeId}/complete")
    public ReturnExchange completeExchange(

            @PathVariable Long exchangeId
    ) {

        return returnService.completeExchange(
                exchangeId
        );
    }

    // =====================================
    // APPROVE RETURN
    // =====================================

    @PostMapping("/returns/{returnId}/approve")
    public ReturnOrder approveReturn(

            @PathVariable Long returnId,

            @RequestParam(required = false)
            String adminComment
    ) {

        return returnService.approveReturn(
                returnId,
                adminComment
        );
    }

    // =====================================
    // REJECT RETURN
    // =====================================

    @PostMapping("/returns/{returnId}/reject")
    public ReturnOrder rejectReturn(

            @PathVariable Long returnId,

            @RequestParam(required = false)
            String adminComment
    ) {

        return returnService.rejectReturn(
                returnId,
                adminComment
        );
    }

    // =====================================
    // APPROVE EXCHANGE
    // =====================================

    @PostMapping("/exchanges/{exchangeId}/approve")
    public ReturnExchange approveExchange(

            @PathVariable Long exchangeId,

            @RequestParam(required = false)
            String adminComment
    ) {

        return returnService.approveExchange(
                exchangeId,
                adminComment
        );
    }

    // =====================================
    // REJECT EXCHANGE
    // =====================================

    @PostMapping("/exchanges/{exchangeId}/reject")
    public ReturnExchange rejectExchange(

            @PathVariable Long exchangeId,

            @RequestParam(required = false)
            String adminComment
    ) {

        return returnService.rejectExchange(
                exchangeId,
                adminComment
        );
    }
}