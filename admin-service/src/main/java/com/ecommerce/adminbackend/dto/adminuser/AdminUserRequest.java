package com.ecommerce.adminbackend.dto.adminuser;

import com.ecommerce.adminbackend.entity.AdminRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserRequest {
    private String email;
    private String fullName;
    private String password;
    private AdminRole role;
    private Boolean active;
}
