package com.kalsym.ekedai.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalsym.ekedai.model.OfficeList;

@Repository
public interface OfficeListRepository extends JpaRepository<OfficeList, String> {
    List<OfficeList> findAllByProductId(Integer productId);

}