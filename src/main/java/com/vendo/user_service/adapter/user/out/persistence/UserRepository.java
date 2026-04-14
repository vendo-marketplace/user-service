package com.vendo.user_service.adapter.user.out.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<MongoUser, String> {

    Optional<MongoUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
