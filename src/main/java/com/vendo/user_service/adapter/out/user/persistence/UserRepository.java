package com.vendo.user_service.adapter.out.user.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<MongoUser, String> {

    Optional<MongoUser> findByEmail(String email);

}
