package com.vendo.user_service.adapter.in.security;

import com.vendo.security.AntPathResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

@Component
public class InternalAntPathResolver implements AntPathResolver {

    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public static final String[] INTERNAL_PATHS = new String[] {
            "/internal/**",
    };

    @Override
    public boolean isPermittedPath(String path) {
        return Arrays.stream(INTERNAL_PATHS).noneMatch(pr -> antPathMatcher.match(pr, path));
    }
}
