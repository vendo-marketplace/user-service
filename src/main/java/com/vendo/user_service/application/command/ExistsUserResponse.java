package com.vendo.user_service.application.command;

import lombok.Builder;

@Builder
public record ExistsUserResponse(boolean exists) {
}
