package com.kalsym.ekedai.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.Favourite;

@Repository
public interface FavouriteRepository extends JpaRepository<Favourite, Long> {

    Page<Favourite> findAll(Specification<Favourite> favouriteByUser, Pageable pageable);
    Favourite findByUserIdAndProductCode(String userId, String productCode);

}