package com.quickchat.backend.service;

import com.quickchat.backend.domain.*;
import com.quickchat.backend.exception.AlreadyMemberException;
import com.quickchat.backend.redis.MembershipCacheService;
import com.quickchat.backend.repository.ChannelMemberRepository;
import com.quickchat.backend.repository.ChannelRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
// mock()/when() 만 사용하므로 Mockito 와일드카드 임포트는 필요 없음

/**
 * PBT-01 Testable Properties (ChannelComponent, Invariant):
 * - "생성자는 항상 멤버" / "중복 참여/초대는 항상 멤버 목록을 변경하지 않음" - business-logic-model.md 참고.
 * - "캐시 무효화 후 캐시-DB 일관성" - nfr-design-patterns.md 보완 항목.
 */
class ChannelServicePropertyTest {

    @Property
    @Label("createChannel 이후 생성자는 항상 채널의 OWNER 멤버다")
    void creatorIsAlwaysMember(@ForAll @AlphaChars @StringLength(min = 1, max = 40) String channelName) {
        ChannelHarness h = ChannelHarness.create();
        UUID ownerId = UUID.randomUUID();

        Channel channel = h.service.createChannel(channelName, ChannelType.GROUP, ChannelVisibility.PUBLIC, ownerId);

        assertThat(h.service.isMember(channel.getId(), ownerId)).isTrue();
        assertThat(h.members).anyMatch(m -> m.getUserId().equals(ownerId) && m.getRole() == ChannelRole.OWNER);
    }

    @Property
    @Label("이미 멤버인 사용자의 재참여 시도는 항상 실패하고 멤버 목록을 바꾸지 않는다")
    void duplicateJoinNeverChangesMembership(@ForAll @AlphaChars @StringLength(min = 1, max = 40) String channelName) {
        ChannelHarness h = ChannelHarness.create();
        UUID ownerId = UUID.randomUUID();
        UUID joinerId = UUID.randomUUID();

        Channel channel = h.service.createChannel(channelName, ChannelType.GROUP, ChannelVisibility.PUBLIC, ownerId);
        h.service.joinChannel(channel.getId(), joinerId);
        int sizeAfterFirstJoin = h.members.size();

        assertThatThrownBy(() -> h.service.joinChannel(channel.getId(), joinerId))
                .isInstanceOf(AlreadyMemberException.class);

        assertThat(h.members).hasSize(sizeAfterFirstJoin);
    }

    @Property
    @Label("멤버 제외 후 캐시가 무효화되면 isMember는 항상 DB의 실제 상태(false)와 일치한다 (캐시-DB 일관성)")
    void isMemberReflectsTrueStateAfterCacheInvalidation(
            @ForAll @AlphaChars @StringLength(min = 1, max = 40) String channelName) {
        ChannelHarness h = ChannelHarness.create();
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        Channel channel = h.service.createChannel(channelName, ChannelType.GROUP, ChannelVisibility.PUBLIC, ownerId);
        h.service.joinChannel(channel.getId(), memberId);
        assertThat(h.service.isMember(channel.getId(), memberId)).isTrue(); // 캐시에 적재됨

        h.service.removeMember(channel.getId(), ownerId, memberId); // 캐시 무효화 + DB 삭제

        assertThat(h.service.isMember(channel.getId(), memberId)).isFalse();
    }

    /** ChannelService + 실제 상태를 흉내내는 in-memory 백킹의 Mockito 조합. */
    private static final class ChannelHarness {
        final ChannelService service;
        final List<ChannelMember> members = new ArrayList<>();

        private ChannelHarness(ChannelService service) {
            this.service = service;
        }

        static ChannelHarness create() {
            ChannelRepository channelRepository = mock(ChannelRepository.class);
            ChannelMemberRepository memberRepository = mock(ChannelMemberRepository.class);
            MembershipCacheService membershipCache = mock(MembershipCacheService.class);

            // membershipCache를 실제 상태가 있는 것처럼 흉내내어(Set 백킹) 캐시 적재/무효화 흐름을 검증 가능하게 한다.
            java.util.Set<String> cached = new java.util.HashSet<>();
            when(membershipCache.isCachedMember(any(), any())).thenAnswer(inv ->
                    cached.contains(inv.getArgument(0) + ":" + inv.getArgument(1)));
            org.mockito.Mockito.doAnswer(inv -> {
                cached.add(inv.getArgument(0) + ":" + inv.getArgument(1));
                return null;
            }).when(membershipCache).cacheMember(any(), any());
            org.mockito.Mockito.doAnswer(inv -> {
                cached.remove(inv.getArgument(0) + ":" + inv.getArgument(1));
                return null;
            }).when(membershipCache).invalidate(any(), any());

            java.util.Map<UUID, Channel> channels = new java.util.HashMap<>();
            when(channelRepository.save(any())).thenAnswer(inv -> {
                Channel c = inv.getArgument(0);
                channels.put(c.getId(), c); // Channel 생성자가 이미 id를 할당하므로(클라이언트 측 UUID 생성) null이 될 수 없음
                return c;
            });
            when(channelRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(channels.get(inv.getArgument(0))));

            ChannelHarness harness = new ChannelHarness(new ChannelService(channelRepository, memberRepository, membershipCache));

            when(memberRepository.save(any())).thenAnswer(inv -> {
                harness.members.add(inv.getArgument(0));
                return inv.getArgument(0);
            });
            when(memberRepository.findByChannelId(any())).thenAnswer(inv -> {
                UUID channelId = inv.getArgument(0);
                return harness.members.stream().filter(m -> m.getChannelId().equals(channelId)).toList();
            });
            org.mockito.Mockito.doAnswer(inv -> {
                ChannelMemberId id = inv.getArgument(0);
                harness.members.removeIf(m -> new ChannelMemberId(m.getChannelId(), m.getUserId()).equals(id));
                return null;
            }).when(memberRepository).deleteById(any());

            return harness;
        }
    }
}
