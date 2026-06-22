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
import java.util.Map;
import java.util.Set;
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
        // endDate가 null이면 startDate와 같게 설정 → startDate 단독 시 정확 일치 동작 유지
        LocalDate effectiveEndDate = (endDate != null) ? endDate : startDate;

        Page<Posting> postings = postingRepository.findAllBySearchCondition(
            startDate, effectiveEndDate, memberId, pageable
        );

        Set<Long> memberIds = postings.getContent().stream()
            .map(Posting::getMemberId)
            .collect(Collectors.toSet());

        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
            .collect(Collectors.toMap(Member::getId, m -> m));

        return postings.map(p -> {
            Member member = memberMap.get(p.getMemberId());
            return PostingResponse.PostingDto.from(
                p,
                member != null ? member.getNickname() : null,
                member != null ? member.getName() : null
            );
        });
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
            log.info("Created posting - member={}, week={}, counts={}{}{}{}{}{}{}",
                memberId, weekStartDate, mon, tue, wed, thu, fri, sat, sun);
        } else {
            existing.update(mon, tue, wed, thu, fri, sat, sun);
            postingRepository.save(existing);
            log.info("Updated posting - member={}, week={}, counts={}{}{}{}{}{}{}",
                memberId, weekStartDate, mon, tue, wed, thu, fri, sat, sun);
        }
    }
}
