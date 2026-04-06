package com.kalsym.ekedai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.ImageAssets;

import java.util.Optional;

@Repository
public interface ImageAssetsRepository extends JpaRepository<ImageAssets, String>{
    Optional<ImageAssets> findByFileName(String fileName);
}
