package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.dto.common.NoteRequest;
import com.ecommerce.adminbackend.service.ContactAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/contacts")
@RequiredArgsConstructor
public class ContactAdminController {

    private static final Logger log = LogFactory.getLogger(ContactAdminController.class);

    private final ContactAdminService contactAdminService;

    @GetMapping
    public PageResponse<Map<String, Object>> listContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return contactAdminService.listContacts(page, size);
    }

    @PostMapping
    public Map<String, Object> createContact(@RequestBody Map<String, Object> body) {
        return contactAdminService.createContact(body);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return contactAdminService.stats();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getContact(@PathVariable Integer id) {
        return contactAdminService.getContact(id);
    }

    @PatchMapping("/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Integer id, @RequestBody NoteRequest request) {
        return contactAdminService.updateStatus(id, request != null ? request.getActive() : null);
    }

    @PostMapping("/{id}/reply")
    public Map<String, Object> reply(@PathVariable Integer id, @RequestBody NoteRequest request) {
        return contactAdminService.reply(id, request != null ? request.getReply() : null);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public void deleteContact(@PathVariable Integer id) {
        contactAdminService.deleteContact(id);
    }
}
