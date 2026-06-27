package com.ecommerce.sellerbackend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;

@Repository
public class LocationRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<Object[]> searchCountries(String query) {
        return entityManager.createNativeQuery("""
                SELECT id, country_name FROM countries
                WHERE (:q IS NULL OR :q = ''
                    OR LOWER(country_name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(REPLACE(country_name, ' ', '')) LIKE LOWER(REPLACE(CONCAT('%', :q, '%'), ' ', '')))
                ORDER BY country_name
                LIMIT 500
                """)
                .setParameter("q", query)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> searchStates(Integer countryId, String query) {
        return entityManager.createNativeQuery("""
                SELECT s.id, s.state_name, s.country_id, c.country_name
                FROM states s
                JOIN countries c ON c.id = s.country_id
                WHERE (:countryId IS NULL OR s.country_id = :countryId)
                  AND (:q IS NULL OR :q = ''
                    OR LOWER(s.state_name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(REPLACE(s.state_name, ' ', '')) LIKE LOWER(REPLACE(CONCAT('%', :q, '%'), ' ', '')))
                ORDER BY s.state_name
                LIMIT 2000
                """)
                .setParameter("countryId", countryId)
                .setParameter("q", query)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> searchCities(Integer stateId, String query) {
        return entityManager.createNativeQuery("""
                SELECT c.id, c.city_name, c.state_id, s.state_name
                FROM cities c
                JOIN states s ON s.id = c.state_id
                WHERE (:stateId IS NULL OR c.state_id = :stateId)
                  AND (:q IS NULL OR :q = ''
                    OR LOWER(c.city_name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(REPLACE(c.city_name, ' ', '')) LIKE LOWER(REPLACE(CONCAT('%', :q, '%'), ' ', '')))
                ORDER BY c.city_name
                LIMIT 5000
                """)
                .setParameter("stateId", stateId)
                .setParameter("q", query)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> searchAreas(Integer cityId, String query) {
        return entityManager.createNativeQuery("""
                SELECT a.id, a.area_name, a.city_id, c.city_name
                FROM areas a
                JOIN cities c ON c.id = a.city_id
                WHERE (:cityId IS NULL OR a.city_id = :cityId)
                  AND (:q IS NULL OR :q = ''
                    OR LOWER(a.area_name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(REPLACE(a.area_name, ' ', '')) LIKE LOWER(REPLACE(CONCAT('%', :q, '%'), ' ', '')))
                ORDER BY a.area_name
                LIMIT 5000
                """)
                .setParameter("cityId", cityId)
                .setParameter("q", query)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> searchPincodes(Integer areaId, String query) {
        return entityManager.createNativeQuery("""
                SELECT p.id, p.pincode, p.area_id, a.area_name
                FROM pincodes p
                JOIN areas a ON a.id = p.area_id
                WHERE (:areaId IS NULL OR p.area_id = :areaId)
                  AND (:q IS NULL OR :q = '' OR p.pincode LIKE CONCAT('%', :q, '%'))
                ORDER BY p.pincode
                LIMIT 5000
                """)
                .setParameter("areaId", areaId)
                .setParameter("q", query)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> searchLocationsByPincode(String query, String country) {
        return entityManager.createNativeQuery("""
                SELECT p.id, p.pincode, a.id, a.area_name, c.id, c.city_name, s.id, s.state_name, co.id, co.country_name
                FROM pincodes p
                JOIN areas a ON a.id = p.area_id
                JOIN cities c ON c.id = a.city_id
                JOIN states s ON s.id = c.state_id
                JOIN countries co ON co.id = s.country_id
                WHERE (:country IS NULL OR :country = ''
                    OR LOWER(co.country_name) = LOWER(:country)
                    OR LOWER(REPLACE(co.country_name, ' ', '')) = LOWER(REPLACE(:country, ' ', '')))
                  AND (:q IS NULL OR :q = '' OR p.pincode LIKE CONCAT('%', :q, '%'))
                ORDER BY
                    CASE WHEN p.pincode = :q THEN 0
                         WHEN p.pincode LIKE CONCAT(:q, '%') THEN 1
                         ELSE 2 END,
                    a.area_name, c.city_name, p.pincode
                LIMIT 300
                """)
                .setParameter("q", query)
                .setParameter("country", country)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> searchLocations(String query, String country) {
        return entityManager.createNativeQuery("""
                SELECT p.id, p.pincode, a.id, a.area_name, c.id, c.city_name, s.id, s.state_name, co.id, co.country_name
                FROM areas a
                JOIN cities c ON c.id = a.city_id
                JOIN states s ON s.id = c.state_id
                JOIN countries co ON co.id = s.country_id
                LEFT JOIN pincodes p ON p.id = (
                    SELECT p2.id FROM pincodes p2
                    WHERE p2.area_id = a.id
                    ORDER BY p2.id
                    LIMIT 1
                )
                WHERE (:country IS NULL OR :country = ''
                    OR LOWER(co.country_name) = LOWER(:country)
                    OR LOWER(REPLACE(co.country_name, ' ', '')) = LOWER(REPLACE(:country, ' ', '')))
                  AND (:q IS NULL OR :q = ''
                    OR (p.pincode IS NOT NULL AND p.pincode LIKE CONCAT('%', :q, '%'))
                    OR LOWER(a.area_name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(REPLACE(a.area_name, ' ', '')) LIKE LOWER(REPLACE(CONCAT('%', :q, '%'), ' ', ''))
                    OR LOWER(c.city_name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(REPLACE(c.city_name, ' ', '')) LIKE LOWER(REPLACE(CONCAT('%', :q, '%'), ' ', ''))
                    OR LOWER(s.state_name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(REPLACE(s.state_name, ' ', '')) LIKE LOWER(REPLACE(CONCAT('%', :q, '%'), ' ', '')))
                ORDER BY
                    CASE WHEN LOWER(a.area_name) = LOWER(:q) THEN 0
                         WHEN LOWER(c.city_name) = LOWER(:q) THEN 1
                         WHEN LOWER(a.area_name) LIKE LOWER(CONCAT(:q, '%')) THEN 2
                         WHEN LOWER(c.city_name) LIKE LOWER(CONCAT(:q, '%')) THEN 3
                         WHEN p.pincode IS NOT NULL AND p.pincode = :q THEN 4
                         WHEN p.pincode IS NOT NULL AND p.pincode LIKE CONCAT(:q, '%') THEN 5
                         WHEN LOWER(a.area_name) LIKE LOWER(CONCAT('%', :q, '%')) THEN 6
                         WHEN LOWER(c.city_name) LIKE LOWER(CONCAT('%', :q, '%')) THEN 7
                         WHEN LOWER(s.state_name) LIKE LOWER(CONCAT('%', :q, '%')) THEN 8
                         ELSE 9 END,
                    a.area_name, c.city_name, p.pincode
                LIMIT 300
                """)
                .setParameter("q", query)
                .setParameter("country", country)
                .getResultList();
    }

    public Integer findCountryIdByName(String name) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT id FROM countries
                WHERE LOWER(TRIM(country_name)) = LOWER(TRIM(:name))
                LIMIT 1
                """)
                .setParameter("name", name)
                .getResultList();
        return rows.isEmpty() ? null : ((Number) rows.get(0)).intValue();
    }

    public Integer insertCountry(String name) {
        Integer nextId = nextId("countries");
        entityManager.createNativeQuery("""
                INSERT INTO countries (id, country_name, country_code, status, created_at)
                VALUES (:id, :name, :code, 1, NOW())
                """)
                .setParameter("id", nextId)
                .setParameter("name", name.trim())
                .setParameter("code", resolveCountryCode(name))
                .executeUpdate();
        return nextId;
    }

    public Integer findStateIdByName(Integer countryId, String name) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT id FROM states
                WHERE country_id = :countryId
                  AND LOWER(TRIM(state_name)) = LOWER(TRIM(:name))
                LIMIT 1
                """)
                .setParameter("countryId", countryId)
                .setParameter("name", name)
                .getResultList();
        return rows.isEmpty() ? null : ((Number) rows.get(0)).intValue();
    }

    public Integer insertState(Integer countryId, String name) {
        Integer nextId = nextId("states");
        entityManager.createNativeQuery("""
                INSERT INTO states (id, state_name, country_id, status, created_at)
                VALUES (:id, :name, :countryId, 1, NOW())
                """)
                .setParameter("id", nextId)
                .setParameter("name", name.trim())
                .setParameter("countryId", countryId)
                .executeUpdate();
        return nextId;
    }

    public Integer findCityIdByName(Integer stateId, String name) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT id FROM cities
                WHERE state_id = :stateId
                  AND LOWER(TRIM(city_name)) = LOWER(TRIM(:name))
                LIMIT 1
                """)
                .setParameter("stateId", stateId)
                .setParameter("name", name)
                .getResultList();
        return rows.isEmpty() ? null : ((Number) rows.get(0)).intValue();
    }

    public Integer insertCity(Integer stateId, Integer countryId, String name) {
        Integer nextId = nextId("cities");
        entityManager.createNativeQuery("""
                INSERT INTO cities (id, city_name, state_id, country_id, status, created_at)
                VALUES (:id, :name, :stateId, :countryId, 1, NOW())
                """)
                .setParameter("id", nextId)
                .setParameter("name", name.trim())
                .setParameter("stateId", stateId)
                .setParameter("countryId", countryId)
                .executeUpdate();
        return nextId;
    }

    public Integer findAreaIdByName(Integer cityId, String name) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT id FROM areas
                WHERE city_id = :cityId
                  AND LOWER(TRIM(area_name)) = LOWER(TRIM(:name))
                LIMIT 1
                """)
                .setParameter("cityId", cityId)
                .setParameter("name", name)
                .getResultList();
        return rows.isEmpty() ? null : ((Number) rows.get(0)).intValue();
    }

    public Integer insertArea(Integer cityId, Integer stateId, Integer countryId, String name) {
        Integer nextId = nextId("areas");
        entityManager.createNativeQuery("""
                INSERT INTO areas (id, area_name, city_id, state_id, country_id, status, created_at)
                VALUES (:id, :name, :cityId, :stateId, :countryId, 1, NOW())
                """)
                .setParameter("id", nextId)
                .setParameter("name", name.trim())
                .setParameter("cityId", cityId)
                .setParameter("stateId", stateId)
                .setParameter("countryId", countryId)
                .executeUpdate();
        return nextId;
    }

    @SuppressWarnings("unchecked")
    public Integer[] findAreaHierarchyIds(Integer areaId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT co.id, s.id, c.id
                FROM areas a
                JOIN cities c ON c.id = a.city_id
                JOIN states s ON s.id = c.state_id
                JOIN countries co ON co.id = s.country_id
                WHERE a.id = :areaId
                LIMIT 1
                """)
                .setParameter("areaId", areaId)
                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = rows.get(0);
        return new Integer[]{
                ((Number) row[0]).intValue(),
                ((Number) row[1]).intValue(),
                ((Number) row[2]).intValue()
        };
    }

    public String findFirstPincodeByAreaId(Integer areaId) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT pincode FROM pincodes
                WHERE area_id = :areaId
                ORDER BY id
                LIMIT 1
                """)
                .setParameter("areaId", areaId)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0).toString();
    }

    public Integer findPincodeIdByAreaAndCode(Integer areaId, String pincode) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT id FROM pincodes
                WHERE area_id = :areaId AND pincode = :pincode
                LIMIT 1
                """)
                .setParameter("areaId", areaId)
                .setParameter("pincode", pincode.trim())
                .getResultList();
        return rows.isEmpty() ? null : ((Number) rows.get(0)).intValue();
    }

    public Integer insertPincode(Integer countryId, Integer stateId, Integer cityId, Integer areaId, String pincode) {
        Integer nextId = nextId("pincodes");
        entityManager.createNativeQuery("""
                INSERT INTO pincodes (id, country_id, state_id, city_id, area_id, pincode, status, created_at, updated_at)
                VALUES (:id, :countryId, :stateId, :cityId, :areaId, :pincode, 1, NOW(), NOW())
                """)
                .setParameter("id", nextId)
                .setParameter("countryId", countryId)
                .setParameter("stateId", stateId)
                .setParameter("cityId", cityId)
                .setParameter("areaId", areaId)
                .setParameter("pincode", pincode.trim())
                .executeUpdate();
        return nextId;
    }

    private static String resolveCountryCode(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if ("india".equals(normalized)) {
            return "IN";
        }
        String trimmed = name.trim();
        return trimmed.length() >= 2
                ? trimmed.substring(0, 2).toUpperCase(Locale.ROOT)
                : trimmed.toUpperCase(Locale.ROOT);
    }

    private Integer nextId(String table) {
        Number max = (Number) entityManager.createNativeQuery(
                "SELECT COALESCE(MAX(id), 0) + 1 FROM " + table
        ).getSingleResult();
        return max.intValue();
    }
}
