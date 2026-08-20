import { apiClient } from "./apiClient";
import type { ChannelMember, User } from "@/types/domain";

/**
 * DIRECT 채널의 `name` 필드는 Backend에서 "dm:{requesterId}:{otherUserId}" 형태의 내부용 식별자로
 * 저장되고(ChannelService.getOrCreateDirectChannel), 화면에 보여줄 문자열이 아니다. 화면에는 항상
 * 상대방(나를 제외한 멤버)의 displayName을 표시해야 하므로, 멤버 목록에서 나를 제외한 사용자를 찾아
 * 프로필까지 조회한다.
 */
export async function resolveDirectChannelPeer(channelId: string, currentUserId: string): Promise<User | null> {
  const { data: members } = await apiClient.get<ChannelMember[]>(`/api/channels/${channelId}/members`);
  const other = members.find((m) => m.userId !== currentUserId);
  if (!other) return null;

  const { data: profiles } = await apiClient.get<User[]>("/api/users", { params: { ids: other.userId } });
  return profiles[0] ?? null;
}
