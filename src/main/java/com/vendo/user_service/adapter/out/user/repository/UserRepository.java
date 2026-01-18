package com.vendo.user_service.adapter.out.user.repository;

import com.vendo.user_service.adapter.out.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

}
