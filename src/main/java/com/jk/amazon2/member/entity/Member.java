package com.jk.amazon2.member.entity;

import com.jk.amazon2.common.entity.BaseAudit;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
@Entity
public class Member extends BaseAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_code", length = 10)
    private String categoryCode;

    @Column(name = "nickname", nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT(1) NOT NULL DEFAULT 0")
    private boolean deleted = Boolean.FALSE;

    public static Member of(String nickname, String name, String categoryCode) {
        Member member = new Member();
        member.nickname = nickname;
        member.name = name;
        member.categoryCode = categoryCode;
        return member;
    }

    public void update(String nickname, String name, String categoryCode) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        this.name = name;
        this.categoryCode = categoryCode;
    }

    public void softDelete() {
        this.deleted = true;
    }

    public void restore() {
        this.deleted = false;
    }
}
