package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.support.*;
import com.ecommerce.sellerbackend.entity.ContactInquiry;
import com.ecommerce.sellerbackend.entity.SellerLiveChatMessage;
import com.ecommerce.sellerbackend.repository.ContactInquiryRepository;
import com.ecommerce.sellerbackend.repository.SellerLiveChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportContactService {

    private final SellerLiveChatMessageRepository chatRepository;
    private final ContactInquiryRepository contactRepository;

    @Value("${app.support.chat.enabled:true}")
    private boolean chatEnabled;

    @Value("${app.support.email:support@flintandthread.in}")
    private String supportEmail;

    @Value("${app.support.phone:9063499092}")
    private String supportPhone;

    @Value("${app.support.phone-hours:Mon–Sun, 9 AM – 6 PM}")
    private String supportPhoneHours;

    @Value("${app.support.whatsapp:919063499092}")
    private String whatsappNumber;

    public SupportContactConfigResponse getContactConfig() {
        return SupportContactConfigResponse.builder()
                .chat(SupportContactConfigResponse.ChatContact.builder()
                        .enabled(chatEnabled)
                        .subtitle("Live Chat · Typically replies in minutes")
                        .whatsappNumber(whatsappNumber)
                        .build())
                .email(SupportContactConfigResponse.EmailContact.builder()
                        .address(supportEmail)
                        .subtitle(supportEmail)
                        .build())
                .call(SupportContactConfigResponse.CallContact.builder()
                        .phone(supportPhone)
                        .subtitle(supportPhoneHours)
                        .hours(supportPhoneHours)
                        .build())
                .build();
    }

    @Transactional
    public ContactInquiry sendEmailInquiry(SendSupportEmailRequest request) {
        ContactInquiry inquiry = ContactInquiry.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .subject("[Seller #" + request.getSellerId() + "] " + request.getSubject().trim())
                .message(request.getMessage().trim())
                .status(false)
                .build();
        return contactRepository.save(inquiry);
    }

    public List<LiveChatMessageResponse> getChatHistory(Integer sellerId) {
        return chatRepository.findBySellerIdOrderByCreatedAtAsc(sellerId).stream()
                .map(this::toChatResponse)
                .toList();
    }

    @Transactional
    public List<LiveChatMessageResponse> sendChatMessage(LiveChatMessageRequest request) {
        SellerLiveChatMessage sellerMsg = SellerLiveChatMessage.builder()
                .sellerId(request.getSellerId())
                .senderType("seller")
                .message(request.getMessage().trim())
                .build();
        chatRepository.save(sellerMsg);

        String botReply = buildBotReply(request.getMessage());
        SellerLiveChatMessage botMsg = SellerLiveChatMessage.builder()
                .sellerId(request.getSellerId())
                .senderType("bot")
                .message(botReply)
                .build();
        chatRepository.save(botMsg);

        return getChatHistory(request.getSellerId());
    }

    private String buildBotReply(String userText) {
        String l = userText.toLowerCase();
        if (l.contains("order")) {
            return "You can check your orders in the Orders section. Need help with a specific order?";
        }
        if (l.contains("pay") || l.contains("refund") || l.contains("payout")) {
            return "For payment issues, payouts are usually processed within 3–5 business days.";
        }
        if (l.contains("product")) {
            return "Browse and manage products from the Products tab in your seller dashboard.";
        }
        if (l.contains("account") || l.contains("login") || l.contains("password")) {
            return "For account issues, go to Settings → Account or use Forgot Password on the login screen.";
        }
        if (l.contains("human") || l.contains("agent") || l.contains("call")) {
            return "Our team will assist you shortly. You can also email us or call during business hours.";
        }
        return "Thanks for your message. Our support team will review it. You can also raise a ticket from Help & Support.";
    }

    private LiveChatMessageResponse toChatResponse(SellerLiveChatMessage m) {
        return LiveChatMessageResponse.builder()
                .id(m.getId())
                .sellerId(m.getSellerId())
                .senderType(m.getSenderType())
                .message(m.getMessage())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
