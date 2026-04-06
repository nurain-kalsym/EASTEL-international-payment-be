package com.kalsym.ekedai.repositories;

import com.kalsym.ekedai.model.UserMerge;
import com.kalsym.ekedai.model.enums.UserMergeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserMergeRepository extends JpaRepository<UserMerge, Long> {
    List<UserMerge> findByMergeStatus(@Param("mergeStatus") UserMergeStatus mergeStatus);
}