package com.kalsym.ekedai.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kalsym.ekedai.model.DiscountEvent;
import com.kalsym.ekedai.repositories.DiscountEventRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


@Service
public class DiscountEventService {
    
    @Autowired
    DiscountEventRepository discountEventRepository;

    public DiscountEvent createDiscountEvent(DiscountEvent discountEventBody){
        return discountEventRepository.save(discountEventBody);

    }

    public DiscountEvent updateDiscountEvent(Integer id, DiscountEvent discountEventBody){

        DiscountEvent data = discountEventRepository.findById(id).get();
        data.updateData(discountEventBody);

        return discountEventRepository.save(data);                                
    }

    public Page<DiscountEvent> getAllDiscountEvent(int page, int pageSize, String sortBy, Sort.Direction sortingOrder){
        
        Pageable pageable = PageRequest.of(page, pageSize);

        if (sortingOrder==Sort.Direction.ASC)
            pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).ascending());
        else if (sortingOrder==Sort.Direction.DESC)
            pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).descending());

        Page<DiscountEvent> result =discountEventRepository.findAll(pageable); 

        return result;
    }
}
