package com.kalsym.ekedai.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class UserMergeRequest {
    private String newUserId;
    private String oldPhoneNumber;
    private String mergeReason;
}
