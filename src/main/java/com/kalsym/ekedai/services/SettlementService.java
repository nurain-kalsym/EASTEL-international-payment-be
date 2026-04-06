package com.kalsym.ekedai.services;

import com.kalsym.ekedai.model.Settlement;
import com.kalsym.ekedai.repositories.SettlementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettlementService {

    @Autowired
    private SettlementRepository repository;

    public List<Settlement> getAllSettlements() {
        return repository.findAll();
    }

    public Settlement getSettlementById(Integer id) {
        return repository.findById(id).orElse(null);
    }

    public Settlement saveSettlement(Settlement settlement) {
        return repository.save(settlement);
    }

    public void deleteSettlement(Integer id) {
        repository.deleteById(id);
    }

}
