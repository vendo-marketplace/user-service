package com.vendo.user_service.adapter.out.common.config;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN,
        unmappedSourcePolicy = ReportingPolicy.WARN,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface MapStructConfig {
}