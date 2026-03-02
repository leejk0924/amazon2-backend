package com.jk.amazon2.common.auditor;

import lombok.NonNull;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

public class CurrentUserProvider implements AuditorAware<String> {
    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        // TODO: 추후 로그인 기능 구현 시 관리 구현 예정
        return Optional.of("admin");
    }
}
