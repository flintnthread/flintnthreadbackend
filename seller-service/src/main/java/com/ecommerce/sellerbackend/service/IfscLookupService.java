package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.profile.IfscLookupResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class IfscLookupService {

    private static final String RAZORPAY_IFSC_URL = "https://ifsc.razorpay.com/";

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public IfscLookupResponse lookup(String rawIfsc) {
        String ifsc = rawIfsc == null ? "" : rawIfsc.trim().toUpperCase(Locale.ROOT);
        if (ifsc.isBlank()) {
            return notFound(ifsc);
        }

        try {
            String body = restClient.get()
                    .uri(RAZORPAY_IFSC_URL + ifsc)
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                return notFound(ifsc);
            }

            JsonNode node = objectMapper.readTree(body);
            String bank = textOrEmpty(node, "BANK");
            String branch = textOrEmpty(node, "BRANCH");

            if (bank.isBlank() && branch.isBlank()) {
                return notFound(ifsc);
            }

            return IfscLookupResponse.builder()
                    .ifscCode(ifsc)
                    .bankName(bank)
                    .branchName(branch)
                    .found(true)
                    .build();
        } catch (HttpClientErrorException.NotFound e) {
            return notFound(ifsc);
        } catch (Exception e) {
            return notFound(ifsc);
        }
    }

    private static IfscLookupResponse notFound(String ifsc) {
        return IfscLookupResponse.builder()
                .ifscCode(ifsc)
                .bankName("")
                .branchName("")
                .found(false)
                .build();
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }
}
