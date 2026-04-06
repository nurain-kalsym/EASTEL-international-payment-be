package com.kalsym.ekedai.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "country")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Country {
    @Id
    private String countryCode;

    private String countryName;

    private String countryCurrency;

    private String countryPhoneCode;

    private String currencySymbol;

    private String wspCountryCode;

    private String imageId;
    @OneToOne()
    @JoinColumn(name = "imageId", referencedColumnName = "id", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private ImageAssets imageDetails;

    private String nationality;

}
