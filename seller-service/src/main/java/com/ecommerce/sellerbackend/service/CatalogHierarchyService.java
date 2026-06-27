package com.ecommerce.sellerbackend.service;



import com.ecommerce.sellerbackend.dto.CatalogCategoryResponse;

import com.ecommerce.sellerbackend.dto.CatalogLeafSubcategoryResponse;

import com.ecommerce.sellerbackend.dto.CatalogMaterialOptionResponse;

import com.ecommerce.sellerbackend.dto.CatalogSubcategoryResponse;

import com.ecommerce.sellerbackend.entity.Category;

import com.ecommerce.sellerbackend.entity.Subcategory;

import com.ecommerce.sellerbackend.repository.CategoryRepository;

import com.ecommerce.sellerbackend.repository.SubcategoryRepository;

import com.ecommerce.sellerbackend.service.support.MaterialSlabParser;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Service;



import java.math.BigDecimal;

import java.sql.ResultSet;

import java.sql.SQLException;

import java.util.ArrayList;

import java.util.Comparator;

import java.util.HashMap;

import java.util.List;

import java.util.Map;

import java.util.Objects;

import java.util.Optional;



/**

 * Product catalog hierarchy:

 * <ul>

 *   <li>Main category — {@code categories} rows where {@code parent_id IS NULL}</li>
 *   <li>Category — child {@code categories} rows where {@code parent_id = main.id},
 *       or {@code subcategories} rows linked by {@code category_id} when no child categories exist</li>
 *   <li>Subcategory — rows in {@code subcategories} under a child category, or leaf rows in
 *       {@code sub_subcategories} / child {@code subcategories}</li>

 * </ul>

 */

@Service

@RequiredArgsConstructor

public class CatalogHierarchyService {



    private final CategoryRepository categoryRepository;

    private final SubcategoryRepository subcategoryRepository;

    private final JdbcTemplate jdbcTemplate;



    private volatile Boolean mainCategoryParentColumnChecked;

    private volatile String mainCategoryParentColumn;



    public List<CatalogCategoryResponse> loadCategoryTree() {

        List<MainCategoryRow> mainCategories = loadMainCategories();

        Map<Integer, List<LeafRow>> leavesByParentSubId = loadLeafRows();

        boolean leafRowsInSubcategoriesTable = usesSubcategorySelfReferenceForLeaves()

                && !tableExists("sub_subcategories");

        String parentColumn = resolveMainCategoryParentColumn();

        List<CatalogCategoryResponse> responses = new ArrayList<>();

        for (MainCategoryRow main : mainCategories) {

            List<CatalogSubcategoryResponse> subResponses = new ArrayList<>();

            if (parentColumn != null) {
                for (ChildCategoryRow child : loadChildCategories(main.id(), parentColumn)) {
                    List<Subcategory> subs = loadSubcategoriesForCategoryId(child.id(), leafRowsInSubcategoriesTable);
                    List<CatalogLeafSubcategoryResponse> children = buildLeafResponses(subs, leavesByParentSubId);
                    subResponses.add(CatalogSubcategoryResponse.builder()
                            .id(child.id())
                            .name(child.name())
                            .children(children)
                            .build());
                }
            }

            if (subResponses.isEmpty()) {
                List<Subcategory> subs = loadMiddleSubcategories(main.id(), leafRowsInSubcategoriesTable);
                for (Subcategory sub : subs) {
                    subResponses.add(buildMiddleSubcategoryResponse(sub, leavesByParentSubId));
                }
            }

            responses.add(CatalogCategoryResponse.builder()
                    .id(main.id())
                    .name(main.name())
                    .subcategories(subResponses)
                    .build());

        }

        return responses;

    }



    public Optional<MainCategoryRow> findMainCategoryById(Integer id) {

        if (id == null) {

            return Optional.empty();

        }

        return loadMainCategories().stream()

                .filter(row -> row.id() == id)

                .findFirst();

    }



    public Optional<MainCategoryRow> findMainCategoryByName(String name) {

        if (name == null || name.isBlank()) {

            return Optional.empty();

        }

        String trimmed = name.trim();

        return loadMainCategories().stream()

                .filter(row -> row.name().equalsIgnoreCase(trimmed))

                .findFirst();

    }



    public Optional<Subcategory> findMiddleSubcategory(Integer mainCategoryId, String subcategoryName) {

        if (mainCategoryId == null || subcategoryName == null || subcategoryName.isBlank()) {

            return Optional.empty();

        }

        boolean leafRowsInSubcategoriesTable = usesSubcategorySelfReferenceForLeaves()

                && !tableExists("sub_subcategories");

        String parentColumn = resolveMainCategoryParentColumn();
        if (parentColumn != null) {
            Optional<ChildCategoryRow> child = loadChildCategories(mainCategoryId, parentColumn).stream()
                    .filter(row -> row.name().equalsIgnoreCase(subcategoryName.trim()))
                    .findFirst();
            if (child.isPresent()) {
                Subcategory pseudo = new Subcategory();
                pseudo.setId(child.get().id());
                pseudo.setCategoryId(mainCategoryId);
                pseudo.setSubcategoryName(child.get().name());
                return Optional.of(pseudo);
            }
        }

        return loadMiddleSubcategories(mainCategoryId, leafRowsInSubcategoriesTable).stream()

                .filter(sub -> sub.getSubcategoryName().equalsIgnoreCase(subcategoryName.trim()))

                .findFirst();

    }

    /** Normal category id under a main category (categories.parent_id = main). */
    public Optional<Integer> findChildCategoryId(Integer mainCategoryId, String childCategoryName) {
        if (mainCategoryId == null || childCategoryName == null || childCategoryName.isBlank()) {
            return Optional.empty();
        }
        String parentColumn = resolveMainCategoryParentColumn();
        if (parentColumn == null) {
            return Optional.empty();
        }
        return loadChildCategories(mainCategoryId, parentColumn).stream()
                .filter(row -> row.name().equalsIgnoreCase(childCategoryName.trim()))
                .map(ChildCategoryRow::id)
                .findFirst();
    }

    public boolean isChildCategoryId(Integer categoryId, Integer mainCategoryId) {
        if (categoryId == null || mainCategoryId == null) {
            return false;
        }
        String parentColumn = resolveMainCategoryParentColumn();
        if (parentColumn == null) {
            return false;
        }
        return findChildCategoryById(categoryId, parentColumn)
                .map(child -> isChildOfMain(child, mainCategoryId, parentColumn))
                .orElse(false);
    }

    /**
     * Resolve {@code subcategories.id} for a normal category row ({@code categories.id} where parent_id is set).
     */
    public Optional<Integer> resolveSubcategoryIdForChildCategory(
            Integer childCategoryId,
            String subcategoryName) {
        if (childCategoryId == null) {
            return Optional.empty();
        }
        List<Subcategory> subs = subcategoryRepository.findByCategoryIdOrderBySubcategoryNameAsc(childCategoryId);
        if (subs.isEmpty()) {
            return Optional.empty();
        }
        if (subcategoryName != null && !subcategoryName.isBlank()) {
            return subs.stream()
                    .filter(sub -> sub.getSubcategoryName().equalsIgnoreCase(subcategoryName.trim()))
                    .map(Subcategory::getId)
                    .findFirst();
        }
        return Optional.of(subs.get(0).getId());
    }



    public record MainCategoryRow(int id, String name) {}

    private record ChildCategoryRow(int id, String name, Integer parentId) {}

    /** Main category → middle category → leaf subcategory labels for product display/filtering. */
    public record ResolvedCategoryPath(String mainCategory, String middleCategory, String leafSubcategory) {}

    public ResolvedCategoryPath resolveCategoryPath(
            Integer mainCategoryId,
            Integer subcategoryId,
            String leafFromSpecifications) {
        return resolveCategoryPath(loadCategoryTree(), mainCategoryId, subcategoryId, leafFromSpecifications);
    }

    public ResolvedCategoryPath resolveCategoryPath(
            List<CatalogCategoryResponse> categoryTree,
            Integer mainCategoryId,
            Integer subcategoryId,
            String leafFromSpecifications) {
        String main = findMainCategoryById(mainCategoryId)
                .map(MainCategoryRow::name)
                .orElseGet(() -> categoryRepository.findById(mainCategoryId)
                        .map(Category::getCategoryName)
                        .orElse("Uncategorized"));
        String leafFromSpecs = leafFromSpecifications != null ? leafFromSpecifications.trim() : "";

        if (subcategoryId == null) {
            return new ResolvedCategoryPath(main, "", leafFromSpecs);
        }

        CatalogCategoryResponse mainNode = categoryTree.stream()
                .filter(node -> Objects.equals(node.getId(), mainCategoryId))
                .findFirst()
                .orElse(null);
        if (mainNode != null) {
            for (CatalogSubcategoryResponse middle : mainNode.getSubcategories()) {
                if (Objects.equals(middle.getId(), subcategoryId)) {
                    return new ResolvedCategoryPath(main, middle.getName(), leafFromSpecs);
                }
                if (middle.getChildren() != null) {
                    for (CatalogLeafSubcategoryResponse leafNode : middle.getChildren()) {
                        if (Objects.equals(leafNode.getId(), subcategoryId)) {
                            String leaf = leafFromSpecs.isBlank() ? leafNode.getName() : leafFromSpecs;
                            return new ResolvedCategoryPath(main, middle.getName(), leaf);
                        }
                    }
                }
            }
        }

        Subcategory subRow = subcategoryRepository.findById(subcategoryId).orElse(null);
        if (subRow != null) {
            String parentColumn = resolveMainCategoryParentColumn();
            if (parentColumn != null) {
                Optional<ChildCategoryRow> child = findChildCategoryById(subRow.getCategoryId(), parentColumn);
                if (child.isPresent() && isChildOfMain(child.get(), mainCategoryId, parentColumn)) {
                    String leaf = leafFromSpecs.isBlank() ? subRow.getSubcategoryName() : leafFromSpecs;
                    return new ResolvedCategoryPath(main, child.get().name(), leaf);
                }
            }
        }

        String middle = subcategoryRepository.findById(subcategoryId)
                .map(Subcategory::getSubcategoryName)
                .orElse("");
        String leaf = leafFromSpecs;
        if (leaf.isBlank()) {
            return new ResolvedCategoryPath(main, middle, "");
        }
        return new ResolvedCategoryPath(main, middle, leaf);
    }

    private record LeafRow(
            int id,
            int parentSubcategoryId,
            String name,
            BigDecimal gstPercentage,
            String materialSlabs) {}

    private CatalogSubcategoryResponse buildMiddleSubcategoryResponse(
            Subcategory sub,
            Map<Integer, List<LeafRow>> leavesByParentSubId) {
        List<CatalogLeafSubcategoryResponse> children = leavesByParentSubId
                .getOrDefault(sub.getId(), List.of())
                .stream()
                .map(row -> buildLeafResponse(
                        row.id(),
                        row.name(),
                        row.gstPercentage(),
                        row.materialSlabs()))
                .sorted(Comparator.comparing(CatalogLeafSubcategoryResponse::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return CatalogSubcategoryResponse.builder()
                .id(sub.getId())
                .name(sub.getSubcategoryName())
                .gstPercentage(sub.getGstPercentage())
                .materials(materialsForSubcategory(sub))
                .children(children)
                .build();
    }

    private List<CatalogMaterialOptionResponse> materialsForSubcategory(Subcategory sub) {
        return MaterialSlabParser.parse(sub.getMaterialSlabs());
    }

    private CatalogLeafSubcategoryResponse buildLeafResponse(
            int id, String name, BigDecimal gstPercentage, String materialSlabs) {
        return CatalogLeafSubcategoryResponse.builder()
                .id(id)
                .name(name)
                .gstPercentage(gstPercentage)
                .materials(MaterialSlabParser.parse(materialSlabs))
                .build();
    }

    private List<CatalogLeafSubcategoryResponse> buildLeafResponses(
            List<Subcategory> subs,
            Map<Integer, List<LeafRow>> leavesByParentSubId) {
        List<CatalogLeafSubcategoryResponse> children = new ArrayList<>();
        for (Subcategory sub : subs) {
            List<LeafRow> leafRows = leavesByParentSubId.getOrDefault(sub.getId(), List.of());
            if (!leafRows.isEmpty()) {
                leafRows.stream()
                        .map(row -> buildLeafResponse(
                                row.id(),
                                row.name(),
                                row.gstPercentage(),
                                row.materialSlabs()))
                        .forEach(children::add);
            } else {
                children.add(buildLeafResponse(
                        sub.getId(),
                        sub.getSubcategoryName(),
                        sub.getGstPercentage(),
                        sub.getMaterialSlabs()));
            }
        }
        return children.stream()
                .sorted(Comparator.comparing(CatalogLeafSubcategoryResponse::getName, String.CASE_INSENSITIVE_ORDER))
                .distinct()
                .toList();
    }

    private List<ChildCategoryRow> loadChildCategories(int mainCategoryId, String parentColumn) {
        String sql = "SELECT id, category_name, " + parentColumn + " FROM categories WHERE "
                + parentColumn + " = ? ORDER BY category_name";
        return jdbcTemplate.query(sql, ps -> ps.setInt(1, mainCategoryId), (rs, rowNum) -> new ChildCategoryRow(
                rs.getInt("id"),
                rs.getString("category_name"),
                rs.getObject(parentColumn) != null ? rs.getInt(parentColumn) : null));
    }

    private Optional<ChildCategoryRow> findChildCategoryById(int categoryId, String parentColumn) {
        String sql = "SELECT id, category_name, " + parentColumn + " FROM categories WHERE id = ?";
        List<ChildCategoryRow> rows = jdbcTemplate.query(sql, ps -> ps.setInt(1, categoryId), (rs, rowNum) ->
                new ChildCategoryRow(
                        rs.getInt("id"),
                        rs.getString("category_name"),
                        rs.getObject(parentColumn) != null ? rs.getInt(parentColumn) : null));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private boolean isChildOfMain(ChildCategoryRow child, Integer mainCategoryId, String parentColumn) {
        return child.parentId() != null && child.parentId().intValue() == mainCategoryId.intValue();
    }

    private List<Subcategory> loadSubcategoriesForCategoryId(int categoryId, boolean leafRowsInSubcategoriesTable) {
        if (leafRowsInSubcategoriesTable) {
            String sql = """
                    SELECT id, category_id, subcategory_name, gst_percentage, material_slabs
                    FROM subcategories
                    WHERE category_id = ? AND parent_subcategory_id IS NULL
                    ORDER BY subcategory_name
                    """;
            return jdbcTemplate.query(sql, ps -> ps.setInt(1, categoryId), this::mapSubcategoryRow);
        }
        return subcategoryRepository.findByCategoryIdOrderBySubcategoryNameAsc(categoryId);
    }



    private List<MainCategoryRow> loadMainCategories() {

        String parentColumn = resolveMainCategoryParentColumn();

        if (parentColumn != null) {

            String sql = "SELECT id, category_name FROM categories WHERE "

                    + parentColumn + " IS NULL ORDER BY category_name";

            return jdbcTemplate.query(sql, (rs, rowNum) -> new MainCategoryRow(

                    rs.getInt("id"),

                    rs.getString("category_name")));

        }



        return categoryRepository.findAll().stream()

                .sorted(Comparator.comparing(Category::getCategoryName, String.CASE_INSENSITIVE_ORDER))

                .map(category -> new MainCategoryRow(category.getId(), category.getCategoryName()))

                .toList();

    }



    private List<Subcategory> loadMiddleSubcategories(int mainCategoryId, boolean leafRowsInSubcategoriesTable) {

        if (leafRowsInSubcategoriesTable) {

            String sql = """

                    SELECT id, category_id, subcategory_name, gst_percentage, material_slabs

                    FROM subcategories

                    WHERE category_id = ? AND parent_subcategory_id IS NULL

                    ORDER BY subcategory_name

                    """;

            return jdbcTemplate.query(sql, ps -> ps.setInt(1, mainCategoryId), this::mapSubcategoryRow);

        }

        return subcategoryRepository.findByCategoryIdOrderBySubcategoryNameAsc(mainCategoryId);

    }



    private Subcategory mapSubcategoryRow(ResultSet rs, int rowNum) throws SQLException {

        Subcategory subcategory = new Subcategory();

        subcategory.setId(rs.getInt("id"));

        subcategory.setCategoryId(rs.getInt("category_id"));

        subcategory.setSubcategoryName(rs.getString("subcategory_name"));

        subcategory.setGstPercentage(rs.getBigDecimal("gst_percentage"));
        try {
            subcategory.setMaterialSlabs(rs.getString("material_slabs"));
        } catch (SQLException ignored) {
            subcategory.setMaterialSlabs(null);
        }

        return subcategory;

    }



    private Map<Integer, List<LeafRow>> loadLeafRows() {

        Map<Integer, List<LeafRow>> grouped = new HashMap<>();



        if (tableExists("sub_subcategories")) {

            String nameCol = columnExists("sub_subcategories", "sub_subcategory_name")

                    ? "sub_subcategory_name"

                    : (columnExists("sub_subcategories", "name") ? "name" : null);

            if (nameCol != null && columnExists("sub_subcategories", "subcategory_id")) {

                String sql = "SELECT id, subcategory_id, " + nameCol + " AS leaf_name FROM sub_subcategories";

                jdbcTemplate.query(sql, (rs, rowNum) -> new LeafRow(

                        rs.getInt("id"),

                        rs.getInt("subcategory_id"),

                        rs.getString("leaf_name"),

                        null,

                        null

                )).forEach(row -> grouped.computeIfAbsent(row.parentSubcategoryId(), k -> new ArrayList<>()).add(row));

                return grouped;

            }

        }



        if (usesSubcategorySelfReferenceForLeaves()) {

            String sql = """

                    SELECT id, parent_subcategory_id, subcategory_name, gst_percentage, material_slabs

                    FROM subcategories

                    WHERE parent_subcategory_id IS NOT NULL

                    """;

            jdbcTemplate.query(sql, (rs, rowNum) -> new LeafRow(

                    rs.getInt("id"),

                    rs.getInt("parent_subcategory_id"),

                    rs.getString("subcategory_name"),

                    rs.getBigDecimal("gst_percentage"),

                    rs.getString("material_slabs")

            )).forEach(row -> grouped.computeIfAbsent(row.parentSubcategoryId(), k -> new ArrayList<>()).add(row));

        }



        return grouped;

    }



    private boolean usesSubcategorySelfReferenceForLeaves() {

        return columnExists("subcategories", "parent_subcategory_id");

    }



    private String resolveMainCategoryParentColumn() {

        if (mainCategoryParentColumnChecked != null) {

            return mainCategoryParentColumn;

        }

        synchronized (this) {

            if (mainCategoryParentColumnChecked == null) {

                if (columnExists("categories", "parent_id")) {

                    mainCategoryParentColumn = "parent_id";

                } else if (columnExists("categories", "parent_category_id")) {

                    mainCategoryParentColumn = "parent_category_id";

                } else {

                    mainCategoryParentColumn = null;

                }

                mainCategoryParentColumnChecked = true;

            }

        }

        return mainCategoryParentColumn;

    }



    private boolean tableExists(String table) {

        try {

            Integer count = jdbcTemplate.queryForObject(

                    """

                    SELECT COUNT(*) FROM information_schema.TABLES

                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?

                    """,

                    Integer.class,

                    table);

            return count != null && count > 0;

        } catch (Exception ignored) {

            return false;

        }

    }



    private boolean columnExists(String table, String column) {

        try {

            Integer count = jdbcTemplate.queryForObject(

                    """

                    SELECT COUNT(*) FROM information_schema.COLUMNS

                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?

                    """,

                    Integer.class,

                    table,

                    column);

            return count != null && count > 0;

        } catch (Exception ignored) {

            return false;

        }

    }

}


