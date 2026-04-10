package com.kalsym.internationalPayment.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.ImageAssets;
import com.kalsym.internationalPayment.model.enums.ImageType;
import com.kalsym.internationalPayment.repositories.ImageAssetsRepository;
import com.kalsym.internationalPayment.services.ImageAssetService;
import com.kalsym.internationalPayment.utility.HttpResponse;
import com.kalsym.internationalPayment.utility.Logger;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@RestController
@RequestMapping("/assets")
public class ImageAssetsController {

    @Value("${image.assets.location}")
    private String imageAssetPath;

    @Autowired
    ImageAssetsRepository imageAssetsRepository;

    @Autowired
    ImageAssetService imageAssetService;

    
    @Operation(summary = "Get image by ID", description = "To retrieve image by ID")
    @GetMapping(path = { "/image/{id}" })
    public ResponseEntity<?> getImageById(HttpServletRequest request, @PathVariable String id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        String logprefix = "getImageById";
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "image id : " + id);
        try {
            Optional<ImageAssets> optionalImageAssets = imageAssetsRepository.findById(id);
            if (optionalImageAssets.isPresent()) {
                ImageAssets imageAssets = optionalImageAssets.get();

                HttpHeaders headers = new HttpHeaders();
                headers.setCacheControl("max-age=86400");
                headers.setETag("\"" + imageAssets.getId() + "\""); // Set the ETag header

                if (ifNoneMatch != null && ifNoneMatch.equals(headers.getETag())) {
                    // If the ETag value in the request matches the current ETag value of the
                    // resource,
                    // return a 304 Not Modified response.
                    return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                            .headers(headers)
                            .contentType(MediaType.parseMediaType("image/jpeg")) // updated to JPEG
                            .build();
                } else {
                    // If the ETag value in the request does not match the current ETag value of the
                    // resource,
                    // return a 200 OK response with the resource.

                    // Set path based on image type
                    String directoryPath = imageAssetPath + "/" + imageAssets.getImageType().name();

                    File imageFile = new File(directoryPath + "/" + imageAssets.getFileName());
                    ByteArrayResource byteArrayResource = new ByteArrayResource(Files.readAllBytes(imageFile.toPath()));
                    return ResponseEntity.ok()
                            .headers(headers)
                            .contentType(MediaType.IMAGE_JPEG) // updated to JPEG
                            .body(byteArrayResource);
                }
            } else {
                HttpResponse response = new HttpResponse(request.getRequestURI());
                response.setStatus(HttpStatus.NOT_FOUND);
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                        "image not found, image id : " + id);
                return ResponseEntity.status(response.getStatus()).body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            HttpResponse response = new HttpResponse(request.getRequestURI());
            response.setStatus(HttpStatus.EXPECTATION_FAILED);
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());
            return ResponseEntity.status(response.getStatus()).body(response);
        }
    }

    @Operation(summary = "Add new image", description = "Admin can create save image in file server")
    @PostMapping(value = "/image-asset", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HttpResponse> postAssetImage(
            HttpServletRequest request,
            @RequestParam ImageType imageType,
            @RequestParam(value = "file", required = false) MultipartFile file)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "postAssetImage";
        Logger.application.info(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                "filename :" + file.getOriginalFilename());
        try {
            ImageAssets data = imageAssetService.saveImageAsset(file, imageType);
            response.setStatus(HttpStatus.CREATED);
            response.setData(data);
        } catch (IOException e) {
            e.printStackTrace();
            response.setStatus(HttpStatus.BAD_REQUEST);

            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logprefix,
                    "Exception " + e.getMessage());

        }

        return ResponseEntity.status(response.getStatus()).body(response);

    }

}
