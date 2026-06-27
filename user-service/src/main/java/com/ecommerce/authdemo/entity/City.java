package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;

    @Column(name = "state_id", nullable = false)
    private Integer stateId;

    @Column(name = "country_id", nullable = false)
    private Integer countryId;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
