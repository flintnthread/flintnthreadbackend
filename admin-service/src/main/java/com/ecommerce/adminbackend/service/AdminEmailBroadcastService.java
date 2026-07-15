package com.ecommerce.adminbackend.service;

import java.util.Map;

public interface AdminEmailBroadcastService {

    Map<String, Object> sendToCustomers(Map<String, Object> body);

    Map<String, Object> sendToSellers(Map<String, Object> body);
}
