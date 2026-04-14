package com.vendo.user_service.adapter.security.in;

import com.vendo.security_lib.resolver.AntPathResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

@Component
public class InternalAntPathResolver implements AntPathResolver {

    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public static final String[] INTERNAL_PATHS = new String[] {
            "/internal/**",
            "/actuator/health"
    };

    @Override
    public boolean isPermittedPath(String path) {
        return Arrays.stream(INTERNAL_PATHS).noneMatch(pr -> antPathMatcher.match(pr, path));
    }
}
