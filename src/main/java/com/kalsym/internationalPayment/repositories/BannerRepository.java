package com.kalsym.internationalPayment.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.kalsym.internationalPayment.model.Banner;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Integer> {

    @Query("SELECT b FROM Banner b WHERE b.section = :section")
    List<Banner> findBySection(@Param("section") String section);

    @Transactional
    @Modifying
    @Query("DELETE FROM Banner b WHERE b.section = :section")
    void deleteBySection(@Param("section") String section);

}
