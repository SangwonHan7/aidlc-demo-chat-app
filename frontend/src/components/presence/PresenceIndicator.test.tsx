import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { PresenceIndicator } from "./PresenceIndicator";
import { usePresenceStore } from "@/store/presenceStore";

describe("PresenceIndicator (example-based)", () => {
  it("renders as offline (gray, 오프라인) when the user id is not in onlineUserIds", () => {
    usePresenceStore.setState({ onlineUserIds: new Set() });
    render(<PresenceIndicator userId="user-1" />);

    const dot = screen.getByTestId("presence-indicator-user-1");
    expect(dot).toHaveAttribute("title", "오프라인");
    expect(dot.className).toContain("bg-gray-300");
  });

  it("renders as online (green, 온라인) when the user id is in onlineUserIds", () => {
    usePresenceStore.setState({ onlineUserIds: new Set(["user-1"]) });
    render(<PresenceIndicator userId="user-1" />);

    const dot = screen.getByTestId("presence-indicator-user-1");
    expect(dot).toHaveAttribute("title", "온라인");
    expect(dot.className).toContain("bg-green-500");
  });
});
