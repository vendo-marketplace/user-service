package com.vendo.user_service.common.mapper;

import com.vendo.user_service.common.config.MapStructConfig;
import com.vendo.user_service.db.model.User;
import com.vendo.user_service.web.dto.UserProfileResponse;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface UserMapper {

    UserProfileResponse toUserProfileResponse(User user);

}
