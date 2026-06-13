package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.dto.StatisticsResponse;
import com.jk.amazon2.posting.entity.Posting;
import com.jk.amazon2.posting.repository.PostingRepository;
import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final PostingRepository postingRepository;
    private final MemberRepository memberRepository;

    public StatisticsResponse getStatistics(LocalDate startDate, LocalDate endDate) {
        List<Member> members = memberRepository.findAll();
        List<Posting> postings = postingRepository.findAll();

        int totalPostings = 0;
        List<StatisticsResponse.UserStatistics> userStats = new ArrayList<>();

        for (Member member : members) {
            int memberTotal = 0;
            Map<String, Integer> dayOfWeekCounts = new HashMap<>();
            dayOfWeekCounts.put("mon", 0);
            dayOfWeekCounts.put("tue", 0);
            dayOfWeekCounts.put("wed", 0);
            dayOfWeekCounts.put("thu", 0);
            dayOfWeekCounts.put("fri", 0);
            dayOfWeekCounts.put("sat", 0);
            dayOfWeekCounts.put("sun", 0);

            for (Posting p : postings) {
                if (p.getMemberId().equals(member.getId()) &&
                    isInRange(p.getWeekStartDate(), startDate, endDate)) {

                    memberTotal += p.getMon() + p.getTue() + p.getWed() + p.getThu() +
                                 p.getFri() + p.getSat() + p.getSun();

                    dayOfWeekCounts.put("mon", dayOfWeekCounts.get("mon") + p.getMon());
                    dayOfWeekCounts.put("tue", dayOfWeekCounts.get("tue") + p.getTue());
                    dayOfWeekCounts.put("wed", dayOfWeekCounts.get("wed") + p.getWed());
                    dayOfWeekCounts.put("thu", dayOfWeekCounts.get("thu") + p.getThu());
                    dayOfWeekCounts.put("fri", dayOfWeekCounts.get("fri") + p.getFri());
                    dayOfWeekCounts.put("sat", dayOfWeekCounts.get("sat") + p.getSat());
                    dayOfWeekCounts.put("sun", dayOfWeekCounts.get("sun") + p.getSun());
                }
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

    private boolean isInRange(LocalDate weekStart, LocalDate rangeStart, LocalDate rangeEnd) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return !weekEnd.isBefore(rangeStart) && !weekStart.isAfter(rangeEnd);
    }
}
