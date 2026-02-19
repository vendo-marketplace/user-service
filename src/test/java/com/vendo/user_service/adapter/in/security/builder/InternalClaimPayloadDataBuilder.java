package com.vendo.user_service.adapter.in.security.builder;

import com.vendo.core_lib.type.ServiceName;
import com.vendo.core_lib.type.ServiceRole;
import com.vendo.user_service.adapter.out.security.jwt.dto.InternalClaimPayload;

import java.util.List;

public class InternalClaimPayloadDataBuilder {

    public static InternalClaimPayload.InternalClaimPayloadBuilder buildWithAllFields() {
        return InternalClaimPayload.builder()
                .subject(ServiceName.AUTH_SERVICE.toString())
                .audience(ServiceName.USER_SERVICE.toString())
                .roles(List.of(ServiceRole.INTERNAL.toString()));
    }

}
