package com.kalsym.ekedai.controller;

import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.model.*;
import com.kalsym.ekedai.model.categoryTree.TreeNode;
import com.kalsym.ekedai.model.dao.MtradePaymentResponse;
import com.kalsym.ekedai.model.dao.ProductServiceIdRequest;
import com.kalsym.ekedai.model.dao.ServiceIdWithProductsResponse;
import com.kalsym.ekedai.model.enums.Status;
import com.kalsym.ekedai.model.enums.VariantType;
import com.kalsym.ekedai.repositories.OfficeListRepository;
import com.kalsym.ekedai.repositories.ProductRepository;
import com.kalsym.ekedai.repositories.ProductVariantRepository;
import com.kalsym.ekedai.repositories.VariantCategoryRepository;
import com.kalsym.ekedai.services.ProductCategoryService;
import com.kalsym.ekedai.services.ProductService;
import com.kalsym.ekedai.services.ProductVariantService;
import com.kalsym.ekedai.services.WSPRequestService;
import com.kalsym.ekedai.utility.HttpResponse;
import com.kalsym.ekedai.utility.Logger;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    ProductService productService;

    @Autowired
    ProductCategoryService productCategoryService;

    @Autowired
    ProductVariantRepository productVariantRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    WSPRequestService wspRequestService;

    @Autowired
    VariantCategoryRepository variantCategoryRepository;

    @Autowired
    OfficeListRepository officeListRepository;

    @Autowired
    ProductVariantService productVariantService;

    @Operation(summary = "Get all products", description = "To retrieve all information related to products")
    @GetMapping("/get-all")
    public ResponseEntity<HttpResponse> getQueryProduct(HttpServletRequest request,
            @RequestParam(defaultValue = "productName", required = false) String sortBy,
            @RequestParam(defaultValue = "ASC", required = false) Sort.Direction sortingOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer parentCategoryId,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String variantCategory,
            @RequestParam(required = false) String searchProduct,
            @RequestParam(required = false) Status variantStatus,
            @RequestParam(required = false) VariantType productType) {
        String logprefix = "getQueryProduct";

        HttpResponse response = new HttpResponse(request.getRequestURI());

        try {
            Page<Product> data;
            if (categoryId != null && categoryId == -2) {
                // get unfinished product sync from wsp
                data = productService.getAllIncompleteProduct(page, pageSize, sortBy, sortingOrder,
                        status, categoryId, parentCategoryId, countryCode, variantCategory, searchProduct,
                        variantStatus, productType);
            } else {
                data = productService.getAllProduct(page, pageSize, sortBy, sortingOrder, status,
                        categoryId, parentCategoryId, countryCode, variantCategory, searchProduct, variantStatus);
            }

            response.setData(data);
            response.setStatus(HttpStatus.OK);

            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Total Product  : " + data.getTotalElements());
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/incomplete-count")
    public ResponseEntity<HttpResponse> getIncompleteCount(HttpServletRequest request) {

        HttpResponse response = new HttpResponse(request.getRequestURI());

        Long countIncomplete = productRepository.countProductsNotPhysicalOrBillPaymentAndNoVariant();
        Long countIncomplete2 = productRepository.countProductsPaymentAndNoRequiredField();

        final Map<String, Object> body = new HashMap<>();
        body.put("product", countIncomplete);
        body.put("bill", countIncomplete2);

        response.setData(body);
        response.setStatus(HttpStatus.OK);

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/categories")
    public ResponseEntity<HttpResponse> getCategories(HttpServletRequest request,
            @RequestParam(required = true) String countryCode) {

        String logprefix = "getCategories";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        if (countryCode == null) {
            response.setError("Country code is required.");
            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Country code is required.");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        // Fetch distinct categories by countryCode and ACTIVE status directly from the
        // database
        List<String> categories = productRepository.findDistinctCategoriesByCountryCodeAndStatus(countryCode,
                Status.ACTIVE);

        // Fetch category details from VariantCategoryRepository
        List<VariantCategory> list = categories.stream()
                .map(category -> variantCategoryRepository.findById(category).orElse(null))
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(VariantCategory::getPriority))
                .collect(Collectors.toList());

        response.setData(list);
        response.setStatus(HttpStatus.OK);

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Get all product category", description = "To get the product category. If you want to get the subcategory pass query param parent category id. If you want to get a list of parent categories, just send a null value for the query param parentCategoryId")
    @GetMapping("/category")
    public ResponseEntity<HttpResponse> getProductCategory(HttpServletRequest request,
            @RequestParam(required = false) Integer parentCategoryId,
            @RequestParam(required = false) String countryCode) {

        String logprefix = "getProductCategory";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        try {
            List<ProductCategory> categories;

            if (countryCode != null) {
                // Get filtered categories by country code and parent category (if provided)
                categories = productRepository.findActiveCategoriesByCountryCodeAndParentCategory(Status.ACTIVE,
                        countryCode, parentCategoryId);
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Filtering by country code: " + countryCode);
            } else {
                // Get categories without filtering
                categories = productCategoryService.getProductCategory(parentCategoryId);
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "No country code filtering applied.");
            }

            response.setData(categories);
            response.setStatus(HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setError("Error: " + e.getMessage());

            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception: " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Get specific category id", description = "To get the product category id")
    @GetMapping("/category/{id}")
    public ResponseEntity<HttpResponse> getProductCategoryId(HttpServletRequest request,
            @PathVariable(required = false) Integer id) {
        String logprefix = "getProductCategoryId";

        HttpResponse response = new HttpResponse(request.getRequestURI());

        try {

            Optional<ProductCategory> data = productCategoryService.getProductCategoryId(id);

            if (data.isPresent()) {
                response.setData(data.get());
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Category Id : " + id);
            } else {
                response.setData("NOTFOUND");
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Id Not Found : " + id);
            }

            response.setStatus(HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);

            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Get regioun country", description = "To retrieve all information related to regioun country")
    @GetMapping("/country")
    public ResponseEntity<HttpResponse> getRegiounCountry(HttpServletRequest request,
            @RequestParam(required = false) Integer categoryId) {
        String logprefix = "getRegiounCountry";

        HttpResponse response = new HttpResponse(request.getRequestURI());

        try {

            List<Country> data = productService.getCountry(categoryId);
            response.setData(data);
            response.setStatus(HttpStatus.OK);

            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Category Id : " + categoryId);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);

            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Get Phone Operator", description = "To get product list by the phoneNo")
    @GetMapping("/getOperator/{mobileNo}/{countryCode}")
    public ResponseEntity<HttpResponse> getOperator(HttpServletRequest request,
            @PathVariable(name = "mobileNo") String phone,
            @PathVariable(name = "countryCode") String countryCode,
            @RequestParam(required = true) Integer categoryId) {
        String logprefix = "getOperator";

        HttpResponse response = new HttpResponse(request.getRequestURI());

        MtradePaymentResponse queryOperator = wspRequestService.requestGetOperator(phone, countryCode);
        if (queryOperator != null) {
            System.out
                    .print("queryOperator.getProductOwnerName()::::" + queryOperator.getProductOwnerName() + ":::que");
            Optional<Product> product = productRepository
                    .findOne(getProductByProductNameAndCategoryId(queryOperator.getProductOwnerName(), categoryId));
            if (product.isPresent()) {
                Product tempResult = product.get();

                List<ProductVariant> filteredList = tempResult.getProductVariant().stream()
                        .filter(y -> y.getStatus().equals(Status.ACTIVE))
                        .collect(Collectors.toList());

                tempResult.setProductVariant(filteredList);

                response.setData(tempResult);
                response.setStatus(HttpStatus.OK);

                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "FOUND : " + "phoneNom: " + phone + "countryCode: " + countryCode);
            } else {
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "NOT FOUND : " + "phoneNom: " + phone + "countryCode: " + countryCode);
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setMessage("Please Verify With Merchant");
            }
        } else {
            response.setStatus(HttpStatus.NOT_FOUND);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "NOT FOUND : " + "phoneNom: " + phone + "countryCode: " + countryCode);
        }
        return ResponseEntity.status(response.getStatus()).body(response);

    }

    public Specification<Product> getProductByProductNameAndCategoryId(String productName, Integer categoryId) {
        return (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();

            predicates.add(builder.like(root.get("productName"), productName));
            predicates.add(builder.equal(root.get("categoryId"), categoryId));

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @GetMapping("/productType")
    public ResponseEntity<List<VariantType>> getProductType() {

        List<VariantType> variantTypes = Arrays.asList(VariantType.values());

        return ResponseEntity.ok(variantTypes);
    }

    @GetMapping("/getProductById/{id}")
    public ResponseEntity<HttpResponse> getProductById(HttpServletRequest request, @PathVariable Integer id) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getProductById";

        try {
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "product id : " + id);
            Optional<Product> product = productRepository.findById(id);

            if (product.isPresent()) {
                response.setData(product);
                response.setStatus(HttpStatus.OK);
            } else {
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setMessage("Product Not Found");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            response.setStatus(HttpStatus.EXPECTATION_FAILED);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + ex.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Get all offices list", description = "To retrieve all office list based on productId")
    @GetMapping("/get-office-list")
    public ResponseEntity<HttpResponse> getOfficeListByProductId(HttpServletRequest request,
            @RequestParam(required = true) Integer productId) {
        String logprefix = "getOfficeListByProductId";

        HttpResponse response = new HttpResponse(request.getRequestURI());

        try {
            List<OfficeList> processingOfficeLists = officeListRepository.findAllByProductId(productId);
            response.setData(processingOfficeLists);
            response.setStatus(HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/get-all-by-parent-category")
    public ResponseEntity<HttpResponse> getAllProductsByParentCategoryId(HttpServletRequest request,
            @RequestParam() Integer parentCategoryId) {
        String logprefix = "getAllProductsByParentId";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        try {
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "parent category id : " + parentCategoryId);
            List<Product> data = productService.getProductsByParent(parentCategoryId);
            response.setData(data);
            response.setStatus(HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/categories-with-products-variants")
    public ResponseEntity<?> getAllCategoriesWithProductsAndVariants(HttpServletRequest request,
            @RequestParam(defaultValue = "default", required = false) String level) {
        String logPrefix = "getAllCategoriesWithProductsAndVariants";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        try {
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix, "Fetching category tree");
            List<TreeNode> categories = productService.buildCategoryTreeNode(level);
            response.setData(categories);
            response.setStatus(HttpStatus.OK);

        } catch (Exception e) {
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                    "Exception occurred: " + e.getMessage(), e);

            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("error", "An error occurred while fetching categories with products and variants");
            errorDetails.put("detail", e.getMessage());
            response.setData(errorDetails);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/update-service-id/batch")
    public ResponseEntity<HttpResponse> updateProductServiceIdInBatch(HttpServletRequest request,
            @RequestBody List<ProductServiceIdRequest> requestBody) {
        String logPrefix = "updateProductServiceIdInBatch";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        try {
            productService.updateServiceIdInBatch(requestBody);
            response.setStatus(HttpStatus.OK);
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                    "Exception occurred: " + e.getMessage(), e);

            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("error", "An error occurred while updating product service IDs");
            errorDetails.put("detail", e.getMessage());
            response.setData(errorDetails);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/service-ids-with-products")
    public ResponseEntity<HttpResponse> getServiceIdsWithProducts(HttpServletRequest request) {
        HttpResponse response = new HttpResponse(request.getRequestURI());

        List<ServiceIdWithProductsResponse> data = productService.getServiceIdsWithProducts();

        response.setData(data);
        response.setStatus(HttpStatus.OK);

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Get specific variant by id", description = "To get the product variant by id")
    @GetMapping("/product-by-variant-id/{variantId}")
    public ResponseEntity<HttpResponse> getProductByVariantId(HttpServletRequest request,
            @PathVariable(required = false) Integer variantId) {
        String logprefix = "getProductVariantById";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        Optional<ProductVariant> productVariantOptional = productVariantService.getProductVariantById(variantId);

        if (productVariantOptional.isPresent()) {
            ProductVariant productVariant = productVariantOptional.get();
            Optional<Product> optionalProduct = productRepository.findById(productVariant.getProductId());

            if (optionalProduct.isPresent()) {
                Product product = optionalProduct.get();

                // Set null to exclude from response
                // product.setProductVariant(null);
                product.setPurchaseDescription(null);
                product.setTnc(null);
                product.setDescription(null);

                response.setData(product);
                response.setStatus(HttpStatus.OK);
            } else {
                response.setStatus(HttpStatus.NOT_FOUND);
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Product Not Found for Variant ID: " + variantId);
            }

        } else {
            response.setStatus(HttpStatus.NOT_FOUND);
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Variant Not Found for ID: " + variantId);
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // @Operation(summary = "Get fixFee by wspProductCode", description = "To get fixFee for a productVariant using wspProductCode")
    // @GetMapping("fix-fee/{wspProductCode}")
    // public ResponseEntity<HttpResponse> getFixFeeByEspProductCode(HttpServletRequest request,
    //         @PathVariable String wspProductCode) {
        
    //     HttpResponse response = new HttpResponse(request.getRequestURI());
    //     String logPrefix = "getFixFeeByEspProductCode";

    //     try {
    //         Optional<ProductVariant> optionalVariant = productVariantRepository.findByWspProductCode(wspProductCode);

    //         if (optionalVariant.isPresent()){
    //             Double fixFee = optionalVariant.get().getFixFee();
    //             Map<String, Object> body = new HashMap<>();
    //             body.put("wspProductCode", wspProductCode);
    //             body.put("fixFee", fixFee);

    //             response.setData(body);
    //             response.setStatus(HttpStatus.OK);
    //         } else {
    //             response.setStatus(HttpStatus.NOT_FOUND);
    //             Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
    //                 "NOT FOUND : wspProductCode=" + wspProductCode);
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         response.setStatus(HttpStatus.BAD_REQUEST);
    //         Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
    //             "ERROR : wspProductCode=" + wspProductCode + ", message=" + e.getMessage());
    //     }

    //     return ResponseEntity.status(response.getStatus()).body(response);
    // }
}
