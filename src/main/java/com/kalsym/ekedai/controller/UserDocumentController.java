package com.kalsym.ekedai.controller;

import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.model.ImageAssets;
import com.kalsym.ekedai.model.User;
import com.kalsym.ekedai.model.UserDocument;
import com.kalsym.ekedai.model.dao.ProfileServiceResponse;
import com.kalsym.ekedai.model.dao.UserDocumentRequest;
import com.kalsym.ekedai.model.enums.ImageType;
import com.kalsym.ekedai.repositories.UserRepository;
import com.kalsym.ekedai.services.ImageAssetService;
import com.kalsym.ekedai.services.ProfileService;
import com.kalsym.ekedai.services.UserDocumentService;
import com.kalsym.ekedai.utility.HttpResponse;
import com.kalsym.ekedai.utility.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/user-documents")
public class UserDocumentController {

    @Autowired
    private UserDocumentService userDocumentService;

    @Autowired
    private ImageAssetService imageAssetService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<HttpResponse> createUserDocument(HttpServletRequest request,
            @RequestBody UserDocumentRequest userDocument) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logprefix = "createUserDocument";

        try {
            UserDocument createdDocument = userDocumentService.createUserDocument(userDocument);
            response.setData(createdDocument);
            response.setStatus(HttpStatus.OK);

            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logprefix,
                    "Error while creating user document: " + e.getMessage());

            response.setData("Could not create the document: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping()
    public ResponseEntity<HttpResponse> getAllUserDocuments(HttpServletRequest request,
            @RequestParam(defaultValue = "createdAt", required = false) String sortBy,
            @RequestParam(defaultValue = "ASC", required = false) String sortingOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) UserDocument.UserDocumentStatus status,
            @RequestParam(required = false) String globalSearch,
            @RequestParam(required = false) List<String> documentTypes,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date to) {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        // String logprefix = "getAllUserDocuments";

        Page<UserDocument> userDocuments = userDocumentService.getAllUserDocuments(sortBy, sortingOrder, page, pageSize,
                status, globalSearch, from, to, documentTypes);

        response.setStatus(HttpStatus.OK);
        response.setData(userDocuments);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HttpResponse> getUserDocumentById(HttpServletRequest request, @PathVariable String id) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        Optional<UserDocument> document = userDocumentService.getUserDocumentById(id);

        if (document.isPresent()) {
            response.setStatus(HttpStatus.OK);
            response.setData(document.get());
        } else {
            response.setStatus(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<HttpResponse> getUserDocumentsByUserId(HttpServletRequest request,
            @PathVariable String userId) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        List<UserDocument> documents = userDocumentService.getUserDocumentsByUserId(userId);

        response.setStatus(HttpStatus.OK);
        response.setData(documents);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping(path = { "/upload-document-image/{userId}" })
    public ResponseEntity<HttpResponse> postDocumentImage(
            HttpServletRequest request,
            @PathVariable("userId") String userId,
            @RequestParam(value = "file", required = false) MultipartFile file)
            throws Exception {

        HttpResponse response = new HttpResponse(request.getRequestURI());
        String logPrefix = "postDocumentPhoto";

        // Return error if not file provided
        if (file == null) {
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                    "No file provided");
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage("No file provided");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Log the filename and userId
        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                "userId: " + userId + ", filename: " + file.getOriginalFilename());

        // Find user documents
        List<UserDocument> userDocuments = userDocumentService.getUserDocumentsByUserIdAndStatus(userId,
                UserDocument.UserDocumentStatus.PENDING_UPLOAD);

        // Return if no document found
        if (userDocuments.isEmpty()) {

            response.setStatus(HttpStatus.NOT_FOUND, "No pending document");
            Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                    "Document with status PENDING_UPLOAD not found for user: " + userId);

            return ResponseEntity.status(response.getStatus()).body(response);
        }

        // Try to save image if documents found
        try {
            // Save the image asset
            ImageAssets data = imageAssetService.saveImageAsset(file, ImageType.document);

            // If a document with status PENDING_UPLOAD exists, update its imageId and
            // status
            UserDocument userDocument = userDocuments.get(0);
            userDocument.setImageId(data.getId());
            userDocument.setStatus(UserDocument.UserDocumentStatus.PENDING_VERIFICATION);
            userDocument = userDocumentService.updateUserDocument(userDocument);

            response.setStatus(HttpStatus.OK, "Document image uploaded successfully!");
            response.setData(userDocument);

            // Update mongodb user profile status
            try {
                Optional<User> optionalUser = userRepository.findById(userId);

                if (optionalUser.isPresent()) {
                    User user = optionalUser.get();
                    // Update document status to PENDING_VERIFICATION
                    ProfileServiceResponse profileServiceResponse = profileService.updateDocumentUploadStatus(
                            user.getPhoneNumber(),
                            UserDocument.UserDocumentStatus.PENDING_VERIFICATION.name());
                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                            "Customer profile service - update document status: ",
                            profileServiceResponse.getMessage());
                }
            } catch (Exception e) {
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                        "Customer profile service Update document status Error: " + e.getMessage());
            }

        } catch (IOException e) {
            // Handle exceptions
            response.setStatus(HttpStatus.BAD_REQUEST);
            response.setMessage(e.getMessage());

            // Log the exception message
            Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                    "Exception " + e.getMessage());
        }

        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/update-status/{id}")
    public ResponseEntity<HttpResponse> updateUserDocumentStatus(HttpServletRequest request, @PathVariable String id,
            @RequestParam UserDocument.UserDocumentStatus status) {
        HttpResponse response = new HttpResponse(request.getRequestURI());
        Optional<UserDocument> document = userDocumentService.getUserDocumentById(id);
        String logPrefix = "updateUserDocumentStatus";

        if (document.isPresent()) {
            UserDocument userDocument = document.get();
            Optional<User> optionalUser = userRepository.findById(userDocument.getUserId());

            if (optionalUser.isPresent()) {
                // Update mongodb user profile status
                try {
                    User user = optionalUser.get();
                    // Update document status to UPLOADED
                    ProfileServiceResponse profileServiceResponse = profileService.updateDocumentUploadStatus(
                            user.getPhoneNumber(),
                            "UPLOADED");
                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                            "Customer profile service - update document status: ",
                            profileServiceResponse.getMessage());
                } catch (Exception e) {
                    Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                            "Customer profile service Update document status Error: " + e.getMessage());

                    response.setStatus(HttpStatus.BAD_REQUEST);
                    response.setMessage("Customer profile service Update document status Error: " + e.getMessage());
                    return ResponseEntity.status(response.getStatus()).body(response);
                }
            }

            // Update status
            userDocument.setStatus(status);
            userDocument = userDocumentService.updateUserDocument(userDocument);

            response.setStatus(HttpStatus.OK);
            response.setData(userDocument);
        } else {
            response.setStatus(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.status(response.getStatus()).body(response);

    }

    // @PutMapping("/{id}")
    // public ResponseEntity<?> updateUserDocument(@PathVariable String id,
    // @RequestBody UserDocument userDocument) {
    // if (!userDocumentService.getUserDocumentById(id).isPresent()) {
    // return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document not
    // found");
    // }
    // userDocument.setId(id);
    // UserDocument updatedDocument =
    // userDocumentService.updateUserDocument(userDocument);
    // return ResponseEntity.ok(updatedDocument);
    // }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<?> deleteUserDocument(@PathVariable String id) {
    // if (!userDocumentService.getUserDocumentById(id).isPresent()) {
    // return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document not
    // found");
    // }
    // userDocumentService.deleteUserDocument(id);
    // return ResponseEntity.ok("Document deleted successfully");
    // }
}
