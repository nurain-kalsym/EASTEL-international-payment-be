package com.kalsym.internationalPayment.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.kalsym.internationalPayment.model.ImageAssets;
import com.kalsym.internationalPayment.model.enums.ImageType;
import com.kalsym.internationalPayment.repositories.ImageAssetsRepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class ImageAssetService {

    @Value("${image.assets.location}")
    private String imageAssetPath;

    @Autowired
    ImageAssetsRepository imageAssetsRepository;

    @Autowired
    MySQLUserDetailsService mySQLUserDetailsService;

    public ImageAssets saveImageAsset(MultipartFile file, ImageType imageType) throws IOException {
        String oriFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileName = oriFileName.substring(0, oriFileName.lastIndexOf("."));
        String extensionType = oriFileName.substring(oriFileName.lastIndexOf(".") + 1);

        String renameFile = fileName + "_" + mySQLUserDetailsService.generateRandomCode() + "." + extensionType;

        Date now = new Date();
        ImageAssets dataImage = new ImageAssets();
        dataImage.setFileName(renameFile);
        dataImage.setCreated(now);
        dataImage.setVersion(now);
        dataImage.setImageType(imageType);

        // Append subdirectory based on image type
        String directoryPath = imageAssetPath + "/" + imageType.name();

        saveFile(directoryPath, renameFile, file); // This might throw IOException
        return imageAssetsRepository.save(dataImage);
    }

    public static void saveFile(String imageAssetPath, String fileName, MultipartFile multipartFile) throws IOException {
        Path uploadPath = Paths.get(imageAssetPath);

        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException ioe) {
                throw new IOException("Could not create directory: " + uploadPath, ioe);
            }
        }

        try (InputStream inputStream = multipartFile.getInputStream()) {
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            throw new IOException("Could not save image file: " + fileName, ioe);
        }
    }

    public Page<ImageAssets> getImageAssetsList(int page, int pageSize, String sortByCol, Sort.Direction sortingOrder) {

        Pageable pageable = PageRequest.of(page, pageSize);

        if (sortingOrder == Sort.Direction.ASC)
            pageable = PageRequest.of(page, pageSize, Sort.by(sortByCol).ascending());
        else if (sortingOrder == Sort.Direction.DESC)
            pageable = PageRequest.of(page, pageSize, Sort.by(sortByCol).descending());

        Page<ImageAssets> result = imageAssetsRepository.findAll(pageable);

        return result;
    }

    public List<String> bulkDeleteImage(List<String> imageIds) {

        List<String> deletedIds = new ArrayList<>();
        for (String imageId : imageIds) {

            Optional<ImageAssets> optImage = imageAssetsRepository.findById(imageId);

            if (optImage.isPresent()) {

                try {
                    ImageAssets imageAssets = optImage.get();

                    imageAssetsRepository.deleteById(imageId);
                    deleteImageFile(imageAssets.getFileName(), imageAssets.getImageType());

                    deletedIds.add(imageId);

                } catch (Exception e) {

                }

            }

        }

        return deletedIds;
    }

    public void deleteImageFile(String fileName, ImageType imageType) {

        try {
            // Append subdirectory based on image type
            String directoryPath = imageAssetPath + "/" + imageType.name();

            File file = new File(directoryPath + "/" + fileName);
            if (file.delete()) {
                System.out.println(file.getName() + " is deleted!");
                // return true;
            } else {
                System.out.println("Sorry, unable to delete the file.");
                // return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // return false;

        }
    }

    public String deleteImageById(String imageId) {
        Optional<ImageAssets> optImage = imageAssetsRepository.findById(imageId);
        if (optImage.isPresent()) {
            try {
                ImageAssets imageAssets = optImage.get();

                imageAssetsRepository.deleteById(imageId);
                deleteImageFile(imageAssets.getFileName(), imageAssets.getImageType());
                return "Successfully deleted image by ID: " + imageId;

            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        else {
            return "Error: Image not found by ID of: " + imageId;
        }
    }
}
