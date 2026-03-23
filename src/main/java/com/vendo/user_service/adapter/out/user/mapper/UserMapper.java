package com.vendo.user_service.adapter.out.user.mapper;

import com.vendo.user_service.adapter.in.user.dto.SaveUserRequest;
import com.vendo.user_service.adapter.in.user.dto.UpdateUserRequest;
import com.vendo.user_service.adapter.out.config.mapper.MapStructConfig;
import com.vendo.user_service.adapter.out.user.persistence.MongoUser;
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
