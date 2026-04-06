package com.kalsym.ekedai.controller;

import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.helper.CSVHelper;
import com.kalsym.ekedai.model.*;
import com.kalsym.ekedai.model.enums.ImageType;
import com.kalsym.ekedai.model.enums.PaymentStatus;
import com.kalsym.ekedai.model.enums.VariantType;
import com.kalsym.ekedai.repositories.*;
import com.kalsym.ekedai.services.*;
import com.kalsym.ekedai.utility.HttpResponse;
import com.kalsym.ekedai.utility.Logger;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.kalsym.ekedai.filter.SessionRequestFilter.HEADER_STRING;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    SmsService smsService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ImageAssetService imageAssetService;

    @Autowired
    ProductService productService;

    @Autowired
    ProductVariantService productVariantService;

    @Autowired
    ProductDiscountService productDiscountService;

    @Autowired
    DiscountEventService discountEventService;

    @Autowired
    ProductCategoryService productCategoryService;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    CSVService csvService;

    @Value("${image.assets.location:eByzarr}")
    private String imageAssetPath;

    @Value("${sms.brand:eByzarr}")
    private String smsBrand;

    @Autowired
    ImageAssetsRepository imageAssetsRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductVariantRepository productVariantRepository;

    @Autowired
    BannerService bannerService;

    @Autowired
    BannerRepository bannerRepository;

    @Autowired
    ProductRequiredInfoService productRequiredInfoService;

    @Autowired
    SettlementRepository settlementRepository;

    @Autowired
    SummaryRepository summaryRepository;

    @Autowired
    UserService userService;

    @Autowired
    EmailService emailService;

    @Operation(summary = "Get all users", description = "Only admin can view all the users")
    @GetMapping("/getAllUser")
    public ResponseEntity<HttpResponse> getAllUser(HttpServletRequest request,
            @RequestParam(defaultValue = "created", required = false) String sortBy,
            @RequestParam(defaultValue = "ASC", required = false) String sortingOrder,
            @RequestParam(required = false) List<String> roles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getAllUser";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Requested");

        System.out.println("test");
        
        try {

            User users = new User();
            ExampleMatcher matcher = ExampleMatcher
                    .matchingAll()
                    .withIgnoreCase()
                    .withIgnoreNullValues()
                    .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
            Example<User> example = Example.of(users, matcher);

            Pageable pageable = null;
            if (sortingOrder.equalsIgnoreCase("desc")) {
                pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).descending());
            } else {
                pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).ascending());
            }

            Page<User> userList = userRepository.findAll(getUserSpecs(example, roles), pageable);

            response.setData(userList);
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

    public static Specification<User> getUserSpecs(Example<User> example, List<String> roles) {
        return (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();

            if (roles != null && !roles.isEmpty()) {
                List<String> normalizedRoles = roles.stream().map(String::toUpperCase).collect(Collectors.toList());

                Predicate rolePredicate = root.get("role").in(normalizedRoles);
                predicates.add(rolePredicate);
            }

            predicates.add(QueryByExamplePredicateBuilder.getPredicate(root, builder, example));

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    @Operation(summary = "Add new image", description = "Admin can create save image in file server")
    @PostMapping(path = { "/image-asset" })
    public ResponseEntity<HttpResponse> postAssetImage(
            HttpServletRequest request,
            @RequestParam() ImageType imageType,
            @RequestParam(value = "file", required = false) MultipartFile file)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "postAssetImage";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "filename : " + file.getOriginalFilename());
        try {
            ImageAssets data = imageAssetService.saveImageAsset(file, imageType);
            response.setStatus(HttpStatus.CREATED);
            response.setData(data);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @Operation(summary = "Get assets image list", description = "Only admin can request this endpoint for get all the images.")
    @GetMapping(path = { "/image-asset-list" })
    public ResponseEntity<HttpResponse> getImageList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "fileName") String sortByCol,
            @RequestParam(required = false, defaultValue = "ASC") Sort.Direction sortingOrder) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getImageList";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "imagelist Requested");
        try {
            Page<ImageAssets> body = imageAssetService.getImageAssetsList(page, pageSize, sortByCol, sortingOrder);
            response.setData(body);

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

    @Operation(summary = "Bulk delete image", description = "Note: Please include the id you want to delete, Refer the request body in order to send the request. Only admin can request this endpoint.")
    @PostMapping(path = { "/bulk/delete-image" })
    public ResponseEntity<HttpResponse> bulkDeleteImage(
            HttpServletRequest request,
            @RequestBody List<String> imageId) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "bulkDeleteImage";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "imageId : " + imageId.toString());
        try {
            List<String> data = imageAssetService.bulkDeleteImage(imageId);

            response.setData(data);
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

    @Operation(summary = "Create new product", description = "Admin can create the product.")
    @PostMapping(path = { "/product" })
    public ResponseEntity<HttpResponse> postProduct(
            HttpServletRequest request,
            @RequestBody ProductRequest productRequest)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "postProduct";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Product Request" + productRequest);
        try {

            Product body = Product.castReference(productRequest);
            Product data = productService.createProduct(body);

            // to handle null product variant
            if (productRequest.getProductVariant() != null && !productRequest.getProductVariant().isEmpty()) {
                List<ProductVariant> bodyProductVariant = productRequest.getProductVariant().stream()
                        .map((ProductVariantRequest x) -> {
                            ProductVariant productVariant = ProductVariant.castReference(x);
                            productVariant.setProductId(data.getId());
                            ProductVariant dataProductVariant = productVariantService
                                    .createProductVariant(productVariant);
                            return dataProductVariant;
                        })
                        .collect(Collectors.toList());
                data.setProductVariant(bodyProductVariant);
            }

            // to handle null ProductRequired Info Request
            if (productRequest.getProductRequiredInfo() != null && !productRequest.getProductRequiredInfo().isEmpty()) {
                List<ProductRequiredInfo> bodyProductRequiredInfo = productRequest.getProductRequiredInfo().stream()
                        .map((ProductRequiredInfoRequest y) -> {
                            ProductRequiredInfo productRequiredInfo = ProductRequiredInfo.castReference(y);
                            return productRequiredInfoService
                                    .createProductRequiredInfo(productRequiredInfo);
                        })
                        .collect(Collectors.toList());
                data.setProductRequiredInfo(bodyProductRequiredInfo);
            }

            // to set featured image
            data.setFeaturedImage(data.getImageId() != null
                    ? EkedaiApplication.PROTOCOLSUBDOMAIN + EkedaiApplication.CONTEXTPATH + "/assets/image/"
                            + data.getImageId()
                    : null);

            response.setStatus(HttpStatus.CREATED);
            response.setData(data);
        } catch (DataIntegrityViolationException e) {
            e.printStackTrace();

            if (e.getMessage().contains("productCode")) {

                response.setStatus(HttpStatus.EXPECTATION_FAILED, "Product code already exists.");

            } else {

                response.setStatus(HttpStatus.BAD_REQUEST, e.getMessage());

            }

            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

        }
        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @Operation(summary = "Edit product", description = "Edit product")
    @PutMapping(path = { "/product/{id}" })
    public ResponseEntity<HttpResponse> putProduct(
            HttpServletRequest request,
            @RequestBody ProductRequest productRequest,
            @PathVariable Integer id)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "putProduct";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Product Id" + id);
        try {

            productRequest.setId(id);
            Product body = Product.castReference(productRequest);
            Product data = productService.updateProduct(id, body);

            if (productRequest.getProductVariant() != null) {
                // to handle null product variant
                if (!productRequest.getProductVariant().isEmpty()) {
                    List<ProductVariant> bodyProductVariant = productRequest.getProductVariant().stream()
                            .map((ProductVariantRequest x) -> {
                                ProductVariant productVariant = ProductVariant.castReference(x);
                                productVariant.setProductId(data.getId());
                                ProductVariant dataProductVariant;
                                if (productVariant.getId() != null) {
                                    dataProductVariant = productVariantService
                                            .updateProductVariant(productVariant.getId(), productVariant);
                                } else {
                                    dataProductVariant = productVariantService.createProductVariant(productVariant);
                                }
                                return dataProductVariant;
                            })
                            .collect(Collectors.toList());

                    data.setProductVariant(bodyProductVariant);
                }
            }

            // to handle null product variant
            if (productRequest.getProductRequiredInfo() != null) {
                // delete , then will add it back
                List<ProductRequiredInfo> existingData = productRequiredInfoService
                        .getProductRequiredInfosByProductCode(data.getProductCode());
                if (!existingData.isEmpty()) {
                    for (ProductRequiredInfo pri : existingData) {
                        productRequiredInfoService.deleteProductRequiredInfo(pri.getId());
                    }
                }

                if (productRequest.getProductRequiredInfo() != null
                        && !productRequest.getProductRequiredInfo().isEmpty()) {
                    List<ProductRequiredInfo> bodyProductRequiredInfo = productRequest.getProductRequiredInfo().stream()
                            .map((ProductRequiredInfoRequest y) -> {

                                ProductRequiredInfo productRequiredInfo = ProductRequiredInfo.castReference(y);

                                // add it back
                                return productRequiredInfoService
                                        .createProductRequiredInfo(productRequiredInfo);
                            })
                            .collect(Collectors.toList());
                    data.setProductRequiredInfo(bodyProductRequiredInfo);
                }
            }

            // to set featured image
            data.setFeaturedImage(data.getImageId() != null
                    ? EkedaiApplication.PROTOCOLSUBDOMAIN + EkedaiApplication.CONTEXTPATH + "/assets/image/"
                            + data.getImageId()
                    : null);

            // to set setProductCategoryDetails
            if (data.getCategoryId() != null) {
                Optional<ProductCategory> productCategory = productCategoryService
                        .getProductCategoryId(data.getCategoryId());
                if (productCategory.isPresent())
                    data.setProductCategoryDetails(productCategory.get());
            }

            response.setStatus(HttpStatus.OK);
            response.setData(data);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @Operation(summary = "Create product discount", description = "Note: To create product discount do not send the id of discount event request object ; do not send the id and discountid of product disocunt request object.")
    @PostMapping(path = { "/product-discount" })
    public ResponseEntity<HttpResponse> postProductDiscount(
            HttpServletRequest request,
            @RequestBody DiscountEventRequest discountEventRequest) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "postProductDiscount";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Discount Create Body" + discountEventRequest);
        try {
            DiscountEvent body = DiscountEvent.castReference(discountEventRequest);
            DiscountEvent data = discountEventService.createDiscountEvent(body);

            List<ProductDiscount> bodyProductDiscount = discountEventRequest.getProductDiscount().stream()
                    .map((ProductDiscountRequest x) -> {

                        ProductDiscount productDiscountBody = ProductDiscount.castReference(x);
                        productDiscountBody.setDiscountId(data.getId());
                        ProductDiscount dataProductDiscount = productDiscountService
                                .createProductDiscount(productDiscountBody);
                        return dataProductDiscount;
                    })
                    .collect(Collectors.toList());

            data.setProductDiscount(bodyProductDiscount);
            response.setData(data);
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

    @Operation(summary = "Edit product discount", description = "Edit product discount, please include the id of object DiscountEventRequest and also the id of ProductDiscountRequest")
    @PutMapping(path = { "/product-discount/{discounteventId}" })
    public ResponseEntity<HttpResponse> putProductDiscount(
            HttpServletRequest request,
            @RequestBody DiscountEventRequest discountEventRequest,
            @PathVariable Integer discounteventId)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "putProductDiscount";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Discount Event Id" + discounteventId);
        try {

            discountEventRequest.setId(discounteventId);
            DiscountEvent body = DiscountEvent.castReference(discountEventRequest);
            DiscountEvent data = discountEventService.updateDiscountEvent(discounteventId, body);

            List<ProductDiscount> bodyProductDiscount = discountEventRequest.getProductDiscount().stream()
                    .map((ProductDiscountRequest x) -> {

                        ProductDiscount productDiscountBody = ProductDiscount.castReference(x);
                        productDiscountBody.setDiscountId(data.getId());
                        ProductDiscount dataProductDiscount = productDiscountService
                                .updateProductDiscount(productDiscountBody.getId(), productDiscountBody);

                        return dataProductDiscount;
                    })
                    .collect(Collectors.toList());
            data.setProductDiscount(bodyProductDiscount);

            response.setStatus(HttpStatus.OK);
            response.setData(data);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @Operation(summary = "Get all discount event", description = "The list of discount event.")
    @GetMapping(path = { "/discount-event" })
    public ResponseEntity<HttpResponse> getDiscountEvent(
            HttpServletRequest request,
            @RequestParam(defaultValue = "startDate", required = false) String sortBy,
            @RequestParam(defaultValue = "DESC", required = false) Sort.Direction sortingOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getDiscountEvent";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Requested");
        try {

            Page<DiscountEvent> data = discountEventService.getAllDiscountEvent(page, pageSize, sortBy, sortingOrder);

            response.setStatus(HttpStatus.OK);
            response.setData(data);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @Operation(summary = "Create or edit product category", description = "Please include the id of object Product Category for edit purpose. ")
    @PostMapping(path = { "/product-category" })
    public ResponseEntity<HttpResponse> postProductCategory(
            HttpServletRequest request,
            @RequestBody ProductCategory productCategoryRequest)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "postProductCategory";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Request body : " + productCategoryRequest);
        try {

            ProductCategory data;

            if (productCategoryRequest.getId() != null) {
                data = productCategoryService.updateProductCategory(productCategoryRequest.getId(),
                        productCategoryRequest);
            } else {
                data = productCategoryService.createProductCategory(productCategoryRequest);
            }

            response.setStatus(HttpStatus.OK);
            response.setData(data);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @DeleteMapping(path = { "/product-delete/{id}" })
    public ResponseEntity<HttpResponse> deleteProduct(
            HttpServletRequest request,
            @PathVariable Integer id) {
        String logprefix = "deleteProduct";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Request Delete Product id : " + id);
        HttpResponse response = new HttpResponse(request.getRequestURI());
        HttpStatus httpStatus;

        try {
            Boolean isDeleted = productService.deleteProduct(id);

            httpStatus = isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND;

            response.setStatus(httpStatus);
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(null);
        }

    }

    @DeleteMapping(path = { "/product-variant/{id}" })
    public ResponseEntity<HttpResponse> deleteProductVariant(
            HttpServletRequest request,
            @PathVariable Integer id) {
        String logprefix = "deleteProductVariant";

        HttpResponse response = new HttpResponse(request.getRequestURI());
        HttpStatus httpStatus;
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Request Delete Variant id : " + id);
        try {
            Boolean isDeleted = productVariantService.deleteProductVariant(id);

            httpStatus = isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND;

            response.setStatus(httpStatus);
            return ResponseEntity.status(response.getStatus()).body(response);

        } catch (Exception e) {
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(null);
        }

    }

    @GetMapping(path = { "/transaction/history" })
    public ResponseEntity<HttpResponse> getTransactionHistory(HttpServletRequest request,
            @RequestParam(defaultValue = "ASC", required = false) String sortingOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdDate", required = false) String sortBy,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to,
            @RequestParam(required = false) VariantType variantType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String globalSearch,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyFraud,
            @RequestParam(required = false) String userId

    ) throws Exception {
        String logprefix = "getTransactionHistory";

        HttpResponse response = new HttpResponse(request.getRequestURI());
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Requested");
        try {
            Transaction transaction = new Transaction();

            ExampleMatcher matcher = ExampleMatcher
                    .matchingAll()
                    .withIgnoreCase()
                    .withMatcher("userId", new ExampleMatcher.GenericPropertyMatcher().exact())
                    // .withMatcher("status", new ExampleMatcher.GenericPropertyMatcher().exact())
                    .withIgnoreNullValues()
                    .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
            Example<Transaction> example = Example.of(transaction, matcher);

            Pageable pageable = null;
            if (sortingOrder.equalsIgnoreCase("desc")) {
                pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).descending());
            } else {
                pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).ascending());
            }
            Page<Transaction> transactions = transactionRepository
                    .findAll(
                            getTransactionHistorySpec(from, to, example, variantType, status, paymentStatus,
                                    globalSearch,
                                    paymentMethod, onlyFraud, userId),
                            pageable);
            List<Transaction> tempResultList = transactions.getContent();

            tempResultList = tempResultList.stream()
                    .peek(x -> x.getProduct().setProductVariant(null))
                    .collect(Collectors.toList());
            response.setData(transactions);

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(null);

        }

    }

    public static Specification<Transaction> getTransactionHistorySpec(
            Date from, Date to, Example<Transaction> example, VariantType variantType, String status,
            String paymentStatus, String globalSearch, String paymentMethod, Boolean onlyFraud, String userId) {
        return (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();
            Join<Transaction, ProductVariant> productVariant = root.join("productVariant");
            Join<Transaction, Product> product = root.join("product");

            if (from != null && to != null) {

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(to);

                // Add one day to the current date
                calendar.add(Calendar.DAY_OF_MONTH, 1);

                // Get the updated date
                Date updatedDate = calendar.getTime();
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdDate"), from));
                predicates.add(builder.lessThanOrEqualTo(root.get("createdDate"), updatedDate));
            }

            if (variantType != null) {
                predicates.add(builder.equal(productVariant.get("variantType"), variantType));
            }

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            if (paymentStatus != null) {
                predicates.add(builder.equal(root.get("paymentStatus"), PaymentStatus.valueOf(paymentStatus)));
            }

            if (paymentMethod != null) {
                if (paymentMethod.equalsIgnoreCase("ozo")) {
                    predicates.add(builder.or(
                            builder.equal(root.get("paymentMethod"), "BANK"),
                            builder.equal(root.get("paymentMethod"), "CARD"),
                            builder.equal(root.get("paymentMethod"), "EWALLET")));
                } else
                    predicates.add(builder.equal(root.get("paymentMethod"), paymentMethod));
            }

            if (globalSearch != null) {
                // Predicate for Employee Projects data
                predicates.add(builder.or(
                        builder.like(product.get("productName"), "%" + globalSearch + "%"),
                        builder.like(product.get("productCode"), "%" + globalSearch + "%"),
                        builder.like(root.get("name"), "%" + globalSearch + "%"),
                        builder.like(root.get("phoneNo"), "%" + globalSearch + "%"),
                        builder.like(root.get("id"), "%" + globalSearch + "%"),
                        builder.like(root.get("wspTransactionId"), "%" + globalSearch + "%"),
                        builder.like(root.get("transactionId"), "%" + globalSearch + "%"),
                        builder.like(root.get("paymentTransactionId"), "%" + globalSearch + "%"),
                        builder.like(root.get("accountNo"), "%" + globalSearch + "%")));
            }

            // To show only fraud transactions
            if (onlyFraud) {
                predicates.add(builder.equal(root.get("isFraud"), true));
            }

            if (userId != null) {
                predicates.add(builder.equal(root.get("userId"), userId));
            }

            // Exclude records where both status and paymentStatus are PENDING
            // predicates.add(builder.not(
            // builder.and(
            // builder.equal(root.get("status"), "PENDING"),
            // builder.equal(root.get("paymentStatus"), PaymentStatus.PENDING))));

            predicates.add(QueryByExamplePredicateBuilder.getPredicate(root, builder, example));

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    @PostMapping("/bulk/variant/upload/{id}/{type}")
    public ResponseEntity<HttpResponse> uploadFile(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @PathVariable Integer id,
            @PathVariable String type) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String message = "";
        String logprefix = "uploadFile";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Product id : " + id);

        List<ProductVariant> objectList = new ArrayList<>();
        List<ProductVariant> duplicate = new ArrayList<>();
        List<ProductVariant> successList = new ArrayList<>();

        if (CSVHelper.hasCSVFormat(file)) {
            try {
                objectList = csvService.convertToList(file, type);
                for (ProductVariant variant : objectList) {
                    variant.setProductId(id);
                    Optional<ProductVariant> duplicateData = productVariantRepository
                            .findByWspProductCode(variant.getWspProductCode());
                    if (duplicateData.isPresent()) {
                        duplicate.add(variant); // Add variant to the duplicate list
                    } else {
                        successList.add(variant);
                        productVariantService.createProductVariant(variant);
                    }
                }

                if (duplicate.size() > 0 && successList.size() < 1) {
                    List<String> duplicateNames = new ArrayList<>();
                    for (ProductVariant duplicateVariant : duplicate) {
                        String variantName = duplicateVariant.getVariantName();
                        duplicateNames.add(variantName);
                    }
                    message = "Failed To Upload. \nDuplicate WSP Product Code For Variant : "
                            + duplicateNames.toString();

                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                            "Failed To Upload. \nDuplicate WSP Product Code For Variant : "
                                    + duplicateNames.toString());
                    response.setStatus(HttpStatus.EXPECTATION_FAILED, message);
                } else if (duplicate.size() > 0 && successList.size() > 0) {
                    List<String> duplicateNames = new ArrayList<>();
                    for (ProductVariant duplicateVariant : duplicate) {
                        String variantName = duplicateVariant.getVariantName();
                        duplicateNames.add(variantName);
                    }

                    List<String> succeStrings = new ArrayList<>();
                    for (ProductVariant successVariant : successList) {
                        String variantName = successVariant.getVariantName();
                        succeStrings.add(variantName);
                    }

                    message = "Successful Upload: " + succeStrings.size() + "\nFailed To Upload: " + duplicate.size()
                            + "\nDuplicate WSP Product Code for Variants: " + duplicateNames.toString();

                    response.setStatus(HttpStatus.PARTIAL_CONTENT, message);
                } else if (duplicate.size() < 1) {
                    message = "Uploaded the file successfully: " + file.getOriginalFilename();
                    response.setStatus(HttpStatus.OK, message);
                }

                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "file uploaded");

                response.setMessage(message);

                Optional<Product> data = productRepository.findById(id);
                response.setData(data); // Set the list of product variants as the response data

                return ResponseEntity.status(response.getStatus()).body(response);
            } catch (Exception e) {
                message = "Could not upload the file: " + file.getOriginalFilename() + "! " + e.getMessage();
                response.setMessage(message);
                // e.printStackTrace();
                if (e.getMessage().contains("wspProductCode")) {
                    System.err.println("PRINT HERE ::: " + e.getMessage());
                    response.setStatus(HttpStatus.EXPECTATION_FAILED,
                            "Duplicate WSP Product Code For : " + duplicate.toString());

                } else {
                    response.setStatus(HttpStatus.BAD_REQUEST, message);
                }
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Exception ", e);
                return ResponseEntity.status(response.getStatus()).body(response);
            }

        }

        message = "Please upload CSV file!";
        response.setMessage(message);
        response.setStatus(HttpStatus.BAD_REQUEST, message);

        Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Please upload CSV file!");

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/bulk/variant/uploadToUpdate/{id}/{type}")
    public ResponseEntity<HttpResponse> uploadFileToUpdate(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @PathVariable Integer id,
            @PathVariable String type) {

        String logprefix = "uploadFileToUpdate";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "ProductVariant id : " + id);

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String message = "";
        List<ProductVariant> objectList = new ArrayList<>();
        String variantName = "";
        Optional<ProductVariant> variantData = null;
        if (CSVHelper.hasCSVFormat(file)) {
            try {
                objectList = csvService.convertToListToUpdate(file, type);
                for (ProductVariant variant : objectList) {
                    variant.setProductId(id);
                    if (variant.getId() != null) {

                        variantData = productVariantRepository.findByWspProductCode(variant.getWspProductCode());
                        variantName = variant.getVariantName();
                        productVariantService.updateProductVariant(variant.getId(), variant);
                    } else {

                    }
                }

                message = "Uploaded the file successfully: " + file.getOriginalFilename();
                response.setStatus(HttpStatus.OK, message);
                response.setMessage(message);

                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "file uploaded");

                Optional<Product> data = productRepository.findById(id);
                response.setData(data); // Set the list of product variants as the response data

                return ResponseEntity.status(response.getStatus()).body(response);
            } catch (Exception e) {
                // e.printStackTrace();
                if (e.getMessage().contains("wspProductCode")) {
                    message = "Duplicate WSP Product Code : " + variantData.get().getWspProductCode()
                            + "\nVariant Name: " + variantName
                            + "\nDuplicated With Variant : " + variantData.get().getVariantName();
                    response.setStatus(HttpStatus.EXPECTATION_FAILED, message);
                } else {
                    message = "Could not upload the file: " + file.getOriginalFilename() + "! " + e.getMessage();
                    response.setStatus(HttpStatus.BAD_REQUEST, message);
                }
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "Exception " + e.getMessage());
                response.setMessage(message);
                return ResponseEntity.status(response.getStatus()).body(response);
            }

        }

        message = "Please upload an Excel file!";
        response.setMessage(message);
        response.setStatus(HttpStatus.BAD_REQUEST, message);

        Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Please upload an Excel file!");

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Edit variant", description = "Edit variant")
    @PutMapping(path = { "/productVariant/{id}" })
    public ResponseEntity<HttpResponse> putProductVariant(
            HttpServletRequest request,
            @RequestBody ProductRequest productRequest,
            @PathVariable Integer id)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "putProductVariant";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "ProductVariant id : " + id);
        try {
            if (productRequest.getProductVariant() != null) {
                // to handle null product variant
                if (!productRequest.getProductVariant().isEmpty()) {
                    productRequest.getProductVariant().stream()
                            .map((ProductVariantRequest x) -> {

                                ProductVariant productVariant = ProductVariant.castReference(x);
                                productVariant.setProductId(id);
                                ProductVariant dataProductVariant;
                                if (productVariant.getId() != null) {
                                    dataProductVariant = productVariantService
                                            .updateProductVariant(productVariant.getId(), productVariant);

                                } else {
                                    dataProductVariant = productVariantService.createProductVariant(productVariant);
                                }

                                return dataProductVariant;
                            })
                            .collect(Collectors.toList());
                }
            }

            Optional<Product> data = productRepository.findById(id);

            response.setStatus(HttpStatus.OK);
            response.setData(data);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage().contains("wspProductCode")) {
                response.setStatus(HttpStatus.EXPECTATION_FAILED, "Duplicate WSP Product Code.");
            } else {
                response.setStatus(HttpStatus.BAD_REQUEST, e.getMessage());
            }
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @PostMapping(path = "/makeAsAdmin")
    public ResponseEntity<HttpResponse> makeAsAdmin(
            HttpServletRequest request,
            @RequestBody User userBody) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "makeAsAdmin";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "makeAsAdmin Request : " + userBody);
        try {

            Optional<User> existingUser = userRepository.findById(userBody.getId());
            if (existingUser.isPresent()) {
                User user = existingUser.get();
                user.setRole(userBody.getRole());
                userRepository.save(user);
            }

            response.setData(existingUser);
            response.setStatus(HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.EXPECTATION_FAILED);
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @PostMapping("/postBulkBanners/{section}")
    public ResponseEntity<HttpResponse> createOrUpdateBulkBanners(HttpServletRequest request,
            @PathVariable String section,
            @RequestBody List<Banner> banners) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "postBulkBanners";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Request body " + banners);
        try {
            Date now = new Date();

            // Delete all existing banners by section
            bannerRepository.deleteBySection(section.toLowerCase());

            // Create new banners
            List<Banner> createdBanners = new ArrayList<>();
            for (Banner banner : banners) {
                banner.setName(banner.getName());
                banner.setImageId(banner.getImageId());
                banner.setUrl(banner.getUrl());
                banner.setSection(section.toLowerCase());
                banner.setCreatedAt(now);
                banner.setUpdatedAt(now);
                Banner createdBanner = bannerRepository.save(banner);

                // Fetch associated image details
                ImageAssets imageOptional = imageAssetsRepository.findById(banner.getImageId()).orElse(null);
                createdBanner.setImageDetails(imageOptional);

                createdBanners.add(createdBanner);
            }

            response.setStatus(HttpStatus.OK);
            response.setData(createdBanners);
            response.setMessage("Created banners: " + createdBanners.size());
        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Error creating banners: " + e.getMessage());
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @DeleteMapping("/deleteBulkImages")
    public ResponseEntity<HttpResponse> deleteBulkImages(HttpServletRequest request,
            @RequestBody List<String> imageIds) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "deleteBulkImages";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Request id " + imageIds);
        try {
            List<String> deletedIds = imageAssetService.bulkDeleteImage(imageIds);

            response.setStatus(HttpStatus.OK);
            response.setData(deletedIds);
            response.setMessage("Deleted images: " + deletedIds.size());
        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Error deleting images: " + e.getMessage());
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @DeleteMapping("/deleteImageById/{fileName}")
    public ResponseEntity<HttpResponse> deleteImageById(HttpServletRequest request,
            @PathVariable String fileName) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "deleteImageById";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "fileName " + fileName);
        Optional<ImageAssets> optionalImageAssets = imageAssetsRepository.findByFileName(fileName);

        // Return if not found
        if (!optionalImageAssets.isPresent()) {
            response.setStatus(HttpStatus.NOT_FOUND, "Image id not found");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        try {
            ImageAssets imageAssets = optionalImageAssets.get();

            imageAssetService.deleteImageFile(fileName, imageAssets.getImageType());

            response.setStatus(HttpStatus.OK);
            response.setData(fileName);
            response.setMessage("Deleted");
        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Error deleting images: " + e.getMessage());
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/editTransactionStatus/{id}")
    public ResponseEntity<HttpResponse> editTransactionStatus(HttpServletRequest request, @PathVariable String id) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "editTransactionStatus()";

        try {
            User user = userService.getUser(request.getHeader(HEADER_STRING));
            Optional<Transaction> optionalTransaction = transactionRepository.findByTransactionId(id);

            if (!optionalTransaction.isPresent()) {
                response.setStatus(HttpStatus.NOT_FOUND);
                return ResponseEntity.status(response.getStatus()).body(response);
            }
            Transaction transaction = optionalTransaction.get();

            // Get the current date and time
            Date now = new Date();

            // Create a Calendar instance and set it to the current date and time
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);

            // Set the calendar time to 10:00 PM of the current day
            calendar.set(Calendar.HOUR_OF_DAY, 22);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);

            // Get the cutoff time as a Date object
            Date cutoffTime = calendar.getTime();

            // Assuming transaction.getDate() returns a Date object
            Date transactionDate = transaction.getPaidDate();

            // Check if the transaction date is before 10:00:59.999 PM today and the payment
            // status is neither FAILED nor PENDING
            if (transactionDate.before(cutoffTime) &&
                    !transaction.getPaymentStatus().equals(PaymentStatus.FAILED) &&
                    !transaction.getPaymentStatus().equals(PaymentStatus.PENDING)) {

                transaction.setEditBy(user.getId());
                transaction.setEditUser(user);
                transaction.setPaymentStatus(PaymentStatus.REFUNDED);
                transaction.setStatus("FAILED");

                // sending refund message
                try {
                    String formattedAmount = String.format("%.2f", transaction.getDenoAmount());
                    String message = "RM0 " + smsBrand + ": Your eByzarr transaction RM" + formattedAmount +
                            " has been refunded. Expect the process to complete within 1-3 working days. Ref ID: " +
                            transaction.getTransactionId() + ".";
                    smsService.sendHttpGetRequest(transaction.getPhoneNo(), message, false);
                    transaction.setNotificationSent(true);
                    Logger.application.info("Refund success send message to: " + transaction.getPhoneNo());
                } catch (Exception e) {
                    transaction.setNotificationSent(false);
                    Logger.application.error("Refund failed to send message to: " + transaction.getPhoneNo()
                            + " , Exception: " + e.getMessage());
                }
                transactionRepository.save(transaction);
                response.setStatus(HttpStatus.OK);
                response.setData(transaction);
            } else {
                response.setStatus(HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception:  " + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/getSettlementByBatchDate")
    public ResponseEntity<HttpResponse> getSettlementByBatchDate(HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(name = "batchDate", required = true) String batchDateString) {

        HttpResponse response = new HttpResponse(request.getRequestURI());

        String logprefix = "getSettlementByBatchDate";

        try {
            Pageable pageable = PageRequest.of(page, pageSize);

            pageable = PageRequest.of(page, pageSize, Sort.by("transactionDate").descending());

            LocalDate batchDate = LocalDate.parse(batchDateString); // Convert String to LocalDate
            Page<Settlement> settlementPage = settlementRepository.findByBatchDate(batchDate, pageable);

            response.setStatus(HttpStatus.OK);
            response.setData(settlementPage);

        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Failed to fetch settlements: " + e.getMessage());
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/getSummary")
    public ResponseEntity<HttpResponse> getAllSummary(HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        HttpResponse response = new HttpResponse(request.getRequestURI());

        String logprefix = "getAllSummary";

        try {
            Pageable pageable = PageRequest.of(page, pageSize);

            pageable = PageRequest.of(page, pageSize, Sort.by("date").descending());

            Page<Summary> summaryPage = summaryRepository.findAll(pageable);

            response.setStatus(HttpStatus.OK);
            response.setData(summaryPage);

        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Failed to fetch summarys: " + e.getMessage());
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // create product variant by admin from pending customer rate plan (wsp
    // database)
    @PostMapping(path = {
            "/createOrUpdate/product-variant" }, name = "create-update-product-variant-by-admin", produces = "application/json")
    public ResponseEntity<HttpResponse> createUpdateProductVariantByAdmin(
            HttpServletRequest request,
            @RequestParam String variantName,
            @RequestParam String productCode,
            @RequestParam String productName,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Double price,
            @RequestParam(required = false) VariantType variantType,
            @RequestParam(required = false) VariantType productType,
            @RequestParam String wspProductCode,
            @RequestParam String countryCode,
            @RequestParam(required = false) Double deno) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "create-product-variant";
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                "Create or Update Product Variant");
        try {
            User user = userService.getUser(request.getHeader(HEADER_STRING));
            if (user == null) {
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setMessage("User Not Found");
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                        "User Not Found, User token : " + request.getHeader(HEADER_STRING));

                return ResponseEntity.status(response.getStatus()).body(response);
            }

            // Create product
            Product resultProduct = productService.createOrUpdateProduct(productCode, productName, categoryId,
                    countryCode, productType);

            // Call the service method to create a new product
            ProductVariant createdProductVariant = productVariantService.createUpdateProductVariant(
                    variantName, price, variantType, wspProductCode, resultProduct, deno);

            // to return status and display data at swagger
            response.setStatus(HttpStatus.OK, "Successfully");
            response.setData(createdProductVariant);

        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Bad Request");

            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @GetMapping("/download/products/csv")
    public ResponseEntity<?> downloadProductsCSV() {
        try {
            List<Product> products = productRepository.findAll();
            ByteArrayInputStream csvData = csvService.productsToCSV(products);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=products.csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(new InputStreamResource(csvData));
        } catch (Exception e) {
            // Return an error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error occurred while generating the CSV file: " + e.getMessage());
        }
    }

    @PostMapping("/csv-upload/update-variant-service-id")
    public ResponseEntity<HttpResponse> uploadCsvToUpdateVariantServiceID(HttpServletRequest request,
            @RequestParam("file") MultipartFile file) {

        String logPrefix = "uploadCsvToUpdateVariantServiceID";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        if (file.isEmpty() || !CSVHelper.hasCSVFormat(file)) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Invalid file. Please upload a CSV file.");
            return ResponseEntity.status(response.getStatus()).body(response);
        }

        try (InputStream inputStream = file.getInputStream()) {
            // Call the service to update serviceIds from the uploaded CSV
            List<String> skippedLines = productVariantService.updateServiceIdsFromCSV(inputStream);
            response.setStatus(HttpStatus.OK);
            if (!skippedLines.isEmpty()) {
                response.setData(skippedLines); // Set skipped lines in response
                response.setMessage("Some rows were skipped due to issues");
            } else {
                response.setMessage("All rows processed successfully");
            }
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                    "Exception " + e.getMessage());

            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @PostMapping("/send-fraud-alert")
    public ResponseEntity<String> sendFraudEmail() {
        Optional<Transaction> transaction = transactionRepository.findById("44b86cdc-4ebd-4fe5-896b-715595507ab1");
        if (!transaction.isPresent()) {
            return ResponseEntity.badRequest().body("Transaction not found");
        }

        try {
            emailService.sendFraudAlert(transaction.get());
            return ResponseEntity.ok("Fraud alert email sent successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }

}
