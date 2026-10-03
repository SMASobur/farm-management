package tc.impact.farmmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tc.impact.farmmanagement.dto.request.CategoryRequest;
import tc.impact.farmmanagement.dto.response.CategoryResponse;
import tc.impact.farmmanagement.entity.Category;
import tc.impact.farmmanagement.entity.Farm;
import tc.impact.farmmanagement.exception.BusinessException;
import tc.impact.farmmanagement.exception.ResourceNotFoundException;
import tc.impact.farmmanagement.repository.CategoryRepository;
import tc.impact.farmmanagement.repository.ExpenseRepository;
import tc.impact.farmmanagement.repository.SaleRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final SaleRepository saleRepository;
    private final FarmContext farmContext;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Farm farm = farmContext.getCurrentFarm();
        String code = normalizeCode(request.getCode());

        if (categoryRepository.existsByFarmIdAndCodeAndType(farm.getId(), code, request.getType())) {
            throw new BusinessException(
                    "Category with code '" + code + "' already exists for type " + request.getType());
        }

        Category category = new Category();
        category.setFarm(farm);
        category.setCode(code);
        category.setName(request.getName());
        category.setNameBn(request.getNameBn());
        category.setType(request.getType());
        category.setActive(request.getActive() != null ? request.getActive() : true);
        category.setSystemDefault(false);
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 100);

        return toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(Category.CategoryType type, boolean activeOnly) {
        Long farmId = farmContext.getCurrentFarmId();
        List<Category> categories;
        if (type != null && activeOnly) {
            categories = categoryRepository.findByFarmIdAndTypeAndActiveTrueOrderByDisplayOrderAscNameAsc(farmId, type);
        } else if (type != null) {
            categories = categoryRepository.findByFarmIdAndTypeOrderByDisplayOrderAscNameAsc(farmId, type);
        } else {
            categories = categoryRepository.findByFarmId(farmId);
        }
        return categories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Long farmId = farmContext.getCurrentFarmId();
        Category category = findOrThrow(id);
        String code = normalizeCode(request.getCode());

        boolean codeChanged = !category.getCode().equals(code);
        boolean typeChanged = !category.getType().equals(request.getType());

        if ((codeChanged || typeChanged)
                && categoryRepository.existsByFarmIdAndCodeAndType(farmId, code, request.getType())) {
            throw new BusinessException(
                    "Category with code '" + code + "' already exists for type " + request.getType());
        }

        if (category.isSystemDefault() && typeChanged) {
            throw new BusinessException("Cannot change the type of a system category");
        }

        category.setCode(code);
        category.setName(request.getName());
        category.setNameBn(request.getNameBn());
        category.setType(request.getType());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findOrThrow(id);

        if (category.isSystemDefault()) {
            throw new BusinessException(
                    "Cannot delete system category '" + category.getCode() +
                            "'. Deactivate it instead (set active=false).");
        }

        long expenseCount = expenseRepository.countByCategoryId(id);
        long saleCount = saleRepository.countByCategoryId(id);

        if (expenseCount > 0 || saleCount > 0) {
            throw new BusinessException(
                    "Cannot delete category '" + category.getName() +
                            "'. It is referenced by " + expenseCount + " expense(s) and " +
                            saleCount + " sale(s). Deactivate it instead.");
        }

        categoryRepository.deleteById(id);
    }

    private Category findOrThrow(Long id) {
        Long farmId = farmContext.getCurrentFarmId();
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        if (!category.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        return category;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase().replaceAll("\\s+", "_");
    }

    CategoryResponse toResponse(Category c) {
        CategoryResponse response = new CategoryResponse();
        response.setId(c.getId());
        response.setCode(c.getCode());
        response.setName(c.getName());
        response.setNameBn(c.getNameBn());
        response.setType(c.getType());
        response.setActive(c.isActive());
        response.setSystemDefault(c.isSystemDefault());
        response.setDisplayOrder(c.getDisplayOrder());
        response.setCreatedAt(c.getCreatedAt());
        return response;
    }
}