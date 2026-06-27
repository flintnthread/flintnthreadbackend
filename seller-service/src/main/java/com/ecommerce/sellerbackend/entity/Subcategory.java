package com.ecommerce.sellerbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subcategories")
@Getter
@Setter
public class Subcategory {

    @Id
    private Integer id;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "subcategory_name", nullable = false)
    private String subcategoryName;

    @Column(name = "gst_percentage")
    private java.math.BigDecimal gstPercentage;

    @Column(name = "material_slabs")
    private String materialSlabs;
}
