package com.ecommerce.adminbackend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LocationRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<Object[]> searchCountries(String query) {
        return entityManager.createNativeQuery("""
                SELECT id, country_name FROM countries
                WHERE (:q IS NULL OR :q = '' OR LOWER(country_name) LIKE LOWER(CONCAT('%', :q, '%')))
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
                  AND (:q IS NULL OR :q = '' OR LOWER(s.state_name) LIKE LOWER(CONCAT('%', :q, '%')))
                ORDER BY s.state_name
                LIMIT 500
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
                  AND (:q IS NULL OR :q = '' OR LOWER(c.city_name) LIKE LOWER(CONCAT('%', :q, '%')))
                ORDER BY c.city_name
                LIMIT 500
                """)
                .setParameter("stateId", stateId)
                .setParameter("q", query)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> searchPincodes(String query) {
        return entityManager.createNativeQuery("""
                SELECT p.id, p.pincode, a.area_name, c.city_name, s.state_name, co.country_name
                FROM pincodes p
                JOIN areas a ON a.id = p.area_id
                JOIN cities c ON c.id = a.city_id
                JOIN states s ON s.id = c.state_id
                JOIN countries co ON co.id = s.country_id
                WHERE (:q IS NULL OR :q = '' OR p.pincode LIKE CONCAT('%', :q, '%')
                    OR LOWER(a.area_name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(c.city_name) LIKE LOWER(CONCAT('%', :q, '%')))
                ORDER BY p.pincode, a.area_name
                LIMIT 500
                """)
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
                  AND (:q IS NULL OR :q = '' OR LOWER(a.area_name) LIKE LOWER(CONCAT('%', :q, '%')))
                ORDER BY a.area_name
                LIMIT 500
                """)
                .setParameter("cityId", cityId)
                .setParameter("q", query)
                .getResultList();
    }

    public Long countCountries() {
        return (Long) entityManager.createNativeQuery("SELECT COUNT(*) FROM countries").getSingleResult();
    }

    public Long countStates(Integer countryId) {
        String sql = countryId != null 
            ? "SELECT COUNT(*) FROM states WHERE country_id = :countryId"
            : "SELECT COUNT(*) FROM states";
        var query = entityManager.createNativeQuery(sql);
        if (countryId != null) {
            query.setParameter("countryId", countryId);
        }
        return (Long) query.getSingleResult();
    }

    public Long countCities(Integer stateId) {
        String sql = stateId != null 
            ? "SELECT COUNT(*) FROM cities WHERE state_id = :stateId"
            : "SELECT COUNT(*) FROM cities";
        var query = entityManager.createNativeQuery(sql);
        if (stateId != null) {
            query.setParameter("stateId", stateId);
        }
        return (Long) query.getSingleResult();
    }

    public Long countAreas(Integer cityId) {
        String sql = cityId != null 
            ? "SELECT COUNT(*) FROM areas WHERE city_id = :cityId"
            : "SELECT COUNT(*) FROM areas";
        var query = entityManager.createNativeQuery(sql);
        if (cityId != null) {
            query.setParameter("cityId", cityId);
        }
        return (Long) query.getSingleResult();
    }

    public Long countPincodes() {
        return (Long) entityManager.createNativeQuery("SELECT COUNT(*) FROM pincodes").getSingleResult();
    }

    public Integer insertCountry(String name) {
        return (Integer) entityManager.createNativeQuery("INSERT INTO countries (country_name) VALUES (?) RETURNING id")
                .setParameter(1, name)
                .getSingleResult();
    }

    public Integer insertState(Integer countryId, String name) {
        return (Integer) entityManager.createNativeQuery("INSERT INTO states (state_name, country_id) VALUES (?, ?) RETURNING id")
                .setParameter(1, name)
                .setParameter(2, countryId)
                .getSingleResult();
    }

    public Integer insertCity(Integer stateId, String name) {
        return (Integer) entityManager.createNativeQuery("INSERT INTO cities (city_name, state_id) VALUES (?, ?) RETURNING id")
                .setParameter(1, name)
                .setParameter(2, stateId)
                .getSingleResult();
    }

    public Integer insertArea(Integer cityId, String name) {
        return (Integer) entityManager.createNativeQuery("INSERT INTO areas (area_name, city_id) VALUES (?, ?) RETURNING id")
                .setParameter(1, name)
                .setParameter(2, cityId)
                .getSingleResult();
    }

    public Integer insertPincode(Integer areaId, String pincode) {
        return (Integer) entityManager.createNativeQuery("INSERT INTO pincodes (pincode, area_id) VALUES (?, ?) RETURNING id")
                .setParameter(1, pincode)
                .setParameter(2, areaId)
                .getSingleResult();
    }

    public void deleteCountry(Integer id) {
        entityManager.createNativeQuery("DELETE FROM countries WHERE id = ?")
                .setParameter(1, id)
                .executeUpdate();
    }

    public void deleteState(Integer id) {
        entityManager.createNativeQuery("DELETE FROM states WHERE id = ?")
                .setParameter(1, id)
                .executeUpdate();
    }

    public void deleteCity(Integer id) {
        entityManager.createNativeQuery("DELETE FROM cities WHERE id = ?")
                .setParameter(1, id)
                .executeUpdate();
    }

    public void deleteArea(Integer id) {
        entityManager.createNativeQuery("DELETE FROM areas WHERE id = ?")
                .setParameter(1, id)
                .executeUpdate();
    }

    public void deletePincode(Integer id) {
        entityManager.createNativeQuery("DELETE FROM pincodes WHERE id = ?")
                .setParameter(1, id)
                .executeUpdate();
    }

    public void updateCountry(Integer id, String name) {
        entityManager.createNativeQuery("UPDATE countries SET country_name = ? WHERE id = ?")
                .setParameter(1, name)
                .setParameter(2, id)
                .executeUpdate();
    }

    public void updateState(Integer id, String name) {
        entityManager.createNativeQuery("UPDATE states SET state_name = ? WHERE id = ?")
                .setParameter(1, name)
                .setParameter(2, id)
                .executeUpdate();
    }

    public void updateCity(Integer id, String name) {
        entityManager.createNativeQuery("UPDATE cities SET city_name = ? WHERE id = ?")
                .setParameter(1, name)
                .setParameter(2, id)
                .executeUpdate();
    }

    public void updateArea(Integer id, String name) {
        entityManager.createNativeQuery("UPDATE areas SET area_name = ? WHERE id = ?")
                .setParameter(1, name)
                .setParameter(2, id)
                .executeUpdate();
    }

    public void updatePincode(Integer id, String pincode) {
        entityManager.createNativeQuery("UPDATE pincodes SET pincode = ? WHERE id = ?")
                .setParameter(1, pincode)
                .setParameter(2, id)
                .executeUpdate();
    }
}
