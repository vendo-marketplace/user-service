package com.vendo.user_service.common.builder;

import com.vendo.user_service.web.dto.CompleteAuthRequest;

import java.time.LocalDate;

public class CompleteAuthRequestDataBuilder {

    public static CompleteAuthRequest.CompleteAuthRequestBuilder buildCompleteAuthRequestWithAllFields() {
        return CompleteAuthRequest.builder()
                .fullName("Test Name")
                .birthDate(LocalDate.of(2000, 1, 1));
    }

}
