package com.kalsym.ekedai.services;

import com.kalsym.ekedai.EkedaiApplication;
import com.kalsym.ekedai.model.DiscountUser;
import com.kalsym.ekedai.model.Transaction;
import com.kalsym.ekedai.model.User;
import com.kalsym.ekedai.model.UserMerge;
import com.kalsym.ekedai.model.dao.ProfileServiceResponse;
import com.kalsym.ekedai.model.enums.UserMergeStatus;
import com.kalsym.ekedai.model.enums.UserStatus;
import com.kalsym.ekedai.repositories.DiscountUserRepository;
import com.kalsym.ekedai.repositories.TransactionRepository;
import com.kalsym.ekedai.repositories.UserMergeRepository;
import com.kalsym.ekedai.repositories.UserRepository;
import com.kalsym.ekedai.utility.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class UserMergeService {

    @Autowired
    private UserMergeRepository userMergeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiscountUserRepository discountUserRepository;

    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional
    public UserMerge initiateMerge(String newUserId, String oldPhoneNumber, String reason) {
        String logPrefix = "initiateMerge";
        Optional<User> newUserOptional = userRepository.findById(newUserId);
        Optional<User> oldUserOptional = userRepository.findByPhoneNumber(oldPhoneNumber);

        if (newUserOptional.isPresent() && oldUserOptional.isPresent()) {
            User newUser = newUserOptional.get();
            User oldUser = oldUserOptional.get();
            String newPhoneNumber = newUser.getPhoneNumber();

            // Merge mongodb user profile
            try {
                ProfileServiceResponse profileServiceResponse = profileService.mergeProfile(newPhoneNumber, oldPhoneNumber);
                Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                        "Customer profile service - Merge profile: ",
                        profileServiceResponse.getMessage());
            } catch (Exception e) {
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                        "Customer profile service Merge profile Error: " + e.getMessage());
                throw new RuntimeException("Failed to request merge profiles in MongoDB", e);
            }

            // Log the merge in the UserMerge table
            UserMerge userMerge = new UserMerge();
            userMerge.setNewUserId(newUserId); // new user id
            userMerge.setOldUserId(oldUser.getId()); // old user id
            userMerge.setMergeStatus(UserMergeStatus.PENDING);
            userMerge.setReasonForMerge(reason);

            return userMergeRepository.save(userMerge);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    @Transactional
    public void processPendingMerges() {
        String logPrefix = "processPendingMerges";

        Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, "UserMergeScheduler", "Getting pending merges...");

        // Fetch all pending merge records from the database
        List<UserMerge> pendingMerges = userMergeRepository.findByMergeStatus(UserMergeStatus.PENDING);

        // Iterate over each pending merge request
        for (UserMerge merge : pendingMerges) {
            try {
                // Find both the new user (who requested the merge) and the old user
                Optional<User> newUserOptional = userRepository.findById(merge.getNewUserId());
                Optional<User> oldUserOptional = userRepository.findById(merge.getOldUserId());

                // Ensure both new and old user exist before proceeding
                if (newUserOptional.isPresent() && oldUserOptional.isPresent()) {
                    User newUser = newUserOptional.get();
                    User oldUser = oldUserOptional.get();

                    // Step 1: Disable the old user's account
                    oldUser.setStatus(UserStatus.INACTIVE);   // Mark the old user as inactive
                    oldUser.setIsEnable(false);               // Disable the old user's account
                    userRepository.save(oldUser);             // Save the updated status to the database
                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                            "Disabled old account with ID: " + merge.getOldUserId());

                    // Step 2: Migrate discount user entries associated with the old phone number
                    List<DiscountUser> discountUsers = discountUserRepository.getDiscountUserByUserPhoneNumber(oldUser.getPhoneNumber());

                    for (DiscountUser oldDiscountUser : discountUsers) {
                        DiscountUser newDiscountUser = new DiscountUser();
                        newDiscountUser.setDiscountId(oldDiscountUser.getDiscountId());    // Copy the discount ID
                        newDiscountUser.setUserPhoneNumber(newUser.getPhoneNumber());      // Set the new user's phone number
                        newDiscountUser.setStatus(oldDiscountUser.getStatus());            // Copy over the status of the discount

                        // Save the new DiscountUser and delete the old one
                        discountUserRepository.save(newDiscountUser);
                        discountUserRepository.delete(oldDiscountUser);  // Remove the old entry
                    }

                    Logger.application.info(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                            "Updated Discount User table for new number: " + newUser.getPhoneNumber());

                    // Step 3: Migrate transactions associated with the old user to the new user
                    List<Transaction> transactions = transactionRepository.findByUserId(oldUser.getId());

                    for (Transaction transaction : transactions) {
                        transaction.setUserId(newUser.getId());   // Update the user ID in the transaction to the new user
                    }

                    transactionRepository.saveAll(transactions);   // Save all updated transactions

                    // Step 4: Mark the merge as complete by updating the status to MERGED
                    merge.setMergeStatus(UserMergeStatus.MERGED);
                    userMergeRepository.save(merge);  // Save the merge status change

                } else {
                    // If either the old or new user is not found, throw an exception
                    throw new RuntimeException("User not found");
                }

            } catch (Exception e) {
                // Log any errors during the merge process
                Logger.application.error(Logger.pattern, EkedaiApplication.VERSION, logPrefix,
                        "Error merging user", e);

                // Update the merge status to FAILED in case of an error
                merge.setMergeStatus(UserMergeStatus.FAILED);
                userMergeRepository.save(merge);  // Save the failure status

                // Rethrow the exception to ensure the transaction is rolled back
                throw new RuntimeException("Error processing merge", e);
            }
        }
    }

}
