package com.kalsym.ekedai.services;

import com.kalsym.ekedai.model.Banner;
import com.kalsym.ekedai.repositories.BannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BannerService {

    @Autowired
    private BannerRepository bannerRepository;

    public List<Banner> getAllBanners() {
        return bannerRepository.findAll();
    }

    public Banner getBannerById(int id) {
        return bannerRepository.findById(id).orElse(null);
    }

    public List<Banner> createBulkBanners(List<Banner> banners) {
        Date now = new Date();
        for (Banner banner : banners) {
            banner.setCreatedAt(now);
            banner.setUpdatedAt(now);
        }
        return bannerRepository.saveAll(banners);
    }

    public void deleteBanner(int id) {
        bannerRepository.deleteById(id);
    }
}