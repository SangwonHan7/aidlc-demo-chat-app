"use client";

// 아직 활성 채널이 없을 때(최초 진입, 채널 없음 등) MainPanel이 보여주는 안내 화면.
export function EmptyState() {
  return (
    <div
      className="flex h-full flex-col items-center justify-center gap-2 text-gray-400"
      data-testid="empty-state"
    >
      <p className="text-lg font-medium">채널을 선택해주세요</p>
      <p className="text-sm">왼쪽에서 채널이나 DM을 선택하거나, 새 채널을 만들어보세요.</p>
    </div>
  );
}
