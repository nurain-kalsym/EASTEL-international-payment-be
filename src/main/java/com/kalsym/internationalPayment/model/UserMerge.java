package com.kalsym.internationalPayment.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.util.Date;

import com.kalsym.internationalPayment.model.enums.UserMergeStatus;

@Entity
@Table(name = "user_merge")
@Getter
@Setter
public class UserMerge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oldUserId", nullable = false)
    private String oldUserId;

    @Column(name = "newUserId", nullable = false)
    private String newUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mergeStatus")
    private UserMergeStatus mergeStatus;

    @Column(name = "mergeDate", nullable = false, updatable = false, insertable = false)
    private Date mergeDate;

    @Column(name = "reasonForMerge")
    private String reasonForMerge;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "oldUserId", referencedColumnName = "id", insertable = false, updatable = false)
//    private User oldUser;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "newUserId", referencedColumnName = "id", insertable = false, updatable = false)
//    private User newUser;
}
