package com.ecommerce.adminbackend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;

@Repository
public class LocationRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<Object[]> searchCountries(String query, int page, int size) {
        Query q = entityManager.createNativeQuery("""
                SELECT id, country_name, country_code, status
                FROM countries
                WHERE (:q IS NULL OR :q = '' OR LOWER(country_name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(country_code) LIKE LOWER(CONCAT('%', :q, '%')))
                ORDER BY country_name
                """)
                .setParameter("q", query);
        applyPaging(q, page, size);
        return q.getResultList();
    }

    public long countCountriesSearch(String query) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM countries
                WHERE (:q IS NULL OR :q = '' OR LOWER(country_name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(country_code) LIKE LOWER(CONCAT('%', :q, '%')))
                """)
                .setParameter("q", query)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> searchStates(Integer countryId, String query, int page, int size) {
        Query q = entityManager.createNativeQuery("""
                SELECT s.id, s.state_name, s.country_id, c.country_name, s.status
                FROM states s
                JOIN countries c ON c.id = s.country_id
                WHERE (:countryId IS NULL OR s.country_id = :countryId)
                  AND (:q IS NULL OR :q = '' OR LOWER(s.state_name) LIKE LOWER(CONCAT('%', :q, '%')))
                ORDER BY s.state_name
                """)
                .setParameter("countryId", countryId)
                .setParameter("q", query);
        applyPaging(q, page, size);
        return q.getResultList();
    }

    public long countStatesSearch(Integer countryId, String query) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM states s
                WHERE (:countryId IS NULL OR s.country_id = :countryId)
                  AND (:q IS NULL OR :q = '' OR LOWER(s.state_name) LIKE LOWER(CONCAT('%', :q, '%')))
                """)
                .setParameter("countryId", countryId)
                .setParameter("q", query)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> searchCities(Integer stateId, String query, int page, int size) {
        Query q = entityManager.createNativeQuery("""
                SELECT c.id, c.city_name, c.state_id, s.state_name, c.status
                FROM cities c
                JOIN states s ON s.id = c.state_id
                WHERE (:stateId IS NULL OR c.state_id = :stateId)
                  AND (:q IS NULL OR :q = '' OR LOWER(c.city_name) LIKE LOWER(CONCAT('%', :q, '%')))
                ORDER BY c.city_name
                """)
                .setParameter("stateId", stateId)
                .setParameter("q", query);
        applyPaging(q, page, size);
        return q.getResultList();
    }

    public long countCitiesSearch(Integer stateId, String query) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM cities c
                WHERE (:stateId IS NULL OR c.state_id = :stateId)
                  AND (:q IS NULL OR :q = '' OR LOWER(c.city_name) LIKE LOWER(CONCAT('%', :q, '%')))
                """)
                .setParameter("stateId", stateId)
                .setParameter("q", query)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> searchPincodes(Integer cityId, Integer areaId, String query, int page, int size) {
        Query q = entityManager.createNativeQuery("""
                SELECT p.id, p.pincode, a.area_name, c.city_name, s.state_name, co.country_name,
                       p.city_id, p.area_id
                FROM pincodes p
                JOIN areas a ON a.id = p.area_id
                JOIN cities c ON c.id = a.city_id
                JOIN states s ON s.id = c.state_id
                JOIN countries co ON co.id = s.country_id
                WHERE (:cityId IS NULL OR p.city_id = :cityId OR a.city_id = :cityId)
                  AND (:areaId IS NULL OR p.area_id = :areaId)
                  AND (:q IS NULL OR :q = '' OR p.pincode LIKE CONCAT('%', :q, '%')
                    OR LOWER(a.area_name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(c.city_name) LIKE LOWER(CONCAT('%', :q, '%')))
                ORDER BY p.pincode, a.area_name
                """)
                .setParameter("cityId", cityId)
                .setParameter("areaId", areaId)
                .setParameter("q", query);
        applyPaging(q, page, size);
        return q.getResultList();
    }

    public long countPincodesSearch(Integer cityId, Integer areaId, String query) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM pincodes p
                JOIN areas a ON a.id = p.area_id
                JOIN cities c ON c.id = a.city_id
                WHERE (:cityId IS NULL OR p.city_id = :cityId OR a.city_id = :cityId)
                  AND (:areaId IS NULL OR p.area_id = :areaId)
                  AND (:q IS NULL OR :q = '' OR p.pincode LIKE CONCAT('%', :q, '%')
                    OR LOWER(a.area_name) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(c.city_name) LIKE LOWER(CONCAT('%', :q, '%')))
                """)
                .setParameter("cityId", cityId)
                .setParameter("areaId", areaId)
                .setParameter("q", query)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> searchAreas(Integer cityId, String query, int page, int size) {
        Query q = entityManager.createNativeQuery("""
                SELECT a.id, a.area_name, a.city_id, c.city_name, a.status
                FROM areas a
                JOIN cities c ON c.id = a.city_id
                WHERE (:cityId IS NULL OR a.city_id = :cityId)
                  AND (:q IS NULL OR :q = '' OR LOWER(a.area_name) LIKE LOWER(CONCAT('%', :q, '%')))
                ORDER BY a.area_name
                """)
                .setParameter("cityId", cityId)
                .setParameter("q", query);
        applyPaging(q, page, size);
        return q.getResultList();
    }

    public long countAreasSearch(Integer cityId, String query) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM areas a
                WHERE (:cityId IS NULL OR a.city_id = :cityId)
                  AND (:q IS NULL OR :q = '' OR LOWER(a.area_name) LIKE LOWER(CONCAT('%', :q, '%')))
                """)
                .setParameter("cityId", cityId)
                .setParameter("q", query)
                .getSingleResult()).longValue();
    }

    public Long countCountries() {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM countries").getSingleResult()).longValue();
    }

    public Long countStates(Integer countryId) {
        String sql = countryId != null
                ? "SELECT COUNT(*) FROM states WHERE country_id = :countryId"
                : "SELECT COUNT(*) FROM states";
        var query = entityManager.createNativeQuery(sql);
        if (countryId != null) {
            query.setParameter("countryId", countryId);
        }
        return ((Number) query.getSingleResult()).longValue();
    }

    public Long countCities(Integer stateId) {
        String sql = stateId != null
                ? "SELECT COUNT(*) FROM cities WHERE state_id = :stateId"
                : "SELECT COUNT(*) FROM cities";
        var query = entityManager.createNativeQuery(sql);
        if (stateId != null) {
            query.setParameter("stateId", stateId);
        }
        return ((Number) query.getSingleResult()).longValue();
    }

    public Long countAreas(Integer cityId) {
        String sql = cityId != null
                ? "SELECT COUNT(*) FROM areas WHERE city_id = :cityId"
                : "SELECT COUNT(*) FROM areas";
        var query = entityManager.createNativeQuery(sql);
        if (cityId != null) {
            query.setParameter("cityId", cityId);
        }
        return ((Number) query.getSingleResult()).longValue();
    }

    public Long countPincodes() {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM pincodes").getSingleResult()).longValue();
    }

    public Integer insertCountry(String name, String code, boolean active) {
        Integer nextId = nextId("countries");
        entityManager.createNativeQuery("""
                INSERT INTO countries (id, country_name, country_code, status, created_at)
                VALUES (:id, :name, :code, :status, NOW())
                """)
                .setParameter("id", nextId)
                .setParameter("name", name.trim())
                .setParameter("code", code.trim().toUpperCase(Locale.ROOT))
                .setParameter("status", active ? 1 : 0)
                .executeUpdate();
        return nextId;
    }

    public Integer insertState(Integer countryId, String name, boolean active) {
        Integer nextId = nextId("states");
        entityManager.createNativeQuery("""
                INSERT INTO states (id, state_name, country_id, status, created_at)
                VALUES (:id, :name, :countryId, :status, NOW())
                """)
                .setParameter("id", nextId)
                .setParameter("name", name.trim())
                .setParameter("countryId", countryId)
                .setParameter("status", active ? 1 : 0)
                .executeUpdate();
        return nextId;
    }

    public Integer insertCity(Integer stateId, Integer countryId, String name, boolean active) {
        Integer nextId = nextId("cities");
        entityManager.createNativeQuery("""
                INSERT INTO cities (id, city_name, state_id, country_id, status, created_at)
                VALUES (:id, :name, :stateId, :countryId, :status, NOW())
                """)
                .setParameter("id", nextId)
                .setParameter("name", name.trim())
                .setParameter("stateId", stateId)
                .setParameter("countryId", countryId)
                .setParameter("status", active ? 1 : 0)
                .executeUpdate();
        return nextId;
    }

    public Integer insertArea(Integer cityId, Integer stateId, Integer countryId, String name, boolean active) {
        Integer nextId = nextId("areas");
        entityManager.createNativeQuery("""
                INSERT INTO areas (id, area_name, city_id, state_id, country_id, status, created_at)
                VALUES (:id, :name, :cityId, :stateId, :countryId, :status, NOW())
                """)
                .setParameter("id", nextId)
                .setParameter("name", name.trim())
                .setParameter("cityId", cityId)
                .setParameter("stateId", stateId)
                .setParameter("countryId", countryId)
                .setParameter("status", active ? 1 : 0)
                .executeUpdate();
        return nextId;
    }

    public Integer insertPincode(Integer countryId, Integer stateId, Integer cityId, Integer areaId, String pincode, boolean active) {
        Integer nextId = nextId("pincodes");
        entityManager.createNativeQuery("""
                INSERT INTO pincodes (id, country_id, state_id, city_id, area_id, pincode, status, created_at, updated_at)
                VALUES (:id, :countryId, :stateId, :cityId, :areaId, :pincode, :status, NOW(), NOW())
                """)
                .setParameter("id", nextId)
                .setParameter("countryId", countryId)
                .setParameter("stateId", stateId)
                .setParameter("cityId", cityId)
                .setParameter("areaId", areaId)
                .setParameter("pincode", pincode.trim())
                .setParameter("status", active ? 1 : 0)
                .executeUpdate();
        return nextId;
    }

    @SuppressWarnings("unchecked")
    public Integer findCountryIdByStateId(Integer stateId) {
        List<?> rows = entityManager.createNativeQuery("SELECT country_id FROM states WHERE id = :id LIMIT 1")
                .setParameter("id", stateId)
                .getResultList();
        return rows.isEmpty() ? null : ((Number) rows.get(0)).intValue();
    }

    @SuppressWarnings("unchecked")
    public Integer[] findCityHierarchyIds(Integer cityId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT c.country_id, c.state_id
                FROM cities c
                WHERE c.id = :cityId
                LIMIT 1
                """)
                .setParameter("cityId", cityId)
                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = rows.get(0);
        return new Integer[]{((Number) row[0]).intValue(), ((Number) row[1]).intValue()};
    }

    @SuppressWarnings("unchecked")
    public Integer[] findAreaHierarchyIds(Integer areaId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT a.country_id, a.state_id, a.city_id
                FROM areas a
                WHERE a.id = :areaId
                LIMIT 1
                """)
                .setParameter("areaId", areaId)
                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = rows.get(0);
        return new Integer[]{((Number) row[0]).intValue(), ((Number) row[1]).intValue(), ((Number) row[2]).intValue()};
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

    public void updateCountry(Integer id, String name, String code, boolean active) {
        entityManager.createNativeQuery("""
                UPDATE countries
                SET country_name = :name, country_code = :code, status = :status
                WHERE id = :id
                """)
                .setParameter("name", name.trim())
                .setParameter("code", code.trim().toUpperCase(Locale.ROOT))
                .setParameter("status", active ? 1 : 0)
                .setParameter("id", id)
                .executeUpdate();
    }

    public void updateState(Integer id, String name, boolean active) {
        entityManager.createNativeQuery("UPDATE states SET state_name = :name, status = :status WHERE id = :id")
                .setParameter("name", name.trim())
                .setParameter("status", active ? 1 : 0)
                .setParameter("id", id)
                .executeUpdate();
    }

    public void updateCity(Integer id, String name, boolean active) {
        entityManager.createNativeQuery("UPDATE cities SET city_name = :name, status = :status WHERE id = :id")
                .setParameter("name", name.trim())
                .setParameter("status", active ? 1 : 0)
                .setParameter("id", id)
                .executeUpdate();
    }

    public void updateArea(Integer id, String name, boolean active) {
        entityManager.createNativeQuery("UPDATE areas SET area_name = :name, status = :status WHERE id = :id")
                .setParameter("name", name.trim())
                .setParameter("status", active ? 1 : 0)
                .setParameter("id", id)
                .executeUpdate();
    }

    public void updatePincode(Integer id, String pincode, boolean active) {
        entityManager.createNativeQuery("UPDATE pincodes SET pincode = :pincode, status = :status, updated_at = NOW() WHERE id = :id")
                .setParameter("pincode", pincode.trim())
                .setParameter("status", active ? 1 : 0)
                .setParameter("id", id)
                .executeUpdate();
    }

    private void applyPaging(Query query, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 5000);
        query.setFirstResult(safePage * safeSize);
        query.setMaxResults(safeSize);
    }

    private Integer nextId(String table) {
        Number max = (Number) entityManager.createNativeQuery(
                "SELECT COALESCE(MAX(id), 0) + 1 FROM " + table
        ).getSingleResult();
        return max.intValue();
    }
}
