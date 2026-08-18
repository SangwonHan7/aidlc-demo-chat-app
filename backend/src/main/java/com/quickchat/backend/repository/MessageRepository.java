package com.quickchat.backend.repository;

import com.quickchat.backend.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Cursor 기반 페이지네이션 (business-rules.md Q5 답변 A).
     * beforeSentAt이 null이면 최신 메시지부터, 아니면 그 시각보다 이전 메시지를 조회한다.
     */
    @Query("select m from Message m where m.channelId = :channelId "
            + "and (:beforeSentAt is null or m.sentAt < :beforeSentAt) "
            + "order by m.sentAt desc")
    List<Message> findPage(@Param("channelId") UUID channelId,
                            @Param("beforeSentAt") Instant beforeSentAt,
                            org.springframework.data.domain.Pageable pageable);
}
