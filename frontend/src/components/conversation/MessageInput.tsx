"use client";

import { useState, type FormEvent } from "react";
import { sendMessage } from "@/lib/stompClient";

interface Props {
  channelId: string;
}

export function MessageInput({ channelId }: Props) {
  const [content, setContent] = useState("");

  function handleSubmit(e: FormEvent): void {
    e.preventDefault();
    const trimmed = content.trim();
    if (trimmed.length === 0) return;
    sendMessage(channelId, trimmed);
    setContent("");
  }

  return (
    <form onSubmit={handleSubmit} className="flex gap-2 border-t p-3" data-testid="message-input-form">
      <input
        type="text"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="메시지를 입력하세요"
        className="flex-1 rounded border px-3 py-2"
        data-testid="message-input-content-input"
      />
      <button
        type="submit"
        disabled={content.trim().length === 0}
        className="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50"
        data-testid="message-input-send-button"
      >
        전송
      </button>
    </form>
  );
}
