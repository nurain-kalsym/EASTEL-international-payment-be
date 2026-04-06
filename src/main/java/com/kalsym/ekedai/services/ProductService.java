package com.kalsym.ekedai.services;

import com.kalsym.ekedai.model.Country;
import com.kalsym.ekedai.model.Product;
import com.kalsym.ekedai.model.ProductCategory;
import com.kalsym.ekedai.model.ProductVariant;
import com.kalsym.ekedai.model.categoryTree.TreeNode;
import com.kalsym.ekedai.model.dao.ProductDto;
import com.kalsym.ekedai.model.dao.ProductServiceIdRequest;
import com.kalsym.ekedai.model.dao.ServiceIdWithProductsResponse;
import com.kalsym.ekedai.model.enums.Status;
import com.kalsym.ekedai.model.enums.VariantType;
import com.kalsym.ekedai.repositories.CountryRepository;
import com.kalsym.ekedai.repositories.ProductCategoryRepository;
import com.kalsym.ekedai.repositories.ProductDiscountRepository;
import com.kalsym.ekedai.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductDiscountRepository productDiscountRepository;

    @Autowired
    ProductVariantService productVariantService;

    @Autowired
    CountryRepository countryRepository;

    @Autowired
    ProductCategoryRepository productCategoryRepository;

    public Page<Product> getAllIncompleteProduct(
            int page, int pageSize, String sortBy, Sort.Direction sortingOrder,
            Status status, Integer categoryId, Integer parentCategoryId, String countryCode,
            String variantCategory, String searchProduct, Status variantStatus, VariantType productType) {

        Pageable pageable;
        if (sortingOrder == Sort.Direction.ASC) {
            pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).ascending());
        } else {
            pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).descending());
        }
        // Create Specification to filter products with null categoryId or variantType
        Specification<Product> productSpecs = (root, query, builder) -> {

            // Ensure distinct results
            query.distinct(true);
            Join<Product, ProductVariant> productVariantJoin = root.join("productVariant", JoinType.LEFT);

            Predicate categoryIdIsNull = builder.isNull(root.get("categoryId"));
            Predicate variantTypeIsNull = builder.isNull(productVariantJoin.get("variantType"));
            Predicate orPredicate = builder.or(categoryIdIsNull, variantTypeIsNull);

            // Add an additional condition for parentCategoryId == 1 or
            // productType billpayment only
            Predicate finalPredicate = orPredicate;
            if (productType != null && VariantType.BILLPAYMENT.equals(productType)) {
                Join<Product, ProductVariant> productRequiredInfoJoin = root.join("productRequiredInfo", JoinType.LEFT);

                Predicate requiredInfoIdIsNull = builder.isNull(productRequiredInfoJoin.get("fieldValue"));
                Predicate productBill = builder.equal(root.get("productType"), productType);

                finalPredicate = builder.or(orPredicate, requiredInfoIdIsNull);
                finalPredicate = builder.and(finalPredicate, productBill);
            } else {
                Predicate productTypeIsNull = builder.isNull(root.get("productType"));
                Predicate productTypeIsNotBill = builder.notEqual(root.get("productType"), VariantType.BILLPAYMENT);

                Predicate billPredicate = builder.or(productTypeIsNull, productTypeIsNotBill);

                finalPredicate = builder.and(finalPredicate, billPredicate);
            }

            // Search by product details if searchProduct is provided
            if (searchProduct != null) {
                Predicate searchPredicates = builder.or(
                        builder.like(root.get("productCode"), "%" + searchProduct + "%"),
                        builder.like(productVariantJoin.get("variantName"), "%" + searchProduct + "%"),
                        builder.like(root.get("productName"), "%" + searchProduct + "%"));

                finalPredicate = builder.and(finalPredicate, searchPredicates);
            }

            return finalPredicate;
        };

        Page<Product> result = productRepository.findAll(productSpecs, pageable);
        List<Product> tempResultList = result.getContent();

        tempResultList.forEach(product -> product
                .setFeaturedImage(product.getImageDetails() != null ? product.getImageDetails().getImageUrl() : null));

        return result;
    }

    public Page<Product> getAllProduct(
            int page, int pageSize, String sortBy, Sort.Direction sortingOrder,
            Status status, Integer categoryId, Integer parentCategoryId, String countryCode,
            String variantCategory, String searchProduct, Status variantStatus) {

        Pageable pageable = PageRequest.of(page, pageSize);

        if (sortingOrder == Sort.Direction.ASC)
            pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).ascending());
        else if (sortingOrder == Sort.Direction.DESC)
            pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).descending());

        Specification<Product> productSpecs = searchCriteriaProduct(
                status, categoryId, parentCategoryId, countryCode,
                variantCategory, searchProduct, variantStatus);

        Page<Product> result = productRepository.findAll(productSpecs, pageable);
        List<Product> tempResultList = result.getContent();

        tempResultList.stream()
                .map((Product x) -> {
                    x.setFeaturedImage(x.getImageDetails() != null ? x.getImageDetails().getImageUrl() : null);

                    List<ProductVariant> setProductVariantDiscount = x.getProductVariant().stream()
                            // real filter of country code
                            .filter(y -> variantStatus == null || y.getStatus().equals(variantStatus))
                            .filter(y -> variantCategory == null || y.getCategory().equals(variantCategory))

                            .map((ProductVariant y) -> {

                                List<Object[]> callProcedureProductDiscount = productDiscountRepository
                                        .getProductDiscount(y.getId());//
                                Object[] accessValue = callProcedureProductDiscount.get(0);

                                // SELECT discountName, startDate, endDate, calculationType, discountAmount

                                Double discountedPrice = null;
                                String discountName = String.valueOf(accessValue[0]);
                                // procedure that return 0 is not in period of discount
                                if (!discountName.equals("0")) {
                                    if (String.valueOf(accessValue[3]).equals("FIX")) {
                                        discountedPrice = y.getPrice()
                                                - Double.parseDouble(String.valueOf(accessValue[4]));

                                    } else if (String.valueOf(accessValue[3]).equals("PERCENT")) {
                                        discountedPrice = y.getPrice()
                                                - (Double.parseDouble(String.valueOf(accessValue[4])) / 100
                                                        * y.getPrice());

                                    }

                                }

                                y.setDiscountedPrice(discountedPrice);
                                return y;
                            }).collect(Collectors.toList());

                    x.setProductVariant(setProductVariantDiscount);

                    return x;
                })
                .collect(Collectors.toList());

        return result;
    }

    public static Specification<Product> searchCriteriaProduct(
            Status status,
            Integer categoryId,
            Integer parentCategoryId,
            String countryCode,
            String variantCategory,
            String searchProduct,
            Status variantStatus) {

        return (Specification<Product>) (root, query, builder) -> {
            // final List<Predicate> predicates = new ArrayList<>();
            // Join<Product, tbl> dtl = root.join("tbl");
            final List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            if (countryCode != null) {
                predicates.add(builder.equal(root.get("countryCode"), countryCode));
            }

            if (categoryId != null) {
                // this only for all product
                if (categoryId == -1) {
                    predicates.add(builder.or(
                            builder.not(root.get("productType").in(VariantType.BILLPAYMENT, VariantType.PHYSICAL)),
                            builder.isNull(root.get("productType"))));
                } else
                    predicates.add(builder.equal(root.get("categoryId"), categoryId));
            }

            if (parentCategoryId != null) {
                Join<Product, ProductCategory> productCategoryDetails = root.join("productCategoryDetails");
                predicates.add(builder.equal(productCategoryDetails.get("parentCategoryId"), parentCategoryId));
            }

            if (variantCategory != null) {
                Join<Product, ProductVariant> productVariant = root.join("productVariant");
                predicates.add(builder.equal(productVariant.get("category"), variantCategory));
            }

            if (variantStatus != null) {
                Join<Product, ProductVariant> productVariant = root.join("productVariant");
                predicates.add(builder.equal(productVariant.get("status"), variantStatus));
            }

            if (searchProduct != null) {
                Join<Product, ProductVariant> productVariant = root.join("productVariant");
                predicates.add(builder.or(
                        builder.like(root.get("productCode"), "%" + searchProduct + "%"),
                        builder.like(productVariant.get("variantName"), "%" + searchProduct + "%"),
                        builder.like(root.get("productName"), "%" + searchProduct + "%")));
            }

            query.distinct(true);

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));

        };

    }

    public Product createProduct(Product productBody) {

        return productRepository.save(productBody);
    }

    public Product updateProduct(Integer id, Product productBody) {

        Product data = productRepository.findById(id).get();
        data.updateData(productBody);

        return productRepository.save(data);
    }

    public Optional<Product> getProductById(Integer id) {

        return productRepository.findById(id);
    }

    public Boolean deleteProduct(Integer id) {
        Optional<Product> product = productRepository.findById(id);

        if (product.isPresent()) {

            if (product.get().getProductVariant().size() > 0) {

                for (ProductVariant pv : product.get().getProductVariant()) {
                    productVariantService.deleteProductVariant(pv.getId());

                }
            }
            productRepository.deleteById(id);
            return true;

        } else {
            return false;
        }
    }

    public List<Country> getCountry(Integer categoryId) {

        List<Country> data;

        if (categoryId != null) {

            List<Object[]> result = productRepository.distinctQueryCountry(categoryId);

            data = result.stream()
                    .map((Object[] x) -> {

                        Country country = new Country();
                        Optional<Country> optCountry = countryRepository.findById(x[0].toString());
                        if (optCountry.isPresent()) {
                            country = optCountry.get();
                        }

                        return country;

                    })
                    .filter((Country ct) -> ct.getCountryCode() != null)
                    .collect(Collectors.toList());

        } else {

            data = countryRepository.findAll();

        }

        return data;
    }

    public Optional<Product> getProductCode(String productCode) {

        return productRepository.findByProductCode(productCode);
    }

    public Product createOrUpdateProduct(
            String productCode,
            String productName,
            Integer categoryId,
            String countryCode,
            VariantType productType) {
        Optional<Product> existingProduct = productRepository.findByProductCode(productCode);
        Optional<Country> country = countryRepository.findByWspCountryCode(countryCode);

        if (existingProduct.isPresent()) {
            // Product with the given code already exists, update it
            Product updatedProduct = existingProduct.get();
            updatedProduct.setProductName(productName);

            return productRepository.save(updatedProduct);
        } else {
            // Product with the given code doesn't exist, create a new one
            Product newProduct = new Product();
            newProduct.setProductCode(productCode);
            newProduct.setProductName(productName);
            newProduct.setCategoryId(categoryId);
            newProduct.setStatus(Status.INACTIVE);
            newProduct.setProductType(productType);
            newProduct.setCountryCode(country.get().getCountryCode());

            return productRepository.save(newProduct);
        }
    }

    public List<Product> getProductsByParent(Integer parentCategoryId) {
        return productRepository.findAll(searchProductsByParentCategorySpec(Status.ACTIVE, parentCategoryId));
    }

    public static Specification<Product> searchProductsByParentCategorySpec(
            Status status,
            Integer parentCategoryId) {

        return (Specification<Product>) (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            if (parentCategoryId != null) {
                // Check if the product directly belongs to the parent category
                Predicate directParentPredicate = builder.equal(root.get("categoryId"), parentCategoryId);

                // Check if the product belongs to the parent category through the
                // ProductCategoryDetails
                Join<Product, ProductCategory> productCategoryDetails = root.join("productCategoryDetails",
                        JoinType.LEFT);
                Predicate indirectParentPredicate = builder.equal(productCategoryDetails.get("parentCategoryId"),
                        parentCategoryId);

                // Combine the direct and indirect parent predicates using an OR condition
                predicates.add(builder.or(directParentPredicate, indirectParentPredicate));
            }

            query.distinct(true);

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<TreeNode> buildCategoryTreeNode(String level) {

        List<Integer> excludedCategoryIds = Arrays.asList(6, 8, 17);
        List<ProductCategory> categories = productCategoryRepository.findCategoriesExcluding(excludedCategoryIds);

        // Transform categories to TreeNode structure
        return categories.stream().map(category -> {
            // Use category ID and name to fill TreeNode fields
            TreeNode categoryNode = new TreeNode();
            categoryNode.setKey("C-" + category.getId()); // Unique key, prefix with C for Category
            categoryNode.setLabel(category.getCategory());
            categoryNode.setParent(null);

            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("id", category.getId());
            categoryData.put("name", category.getCategory());
            categoryNode.setData(categoryData);

            if (!"category".equalsIgnoreCase(level)) {
                // For each category, find and transform its products to TreeNode
                List<TreeNode> productNodes = productRepository
                        .findAll(searchProductsByParentCategorySpec(Status.ACTIVE, category.getId()))
                        .stream()
                        .map(product -> {
                            TreeNode productNode = new TreeNode();
                            productNode.setKey("P-" + product.getId()); // Unique key, prefix with P for Product
                            productNode.setLabel(product.getProductName());
                            productNode.setParent(new TreeNode("C-" + category.getId(), category.getCategory()));

                            Map<String, Object> productData = new HashMap<>();
                            productData.put("id", product.getId());
                            productData.put("name", product.getProductName());
                            productNode.setData(productData);

                            if (!"product".equalsIgnoreCase(level)) {
                                // Transform product variants to TreeNode children
                                List<TreeNode> variantNodes = product.getProductVariant().stream()
                                        .map(variant -> {
                                            TreeNode variantNode = new TreeNode();
                                            variantNode.setKey("V-" + variant.getId()); // Unique key, prefix with V for
                                                                                        // Variant
                                            variantNode.setLabel(variant.getVariantName());
                                            variantNode.setParent(
                                                    new TreeNode("P-" + product.getId(), product.getProductName()));

                                            Map<String, Object> variantData = new HashMap<>();
                                            variantData.put("id", variant.getId());
                                            variantData.put("name", variant.getVariantName());
                                            variantNode.setData(variantData);
                                            return variantNode;
                                        })
                                        .collect(Collectors.toList());

                                productNode.setChildren(variantNodes);
                            }

                            return productNode;
                        })
                        .collect(Collectors.toList());

                categoryNode.setChildren(productNodes);
            }

            return categoryNode;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void updateServiceIdInBatch(List<ProductServiceIdRequest> requestBody) {
        Set<Integer> requestProductIds = requestBody.stream()
                .map(ProductServiceIdRequest::getProductId)
                .collect(Collectors.toSet());

        List<Product> productsWithServiceId = productRepository.findByServiceIdIsNotNull();

        // Set serviceId to null for products not in the request list
        productsWithServiceId.stream()
                .filter(product -> !requestProductIds.contains(product.getId()))
                .forEach(product -> {
                    product.setServiceId(null);
                    productRepository.save(product);
                });

        // Update serviceId for products in the request list
        for (ProductServiceIdRequest request : requestBody) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + request.getProductId()));
            product.setServiceId(request.getServiceId());
            productRepository.save(product);
        }
    }

    public List<ServiceIdWithProductsResponse> getServiceIdsWithProducts() {
        List<Object[]> results = productRepository.findServiceIdsWithProducts();
        Map<String, List<ProductDto>> serviceIdMap = new HashMap<>();

        for (Object[] result : results) {
            String serviceId = (String) result[0];
            Integer productId = (Integer) result[1];
            String productName = (String) result[2];

            ProductDto productDto = new ProductDto();
            productDto.setId(productId);
            productDto.setName(productName);

            serviceIdMap.computeIfAbsent(serviceId, k -> new ArrayList<>()).add(productDto);
        }

        return serviceIdMap.entrySet().stream()
                .map(entry -> {
                    ServiceIdWithProductsResponse response = new ServiceIdWithProductsResponse();
                    response.setServiceId(entry.getKey());
                    response.setProducts(entry.getValue());
                    return response;
                })
                .collect(Collectors.toList());
    }
}
