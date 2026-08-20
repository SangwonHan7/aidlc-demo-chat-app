"use client";

import { useState, type FormEvent } from "react";
import type { AxiosError } from "axios";
import { useChatStore } from "@/store/chatStore";
import { messageForErrorCode } from "@/lib/errorMessages";
import type { ApiError, ChannelVisibility } from "@/types/domain";

interface Props {
  onClose: () => void;
}

const NAME_UX_LIMIT = 50; // Backend는 길이 제한이 없음 - UX 권장치일 뿐 강제 차단하지 않음 (business-rules.md)

export function CreateChannelModal({ onClose }: Props) {
  const createChannel = useChatStore((s) => s.createChannel);
  const setActiveChannel = useChatStore((s) => s.setActiveChannel);
  const [name, setName] = useState("");
  const [visibility, setVisibility] = useState<ChannelVisibility>("PUBLIC");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent): Promise<void> {
    e.preventDefault();
    if (name.trim().length === 0) {
      setError("채널 이름을 입력해주세요.");
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const channel = await createChannel(name.trim(), visibility);
      setActiveChannel(channel.id);
      onClose();
    } catch (err) {
      const apiError = (err as AxiosError<ApiError>).response?.data;
      setError(apiError ? messageForErrorCode(apiError.errorCode, apiError.message) : "채널 생성에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 flex items-center justify-center bg-black/30" data-testid="create-channel-modal">
      <form onSubmit={handleSubmit} className="w-full max-w-sm rounded bg-white p-4 shadow">
        <h2 className="mb-3 font-medium">새 채널 만들기</h2>

        <label className="mb-1 flex flex-col gap-1">
          <span className="text-sm text-gray-600">채널 이름</span>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="rounded border px-2 py-1"
            data-testid="create-channel-modal-name-input"
          />
          <span className="text-xs text-gray-500" data-testid="create-channel-modal-name-counter">
            {name.length}/{NAME_UX_LIMIT}
          </span>
        </label>

        <fieldset className="mb-3 flex gap-4">
          <label className="flex items-center gap-1 text-sm">
            <input
              type="radio"
              checked={visibility === "PUBLIC"}
              onChange={() => setVisibility("PUBLIC")}
              data-testid="create-channel-modal-visibility-public"
            />
            공개
          </label>
          <label className="flex items-center gap-1 text-sm">
            <input
              type="radio"
              checked={visibility === "INVITE_ONLY"}
              onChange={() => setVisibility("INVITE_ONLY")}
              data-testid="create-channel-modal-visibility-invite-only"
            />
            초대 전용
          </label>
        </fieldset>

        {error && (
          <p className="mb-2 text-sm text-red-600" data-testid="create-channel-modal-error">
            {error}
          </p>
        )}

        <div className="flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="px-3 py-1 text-sm text-gray-600"
            data-testid="create-channel-modal-cancel-button"
          >
            취소
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="rounded bg-blue-600 px-3 py-1 text-sm text-white disabled:opacity-50"
            data-testid="create-channel-modal-submit-button"
          >
            생성
          </button>
        </div>
      </form>
    </div>
  );
}
