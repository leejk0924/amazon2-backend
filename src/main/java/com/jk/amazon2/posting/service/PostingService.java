package com.jk.amazon2.posting.service;

import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.posting.dto.PostingResponse;
import com.jk.amazon2.posting.entity.Posting;
import com.jk.amazon2.posting.repository.PostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostingService {

    private final PostingRepository postingRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Page<PostingResponse.PostingDto> getPostings(
            LocalDate startDate, LocalDate endDate, Long memberId, Pageable pageable) {
        LocalDate effectiveEndDate = (endDate != null) ? endDate : startDate;

        Page<Member> members = memberRepository.findActiveMembers(memberId, pageable);

        List<Long> memberIds = members.getContent().stream()
            .map(Member::getId)
            .toList();

        if (memberIds.isEmpty()) {
            return members.map(m -> PostingResponse.PostingDto.from(m, null, startDate));
        }

        Map<Long, Posting> postingMap = postingRepository
            .findAllByMemberIdsAndDateRange(memberIds, startDate, effectiveEndDate)
            .stream()
            .collect(Collectors.toMap(Posting::getMemberId, p -> p));

        return members.map(m -> PostingResponse.PostingDto.from(
            m,
            postingMap.get(m.getId()),
            startDate
        ));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void savePosting(Long memberId, LocalDate weekStartDate,
                           Integer mon, Integer tue, Integer wed, Integer thu,
                           Integer fri, Integer sat, Integer sun, String createdBy) {
        Posting existing = postingRepository
            .findByMemberIdAndWeekStartDateWithLock(memberId, weekStartDate)
            .orElse(null);

        if (existing == null) {
            Posting newPosting = new Posting(
                memberId, weekStartDate,
                mon, tue, wed, thu, fri, sat, sun,
                createdBy
            );
            postingRepository.save(newPosting);
            log.debug("Created posting - member={}, week={}, counts={}{}{}{}{}{}{}",
                memberId, weekStartDate, mon, tue, wed, thu, fri, sat, sun);
        } else {
            existing.update(mon, tue, wed, thu, fri, sat, sun);
            postingRepository.save(existing);
            log.debug("Updated posting - member={}, week={}, counts={}{}{}{}{}{}{}",
                memberId, weekStartDate, mon, tue, wed, thu, fri, sat, sun);
        }
    }
}
