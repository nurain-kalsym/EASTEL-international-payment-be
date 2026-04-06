package com.kalsym.internationalPayment.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kalsym.internationalPayment.model.Country;

@Repository
public interface CountryRepository extends JpaRepository<Country, String> {
    Optional<Country> findByWspCountryCode(@Param("countryCode") String countryCode);
    Country findByNationality(String nationality);

    // Retrieve list of country codes
    @Query("SELECT c.countryCode FROM Country c")
    List<String> findAllCountryCodes();

}