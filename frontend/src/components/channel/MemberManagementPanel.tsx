"use client";

import { useEffect, useState } from "react";
import type { AxiosError } from "axios";
import { apiClient } from "@/lib/apiClient";
import { useChatStore } from "@/store/chatStore";
import { useAuthStore } from "@/store/authStore";
import { messageForErrorCode } from "@/lib/errorMessages";
import type { ApiError, ChannelMember, User } from "@/types/domain";

interface Props {
  channelId: string;
  onClose: () => void;
}

type MemberWithProfile = ChannelMember & { profile?: User };

// story 2.2(채널 구성원 초대 및 관리). Backend Post-Approval Patch 3(GET /members, GET /users/search)로
// 발견/보완된 API를 소비한다.
export function MemberManagementPanel({ channelId, onClose }: Props) {
  const inviteMember = useChatStore((s) => s.inviteMember);
  const removeMember = useChatStore((s) => s.removeMember);
  const currentUserId = useAuthStore((s) => s.user?.id);
  const [members, setMembers] = useState<MemberWithProfile[]>([]);
  const [inviteeEmail, setInviteeEmail] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function loadMembers(): Promise<void> {
    const { data: memberList } = await apiClient.get<ChannelMember[]>(`/api/channels/${channelId}/members`);
    if (memberList.length === 0) {
      setMembers([]);
      return;
    }
    const { data: profiles } = await apiClient.get<User[]>("/api/users", {
      params: { ids: memberList.map((m) => m.userId).join(",") },
    });
    const profileById = new Map(profiles.map((p) => [p.id, p]));
    setMembers(memberList.map((m) => ({ ...m, profile: profileById.get(m.userId) })));
  }

  useEffect(() => {
    void loadMembers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [channelId]);

  function describeError(err: unknown, fallback: string): string {
    const apiError = (err as AxiosError<ApiError>).response?.data;
    return apiError ? messageForErrorCode(apiError.errorCode, apiError.message) : fallback;
  }

  async function handleInvite(): Promise<void> {
    setError(null);
    try {
      const { data: matches } = await apiClient.get<User[]>("/api/users/search", {
        params: { email: inviteeEmail.trim() },
      });
      if (matches.length === 0) {
        setError("해당 이메일의 사용자를 찾을 수 없습니다.");
        return;
      }
      await inviteMember(channelId, matches[0].id);
      setInviteeEmail("");
      await loadMembers();
    } catch (err) {
      setError(describeError(err, "초대에 실패했습니다."));
    }
  }

  async function handleRemove(userId: string): Promise<void> {
    setError(null);
    try {
      await removeMember(channelId, userId);
      await loadMembers();
    } catch (err) {
      setError(describeError(err, "제외에 실패했습니다."));
    }
  }

  return (
    <div
      className="fixed inset-0 flex items-center justify-center bg-black/30"
      data-testid="member-management-panel"
    >
      <div className="w-full max-w-md rounded bg-white p-4 shadow">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-medium">채널 멤버 관리</h2>
          <button onClick={onClose} data-testid="member-management-panel-close-button" className="text-gray-500">
            닫기
          </button>
        </div>

        <ul className="mb-3 flex flex-col gap-2" data-testid="member-management-panel-list">
          {members.map((m) => (
            <li
              key={m.userId}
              className="flex items-center justify-between"
              data-testid={`member-management-panel-member-${m.userId}`}
            >
              <span>
                {m.profile?.displayName ?? m.userId}
                {m.role === "OWNER" && " (관리자)"}
              </span>
              {m.userId !== currentUserId && m.role !== "OWNER" && (
                <button
                  onClick={() => handleRemove(m.userId)}
                  data-testid={`member-management-panel-remove-button-${m.userId}`}
                  className="text-sm text-red-600"
                >
                  제외
                </button>
              )}
            </li>
          ))}
        </ul>

        <div className="flex gap-2">
          <input
            value={inviteeEmail}
            onChange={(e) => setInviteeEmail(e.target.value)}
            placeholder="초대할 사용자 이메일"
            className="flex-1 rounded border px-2 py-1 text-sm"
            data-testid="member-management-panel-invite-input"
          />
          <button
            onClick={handleInvite}
            data-testid="member-management-panel-invite-button"
            className="rounded bg-blue-600 px-3 py-1 text-sm text-white"
          >
            초대
          </button>
        </div>
        {error && (
          <p className="mt-2 text-sm text-red-600" data-testid="member-management-panel-error">
            {error}
          </p>
        )}
      </div>
    </div>
  );
}
