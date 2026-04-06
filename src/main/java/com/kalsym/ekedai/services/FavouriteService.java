package com.kalsym.ekedai.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kalsym.ekedai.model.Favourite;
import com.kalsym.ekedai.repositories.CountryRepository;
import com.kalsym.ekedai.repositories.FavouriteRepository;

@Service
public class FavouriteService {
    
    @Autowired
    FavouriteRepository favouriteRepository;


    @Autowired
    CountryRepository countryRepository;


    public Favourite createFavourite(Favourite favouriteBody){

        return favouriteRepository.save(favouriteBody);
    }

    public Favourite updateFavourite(Long id, Favourite favouriteBody){

        try{
            Favourite data = favouriteRepository.findById(id).get();
            
            data.updateData(favouriteBody);
    
            return favouriteRepository.save(data);                                
        }catch(Exception e){
            e.printStackTrace();
            throw e;
        }

    }

    
    public Optional<Favourite> getFavouriteById(Long id){

        return favouriteRepository.findById(id);                                
    }

    public Boolean deleteFavourite(Long id){
        Optional<Favourite> favourite = favouriteRepository.findById(id);

        if (favourite.isPresent()){
            
            favouriteRepository.deleteById(id);
            return true;

        } else{
            return false;
        }
    }

}
