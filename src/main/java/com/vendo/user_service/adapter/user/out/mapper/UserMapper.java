package com.vendo.user_service.adapter.user.out.mapper;

import com.vendo.user_service.adapter.user.in.dto.SaveUserRequest;
import com.vendo.user_service.adapter.user.in.dto.UpdateUserRequest;
import com.vendo.user_service.infrastructure.MapStructConfig;
import com.vendo.user_service.adapter.user.out.persistence.MongoUser;
import com.vendo.user_service.domain.user.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapStructConfig.class)
public interface UserMapper {

    User toUser(MongoUser mongoUser);
    MongoUser toMongoUser(SaveUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUser(@MappingTarget MongoUser user, UpdateUserRequest request);

}
