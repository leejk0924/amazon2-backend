package com.jk.amazon2.posting.entity;

import com.jk.amazon2.common.entity.BaseAudit;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "posting")
@Entity
public class Posting extends BaseAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "mon")
    private Integer mon;

    @Column(name = "tue")
    private Integer tue;

    @Column(name = "wed")
    private Integer wed;

    @Column(name = "thu")
    private Integer thu;

    @Column(name = "fri")
    private Integer fri;

    @Column(name = "sat")
    private Integer sat;

    @Column(name = "sun")
    private Integer sun;

    public Posting(Long memberId, LocalDate weekStartDate,
                   Integer mon, Integer tue, Integer wed, Integer thu,
                   Integer fri, Integer sat, Integer sun, String createdBy) {
        this.memberId = memberId;
        this.weekStartDate = weekStartDate;
        this.mon = mon;
        this.tue = tue;
        this.wed = wed;
        this.thu = thu;
        this.fri = fri;
        this.sat = sat;
        this.sun = sun;
    }

    public void update(Integer mon, Integer tue, Integer wed, Integer thu,
                       Integer fri, Integer sat, Integer sun) {
        this.mon = mon;
        this.tue = tue;
        this.wed = wed;
        this.thu = thu;
        this.fri = fri;
        this.sat = sat;
        this.sun = sun;
    }
}
