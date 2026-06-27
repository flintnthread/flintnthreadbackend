package com.ecommerce.adminbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class MarketplaceUser {

    @Id
    private Integer id;

    private String name;

    private String email;

    @Column(name = "contact_number")
    private String contactNumber;
}
