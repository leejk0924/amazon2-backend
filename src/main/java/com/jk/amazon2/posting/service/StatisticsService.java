package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.dto.StatisticsResponse;
import com.jk.amazon2.posting.dto.WeeklyStatisticsResponse;
import com.jk.amazon2.posting.entity.Posting;
import com.jk.amazon2.posting.exception.PostingErrorCode;
import com.jk.amazon2.posting.exception.PostingException;
import com.jk.amazon2.posting.repository.PostingRepository;
import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final PostingRepository postingRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(LocalDate startDate, LocalDate endDate) {
        List<Member> members = memberRepository.findAllByDeletedFalse();
        if (members.isEmpty()) {
            return new StatisticsResponse(startDate, endDate, 0, List.of());
        }

        List<Long> memberIds = members.stream().map(Member::getId).toList();
        List<Posting> postings = postingRepository.findAllByMemberIdsAndDateRange(memberIds, startDate, endDate);

        Map<Long, List<Posting>> postingsByMember = postings.stream()
            .collect(Collectors.groupingBy(Posting::getMemberId));

        int totalPostings = 0;
        List<StatisticsResponse.UserStatistics> userStats = new ArrayList<>();

        for (Member member : members) {
            List<Posting> memberPostings = postingsByMember.getOrDefault(member.getId(), List.of());

            int memberTotal = 0;
            Map<String, Integer> dayOfWeekCounts = new HashMap<>();
            dayOfWeekCounts.put("mon", 0);
            dayOfWeekCounts.put("tue", 0);
            dayOfWeekCounts.put("wed", 0);
            dayOfWeekCounts.put("thu", 0);
            dayOfWeekCounts.put("fri", 0);
            dayOfWeekCounts.put("sat", 0);
            dayOfWeekCounts.put("sun", 0);

            for (Posting p : memberPostings) {
                memberTotal += nullToZero(p.getMon()) + nullToZero(p.getTue()) + nullToZero(p.getWed())
                             + nullToZero(p.getThu()) + nullToZero(p.getFri())
                             + nullToZero(p.getSat()) + nullToZero(p.getSun());

                dayOfWeekCounts.put("mon", dayOfWeekCounts.get("mon") + nullToZero(p.getMon()));
                dayOfWeekCounts.put("tue", dayOfWeekCounts.get("tue") + nullToZero(p.getTue()));
                dayOfWeekCounts.put("wed", dayOfWeekCounts.get("wed") + nullToZero(p.getWed()));
                dayOfWeekCounts.put("thu", dayOfWeekCounts.get("thu") + nullToZero(p.getThu()));
                dayOfWeekCounts.put("fri", dayOfWeekCounts.get("fri") + nullToZero(p.getFri()));
                dayOfWeekCounts.put("sat", dayOfWeekCounts.get("sat") + nullToZero(p.getSat()));
                dayOfWeekCounts.put("sun", dayOfWeekCounts.get("sun") + nullToZero(p.getSun()));
            }

            if (memberTotal > 0) {
                totalPostings += memberTotal;
                userStats.add(new StatisticsResponse.UserStatistics(
                    member.getId(),
                    member.getNickname(),
                    memberTotal,
                    dayOfWeekCounts
                ));
            }
        }

        return new StatisticsResponse(startDate, endDate, totalPostings, userStats);
    }

    @Transactional(readOnly = true)
    public WeeklyStatisticsResponse getWeeklyStatistics(LocalDate weekStartDate) {
        if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new PostingException(PostingErrorCode.INVALID_WEEK_START_DATE);
        }

        long totalMemberCount = memberRepository.countByDeletedFalse();
        List<Posting> weekPostings = postingRepository.findAllByWeekStartDate(weekStartDate);

        long totalPostingCount = 0;
        long activeMemberCount = 0;

        for (Posting p : weekPostings) {
            int memberTotal = nullToZero(p.getMon()) + nullToZero(p.getTue()) + nullToZero(p.getWed())
                            + nullToZero(p.getThu()) + nullToZero(p.getFri())
                            + nullToZero(p.getSat()) + nullToZero(p.getSun());
            if (memberTotal > 0) {
                activeMemberCount++;
                totalPostingCount += memberTotal;
            }
        }

        double average = activeMemberCount > 0
            ? Math.round((double) totalPostingCount / activeMemberCount * 100.0) / 100.0
            : 0.0;

        return new WeeklyStatisticsResponse(
            weekStartDate,
            totalPostingCount,
            totalMemberCount,
            activeMemberCount,
            average
        );
    }

    private int nullToZero(Integer value) {
        return value != null ? value : 0;
    }
}
