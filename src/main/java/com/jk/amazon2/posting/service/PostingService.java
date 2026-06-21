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
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostingService {

    private final PostingRepository postingRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Page<PostingResponse.PostingDto> getPostings(LocalDate startDate, Pageable pageable) {
        Page<Posting> postings = postingRepository.findAllByWeekStartDate(startDate, pageable);

        Set<Long> memberIds = postings.getContent().stream()
            .map(Posting::getMemberId)
            .collect(Collectors.toSet());

        Map<Long, String> nicknameMap = memberRepository.findAllById(memberIds).stream()
            .collect(Collectors.toMap(Member::getId, Member::getNickname));

        return postings.map(p -> PostingResponse.PostingDto.from(p, nicknameMap.get(p.getMemberId())));
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
