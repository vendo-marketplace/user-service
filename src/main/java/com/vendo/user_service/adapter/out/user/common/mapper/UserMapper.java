package com.vendo.user_service.adapter.out.user.common.mapper;

import com.vendo.user_service.adapter.out.common.config.MapStructConfig;
import com.vendo.user_service.adapter.out.user.persistence.MongoUser;
import com.vendo.user_service.domain.user.dto.SaveUserRequest;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface UserMapper {

    MongoUser mapToUser(SaveUserRequest request);

}
