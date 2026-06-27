package com.ecommerce.sellerbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LocationSearchResponse {
    private Integer pincodeId;
    private String pincode;
    private Integer areaId;
    private String area;
    private Integer cityId;
    private String city;
    private Integer stateId;
    private String state;
    private Integer countryId;
    private String country;
    /** True when suggestion comes from geocoding (not yet in local DB). */
    private Boolean external;
    /** OSM place id for external suggestions. */
    private String externalId;
    /** Full display label from geocoder (Maps-style). */
    private String displayName;
    /** District (state_district) — e.g. Palnadu. Shown in suggestions between mandal and state. */
    private String district;
    /** Mandal / taluk — shown in suggestions; for villages differs from city field. */
    private String mandal;
    /** WGS84 latitude — used for place details on select (Maps-style). */
    private Double latitude;
    /** WGS84 longitude — used for place details on select (Maps-style). */
    private Double longitude;
}
