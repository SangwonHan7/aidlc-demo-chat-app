import { apiClient } from "./apiClient";
import type { ChannelMember, User } from "@/types/domain";

/**
 * 채널 멤버 목록 + 프로필을 한 번에 조회해 userId -> User 맵으로 만든다.
 *
 * 2026-08-22 사용자 요청: "채팅에서 누가 무슨 말을 했는지 구분이 안 된다" - MessageList가 메시지의
 * senderId만 갖고 있고 표시이름을 전혀 조회하지 않아 발신자를 구분할 방법이 없었음(내가 보낸 메시지인지
 * 아닌지만 좌우 정렬로 구분). `GET /api/channels/{id}/members` + `GET /api/users?ids=` 조합은
 * `MemberManagementPanel.tsx`에 이미 있던 것과 같은 패턴을 재사용한다.
 *
 * 알려진 제약: 채널 멤버 목록은 "현재" 멤버만 반환하므로, 이후 채널에서 제외된 사용자가 과거에 보낸
 * 메시지는 프로필을 찾지 못한다(MessageList에서 폴백 문자열로 처리).
 */
export async function fetchMemberProfilesById(channelId: string): Promise<Record<string, User>> {
  const { data: memberList } = await apiClient.get<ChannelMember[]>(`/api/channels/${channelId}/members`);
  if (memberList.length === 0) return {};

  const { data: profiles } = await apiClient.get<User[]>("/api/users", {
    params: { ids: memberList.map((m) => m.userId).join(",") },
  });
  return Object.fromEntries(profiles.map((profile) => [profile.id, profile]));
}
