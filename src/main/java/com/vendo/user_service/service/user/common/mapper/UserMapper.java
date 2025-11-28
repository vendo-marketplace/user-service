package com.vendo.user_service.service.user.common.mapper;

import com.vendo.user_service.common.config.MapStructConfig;
import com.vendo.user_service.model.User;
import com.vendo.user_service.web.dto.UserProfileResponse;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface UserMapper {

    UserProfileResponse toUserProfileResponse(User user);
}
