package com.quickchat.backend.repository;

import com.quickchat.backend.domain.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Cursor 기반 페이지네이션의 첫 페이지(business-rules.md Q5 답변 A) - 최신 메시지부터 조회.
     *
     * <p>2026-08-22 실제 배포 환경(docker-compose, PostgreSQL)에서 처음 발견된 실제 버그: 원래는
     * {@code @Query("... where m.channelId = :channelId and (:beforeSentAt is null or m.sentAt <
     * :beforeSentAt) ...")} 하나의 JPQL로 null 여부에 따라 분기했으나, {@code beforeSentAt}이 실제로
     * null일 때 PostgreSQL이 {@code $2} 파라미터의 타입을 추론하지 못해
     * "could not determine data type of parameter $2"(SQLState 42P18) 오류로 모든 첫 페이지 조회가
     * 500으로 실패했다(이 세션 내내 mcp__workspace__bash 샌드박스가 막혀 있어 실제 DB에 붙여본 적이
     * 없었던 탓에 지금까지 발견되지 못함). null 파라미터의 타입 추론이 필요한 단일 JPQL 대신, null
     * 여부에 따라 아예 다른(둘 다 파라미터가 항상 non-null인) 쿼리 메서드로 분리해 문제의 근본 원인을
     * 제거했다.
     */
    List<Message> findByChannelIdOrderBySentAtDesc(UUID channelId, Pageable pageable);

    /** Cursor 기반 페이지네이션의 다음 페이지 - beforeSentAt보다 이전 메시지만 조회. 위 설명 참고. */
    List<Message> findByChannelIdAndSentAtLessThanOrderBySentAtDesc(
            UUID channelId, Instant beforeSentAt, Pageable pageable);
}
