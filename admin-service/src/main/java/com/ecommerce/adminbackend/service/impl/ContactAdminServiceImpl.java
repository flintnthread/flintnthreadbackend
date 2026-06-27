package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.entity.ContactInquiry;
import com.ecommerce.adminbackend.repository.ContactInquiryRepository;
import com.ecommerce.adminbackend.service.ContactAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContactAdminServiceImpl extends BaseAdminService implements ContactAdminService {

    private final ContactInquiryRepository contactRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listContacts(int page, int size) {
        var result = contactRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return PageResponse.from(result.map(this::toContact));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        long total = contactRepository.count();
        long read = contactRepository.countByStatus(true);
        long unread = contactRepository.countUnread();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("open", unread);
        stats.put("closed", read);
        stats.put("inProgress", 0L);
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getContact(Integer id) {
        return toContact(requireContact(id));
    }

    @Override
    @Transactional
    public Map<String, Object> updateStatus(Integer id, Boolean status) {
        ContactInquiry contact = requireContact(id);
        contact.setStatus(status);
        contactRepository.save(contact);
        return toContact(contact);
    }

    @Override
    @Transactional
    public Map<String, Object> reply(Integer id, String reply) {
        ContactInquiry contact = requireContact(id);
        contact.setAdminNotes(requireNonBlank(reply, "Reply message"));
        contact.setStatus(true);
        contactRepository.save(contact);
        return toContact(contact);
    }

    @Override
    @Transactional
    public void deleteContact(Integer id) {
        ContactInquiry contact = requireContact(id);
        contactRepository.delete(contact);
    }

    private ContactInquiry requireContact(Integer id) {
        return requireFound(contactRepository.findById(id), "Contact inquiry not found.");
    }

    private Map<String, Object> toContact(ContactInquiry contact) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", contact.getId());
        row.put("name", contact.getName());
        row.put("email", contact.getEmail());
        row.put("phone", contact.getPhone());
        row.put("subject", contact.getSubject());
        row.put("message", contact.getMessage());
        row.put("status", contact.getStatus());
        row.put("adminNotes", contact.getAdminNotes());
        row.put("createdAt", contact.getCreatedAt());
        row.put("updatedAt", contact.getUpdatedAt());
        return row;
    }
}
