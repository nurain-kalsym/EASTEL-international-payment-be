package com.kalsym.ekedai.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kalsym.ekedai.model.enums.UserStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.*;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class User {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(unique = true)
    private String email;

    @Column(unique = true) // in db declare constraint name: User_un_phone
    private String phoneNumber;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String fullName;

    private String password;

    private String nationality;

    private Boolean isEnable;

    @Column(nullable = false)
    private String role;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date created;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Kuala_Lumpur")
    private Date updated;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Transient
    private String referral;

    private String language;

    @Transient
    private String documentStatus;

    @Transient
    private String documentNo;

    private String imageId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imageId", referencedColumnName = "id", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private ImageAssets imageDetails;

    @Transient
    private String mergeStatus;

}
