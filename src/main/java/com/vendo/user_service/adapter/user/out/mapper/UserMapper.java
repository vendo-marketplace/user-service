package com.vendo.user_service.adapter.user.out.mapper;

import com.vendo.user_service.adapter.user.in.dto.SaveUserRequest;
import com.vendo.user_service.adapter.user.in.dto.UpdateUserRequest;
import com.vendo.user_service.infrastructure.MapStructConfig;
import com.vendo.user_service.adapter.user.out.persistence.MongoUser;
import com.vendo.user_service.domain.user.User;
import org.mapstruct.*;

@Mapper(config = MapStructConfig.class)
public interface UserMapper {

    User toUser(MongoUser mongoUser);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MongoUser toMongoUser(SaveUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateUser(@MappingTarget MongoUser user, UpdateUserRequest request);

}
