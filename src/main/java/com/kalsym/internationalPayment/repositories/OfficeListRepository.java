package com.kalsym.internationalPayment.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalsym.internationalPayment.model.OfficeList;

@Repository
public interface OfficeListRepository extends JpaRepository<OfficeList, String> {
    List<OfficeList> findAllByProductId(Integer productId);

}