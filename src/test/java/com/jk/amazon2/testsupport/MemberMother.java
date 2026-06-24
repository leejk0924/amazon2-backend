package com.jk.amazon2.testsupport;

import com.jk.amazon2.member.dto.MemberRequest;

public class MemberMother {

    public static final String INSERT_SQL =
            "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) " +
            "VALUES (?, ?, ?, NOW(), 'system', NOW(), 'system')";

    public static Object[] activeParams(String nickname, String categoryCode) {
        return new Object[]{nickname, categoryCode, false};
    }

    public static Object[] deletedParams(String nickname, String categoryCode) {
        return new Object[]{nickname, categoryCode, true};
    }

    public static MemberRequest.MemberCreateDto createDto(String nickname, String categoryCode) {
        return new MemberRequest.MemberCreateDto(nickname, "테스터", categoryCode);
    }

    public static MemberRequest.MemberDto updateDto(String nickname, String categoryCode) {
        return new MemberRequest.MemberDto(nickname, null, categoryCode);
    }
}
