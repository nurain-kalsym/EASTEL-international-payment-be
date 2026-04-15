package com.kalsym.internationalPayment.controller;

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

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.helper.CSVHelper;
import com.kalsym.internationalPayment.model.*;
import com.kalsym.internationalPayment.model.enums.ImageType;
import com.kalsym.internationalPayment.model.enums.PaymentStatus;
import com.kalsym.internationalPayment.model.enums.VariantType;
import com.kalsym.internationalPayment.repositories.*;
import com.kalsym.internationalPayment.services.*;
import com.kalsym.internationalPayment.utility.HttpResponse;
import com.kalsym.internationalPayment.utility.Logger;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;

import static com.kalsym.internationalPayment.filter.SessionRequestFilter.HEADER_STRING;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admins")
public class AdminController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ImageAssetService imageAssetService;

    @Autowired
    ProductService productService;

    @Autowired
    ProductVariantService productVariantService;

    @Autowired
    ProductCategoryService productCategoryService;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    CSVService csvService;

    @Value("${image.assets.location:eastel}")
    private String imageAssetPath;

    @Value("${email.notification}")
    private String emailNotify;

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

    @Autowired
    AdminService adminService;


     /**
     * ------------------------------------------------------------------------------------------------------------------------------------------------
     * User related endpoints
     * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */


    @Operation(summary = "Get all users", description = "To retrieve all user with pagination/filter")
    @GetMapping("/users/pagination")
    public ResponseEntity<HttpResponse> getAllUser(HttpServletRequest request,
            @RequestParam(defaultValue = "created", required = false) String sortBy,
            @RequestParam(defaultValue = "ASC", required = false) String sortingOrder,
            @RequestParam(required = false) List<String> roles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getAllUser";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "Requested");

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

            Page<User> userList = userRepository.findAll(getUserSpecs(roles), pageable);

            response.setData(userList);
            response.setStatus(HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    public static Specification<User> getUserSpecs(List<String> roles) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // role filter
            if (roles != null && !roles.isEmpty()) {
                List<String> normalizedRoles = roles.stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.toList());
                predicates.add(root.get("role").in(normalizedRoles));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }


     /**
     * ------------------------------------------------------------------------------------------------------------------------------------------------
     * Asset related endpoints
     * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    @Operation(summary = "Add new image", description = "To create save image in file server")
    @PostMapping(value = "/assets/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HttpResponse> postAssetImage(
            HttpServletRequest request,
            @RequestParam() ImageType imageType,
            @RequestParam(value = "file", required = false) MultipartFile file)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "postAssetImage";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }
        
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "filename : " + file.getOriginalFilename());
        try {
            ImageAssets data = imageAssetService.saveImageAsset(file, imageType);
            response.setStatus(HttpStatus.CREATED);
            response.setData(data);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @Operation(summary = "Get assets image list", description = "To retrieve all the images with pagination.")
    @GetMapping(path = { "/assets/pagination" })
    public ResponseEntity<HttpResponse> getImageList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "fileName") String sortByCol,
            @RequestParam(required = false, defaultValue = "ASC") Sort.Direction sortingOrder) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getImageList";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "imagelist Requested");
        try {
            Page<ImageAssets> body = imageAssetService.getImageAssetsList(page, pageSize, sortByCol, sortingOrder);
            response.setData(body);

            response.setStatus(HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @Operation(summary = "Bulk delete image", description = "To delete images in bulk with provided image IDs")
    @PostMapping(path = { "/assets/delete/bulk" })
    public ResponseEntity<HttpResponse> bulkDeleteImage(
            HttpServletRequest request,
            @RequestBody List<String> imageId) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "bulkDeleteImage";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "imageId : " + imageId.toString());
        try {
            List<String> data = imageAssetService.bulkDeleteImage(imageId);

            response.setData(data);
            response.setStatus(HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERRROR:::" + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @Operation(summary = "Upload banners in bulk by section", description = "To upload images for banner by section in bulk")
    @PostMapping("/assets/banner/bulk/{section}")
    public ResponseEntity<HttpResponse> createOrUpdateBulkBanners(HttpServletRequest request,
            @PathVariable String section,
            @RequestBody List<Banner> banners) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "postBulkBanners";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
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
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Delete image by filename", description = "To delete image by filename")
    @DeleteMapping("/assets/delete/{fileName}")
    public ResponseEntity<HttpResponse> deleteImageById(HttpServletRequest request,
            @PathVariable String fileName) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "deleteImageById";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
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
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }


    /**
     * ------------------------------------------------------------------------------------------------------------------------------------------------
     * Products related endpoints
     * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    @Operation(summary = "Create new product", description = "To create new product")
    @PostMapping(path = { "/products/create" })
    public ResponseEntity<HttpResponse> postProduct(
            HttpServletRequest request,
            @RequestBody ProductRequest productRequest)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "postProduct";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
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
                    ? InternationalPaymentApplication.PROTOCOLSUBDOMAIN + InternationalPaymentApplication.CONTEXTPATH + "/assets/image/"
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

            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

        }
        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @Operation(summary = "Edit product", description = "To edit product by product ID")
    @PutMapping(path = { "/products/edit/{id}" })
    public ResponseEntity<HttpResponse> putProduct(
            HttpServletRequest request,
            @RequestBody ProductRequest productRequest,
            @PathVariable Integer id)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "putProduct";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
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
                    ? InternationalPaymentApplication.PROTOCOLSUBDOMAIN + InternationalPaymentApplication.CONTEXTPATH + "/assets/image/"
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
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }
    
    @Operation(summary = "Create or edit product category", description = "Please include the id of object Product Category for edit purpose. ")
    @PostMapping(path = { "/products/category" })
    public ResponseEntity<HttpResponse> postProductCategory(
            HttpServletRequest request,
            @RequestBody ProductCategory productCategoryRequest)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "postProductCategory";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
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
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @Operation(summary = "Delete product", description = "To delete product by ID")
    @DeleteMapping(path = { "/products/delete/{id}" })
    public ResponseEntity<HttpResponse> deleteProduct(
            HttpServletRequest request,
            @PathVariable Integer id) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "deleteProduct";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "Request Delete Product id : " + id);
        HttpStatus httpStatus;

        try {
            Boolean isDeleted = productService.deleteProduct(id);

            httpStatus = isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND;

            response.setStatus(httpStatus);
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(null);
        }

    }

    @Operation(summary = "Delete variant", description = "To delete variant by ID")
    @DeleteMapping(path = { "/products/variants/delete/{id}" })
    public ResponseEntity<HttpResponse> deleteProductVariant(
            HttpServletRequest request,
            @PathVariable Integer id) {
        String logprefix = "deleteProductVariant";
        HttpResponse response = new HttpResponse(request.getRequestURI());

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        HttpStatus httpStatus;
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "Request Delete Variant id : " + id);
        try {
            Boolean isDeleted = productVariantService.deleteProductVariant(id);

            httpStatus = isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND;

            response.setStatus(httpStatus);
            return ResponseEntity.status(response.getStatus()).body(response);

        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(null);
        }

    }

    @Operation(summary = "Bulk upload variants", description = "To upload variants in bulk by product ID and type")
    @PostMapping( value = "/products/variants/upload/bulk/{id}/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HttpResponse> uploadFile(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @PathVariable Integer id,
            @PathVariable String type) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String message = "";
        String logprefix = "uploadFile";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
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

                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
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

                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
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
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Exception ", e);
                return ResponseEntity.status(response.getStatus()).body(response);
            }

        }

        message = "Please upload CSV file!";
        response.setMessage(message);
        response.setStatus(HttpStatus.BAD_REQUEST, message);

        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "Please upload CSV file!");

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Bulk update variants", description = "To update variants in bulk by product ID and type")
    @PostMapping( value = "/products/variants/update/bulk/{id}/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HttpResponse> uploadFileToUpdate(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @PathVariable Integer id,
            @PathVariable String type) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "uploadFileToUpdate";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "Product id : " + id);

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
                    }
                }

                message = "Uploaded the file successfully: " + file.getOriginalFilename();
                response.setStatus(HttpStatus.OK, message);
                response.setMessage(message);

                Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
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
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "Exception " + e.getMessage());
                response.setMessage(message);
                return ResponseEntity.status(response.getStatus()).body(response);
            }

        }

        message = "Please upload an Excel file!";
        response.setMessage(message);
        response.setStatus(HttpStatus.BAD_REQUEST, message);

        Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "Please upload an Excel file!");

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Edit variant", description = "To edit variant by product ID")
    @PutMapping(path = { "/products/variants/edit/{id}" })
    public ResponseEntity<HttpResponse> putProductVariant(
            HttpServletRequest request,
            @RequestBody ProductRequest productRequest,
            @PathVariable Integer id)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "putProductVariant";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "product id : " + id);
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
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @Operation(summary = "WSP create/update variant", description = "To create product variant by admin from pending customer rate plan (wsp db)")
    @PostMapping(path = {"/products/variants/wsp" }, produces = "application/json")
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

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "Create or Update Product Variant");
        try {
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

            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @Operation(summary = "Dowload products CSV", description = "To download products in CSV form")
    @GetMapping("/download/products/csv")
    public ResponseEntity<?> downloadProductsCSV( HttpServletRequest request) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "downloadProductsCSV";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

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
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

            // Return an error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error occurred while generating the CSV file: " + e.getMessage());

        }
    }


     /**
     * ------------------------------------------------------------------------------------------------------------------------------------------------
     * Transactions related endpoints
     * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    @Operation(summary = "Get all transactions", description = "To retrieve all transactions with pagination/filter")
    @GetMapping(path = { "/transactions/pagination" })
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
            @RequestParam(required = false) String userId

    ) throws Exception {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getTransactionHistory";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "Requested");
        try {
            Transaction transaction = new Transaction();

            ExampleMatcher matcher = ExampleMatcher
                    .matchingAll()
                    .withIgnoreCase()
                    .withMatcher("userId", new ExampleMatcher.GenericPropertyMatcher().exact())
                    .withIgnoreNullValues()
                    .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
            Example<Transaction> example = Example.of(transaction, matcher);

            Pageable pageable = null;
            if (sortingOrder.equalsIgnoreCase("desc")) {
                pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).descending());
            } else {
                pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).ascending());
            }

            // Check if table is empty
            if (transactionRepository.count() == 0) {
                Page<Transaction> emptyPage = Page.empty(pageable);
                response.setData(emptyPage);
                response.setStatus(HttpStatus.OK);
                return ResponseEntity.status(HttpStatus.OK).body(response);
            }

            Page<Transaction> transactions = transactionRepository
                    .findAll(
                            getTransactionHistorySpec(from, to, example, variantType, status, paymentStatus,
                                    globalSearch,paymentMethod, userId),
                            pageable);
            List<Transaction> tempResultList = transactions.getContent();

            tempResultList = tempResultList.stream()
                    .peek(x -> x.getProduct().setProductVariant(null))
                    .collect(Collectors.toList());
            response.setData(transactions);

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(null);

        }

    }

    public static Specification<Transaction> getTransactionHistorySpec(
            Date from, Date to, Example<Transaction> example, VariantType variantType, String status,
            String paymentStatus, String globalSearch, String paymentMethod, String userId) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // LEFT JOIN instead of INNER JOIN
            Join<Transaction, ProductVariant> productVariant = root.join("productVariant", JoinType.LEFT);
            Join<Transaction, Product> product = root.join("product", JoinType.LEFT);

            if (from != null && to != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(to);
                calendar.add(Calendar.DAY_OF_MONTH, 1);
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

            if (userId != null) {
                predicates.add(builder.equal(root.get("userId"), userId));
            }

            // Add example
            predicates.add(QueryByExamplePredicateBuilder.getPredicate(root, builder, example));

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Operation(summary = "Refund transaction", description = "To refund transation status by transactionId")
    @PutMapping("/transactions/refund/{transactionId}")
    public ResponseEntity<HttpResponse> editTransactionStatus(HttpServletRequest request, @PathVariable String transactionId) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "editTransactionStatus()";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        try {
            User user = userService.getUser(request.getHeader(HEADER_STRING));
            if (user == null) {
                response.setStatus(HttpStatus.NOT_FOUND);
                response.setMessage("User Not Found");
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "User Not Found, User token : " + request.getHeader(HEADER_STRING));

                return ResponseEntity.status(response.getStatus()).body(response);
            }

            Optional<Transaction> optionalTransaction = transactionRepository.findByTransactionId(transactionId);
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

                    if ("true".equalsIgnoreCase(emailNotify)) {
                        emailService.sendRefundEmail(transaction.getName(), transaction.getEmail(), formattedAmount, transaction.getTransactionId());
                        transaction.setNotificationSent(true);
                        Logger.application.info("Refund success send email to: " + transaction.getEmail());
                    } else {
                        Logger.application.info("Refund email notification is OFF");
                    }

                } catch (Exception e) {
                    transaction.setNotificationSent(false);
                    Logger.application.error("Refund failed to send email to: " + transaction.getEmail()
                            + " , Exception: " + e.getMessage());
                }
                transactionRepository.save(transaction);
                response.setStatus(HttpStatus.OK);
                response.setData(transaction);
            } else {
                response.setStatus(HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception:  " + e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

  
     /**
     * ------------------------------------------------------------------------------------------------------------------------------------------------
     * Other endpoints
     * --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
     */

    @Operation(summary = "Assign user as Admin", description = "To assign user role to ADMIN")
    @PostMapping(path = "/assign-as-admin")
    public ResponseEntity<HttpResponse> makeAsAdmin(
            HttpServletRequest request,
            @RequestBody User userBody) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "makeAsAdmin";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
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
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @Operation(summary = "Get settlement by batch date", description = "Fetches paginated settlements filtered by batch date")
    @GetMapping("/settlement-by-batch-date")
    public ResponseEntity<HttpResponse> getSettlementByBatchDate(HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(name = "batchDate", required = true) String batchDateString) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getSettlementByBatchDate";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

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
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Operation(summary = "Get summaries", description = "Fetches paginated summary records sorted by date.")
    @GetMapping("/summary")
    public ResponseEntity<HttpResponse> getAllSummary(HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "getAllSummary";

        // verify admin role
        ResponseEntity<HttpResponse> roleCheck = adminService.filterRole(request, response);
        if (roleCheck != null) {
            return roleCheck;
        }

        try {
            Pageable pageable = PageRequest.of(page, pageSize);

            pageable = PageRequest.of(page, pageSize, Sort.by("date").descending());

            Page<Summary> summaryPage = summaryRepository.findAll(pageable);

            response.setStatus(HttpStatus.OK);
            response.setData(summaryPage);

        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("Failed to fetch summary: " + e.getMessage());
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

}
