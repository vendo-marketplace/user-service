package com.vendo.user_service.adapter.security.in;

import com.vendo.security_lib.resolver.AntPathResolver;
import com.vendo.security_starter.path.PathGroup;
import com.vendo.security_starter.path.PathProps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class InternalAntPathResolver implements AntPathResolver {

    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private final PathProps props;

    @Override
    public boolean isPermittedPath(String path) {
        return Arrays.stream(props.paths(PathGroup.GENERAL)).anyMatch(pr -> antPathMatcher.match(pr, path));
    }
}
