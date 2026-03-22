package com.vendo.user_service.adapter.out.user.persistence;

import com.vendo.user_lib.type.ProviderType;
import com.vendo.user_lib.type.UserRole;
import com.vendo.user_lib.type.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Document
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MongoUser{

    @Id
    private String id;

    private String email;
    private boolean emailVerified;
    private UserRole role;
    private UserStatus status;
    private ProviderType providerType;
    private String password;
    private LocalDate birthDate;
    private String fullName;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

}