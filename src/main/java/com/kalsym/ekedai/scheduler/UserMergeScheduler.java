package com.kalsym.ekedai.scheduler;

import com.kalsym.ekedai.services.UserMergeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UserMergeScheduler {

    @Autowired
    private UserMergeService userMergeService;

    @Scheduled(cron = "0 1 0 * * ?") // Runs every day at 12:01 AM
    public void processPendingMerges() {
        userMergeService.processPendingMerges();
    }
}
