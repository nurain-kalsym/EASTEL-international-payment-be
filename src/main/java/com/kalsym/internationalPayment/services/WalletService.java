package com.kalsym.internationalPayment.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kalsym.internationalPayment.InternationalPaymentApplication;
import com.kalsym.internationalPayment.model.User;
import com.kalsym.internationalPayment.model.Wallet;
import com.kalsym.internationalPayment.model.enums.Status;
import com.kalsym.internationalPayment.repositories.UserRepository;
import com.kalsym.internationalPayment.repositories.WalletRepository;
import com.kalsym.internationalPayment.utility.Logger;

import java.util.Date;
import java.util.Optional;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    public Wallet createWallet(String logPrefix, String userId) {

        try {
            Wallet newWallet = null;

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                Optional<Wallet> walletOpt = walletRepository.findByUserId(userId);
                if (!walletOpt.isPresent()) {
                    newWallet = new Wallet();
                    newWallet.setUserId(userId);
                    newWallet.setBalance(0.00);
                    newWallet.setStatus(Status.ACTIVE);
                    newWallet.setUpdated(new Date());

                    walletRepository.save(newWallet);
                 } else {
                    Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "User already have a wallet tied to their account.");
                }
            } else {
                Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "User not found.");
            }
            
            return newWallet;
        } catch (Exception e) {
            Logger.application.error(Logger.pattern, InternationalPaymentApplication.VERSION, logPrefix, "Exception: ",
                    e.getMessage());
            return null;
        }


    }

}