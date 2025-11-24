package com.vendo.user_service.service.user.common.mapper;

import com.vendo.user_service.model.User;
import com.vendo.user_service.web.dto.UserProfileResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserProfileResponse toUserProfileResponse(User user);
}
