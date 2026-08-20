import { describe, expect, it } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { MessageInput } from "./MessageInput";

describe("MessageInput (example-based)", () => {
  it("disables the send button while the input is empty or whitespace-only", () => {
    render(<MessageInput channelId="channel-1" />);
    const sendButton = screen.getByTestId("message-input-send-button");

    expect(sendButton).toBeDisabled();

    fireEvent.change(screen.getByTestId("message-input-content-input"), {
      target: { value: "   " },
    });
    expect(sendButton).toBeDisabled();
  });

  it("enables the send button once non-whitespace content is entered, and clears after submit", () => {
    render(<MessageInput channelId="channel-1" />);
    const contentInput = screen.getByTestId("message-input-content-input");
    const sendButton = screen.getByTestId("message-input-send-button");

    fireEvent.change(contentInput, { target: { value: "hello" } });
    expect(sendButton).toBeEnabled();

    fireEvent.click(sendButton);
    expect(contentInput).toHaveValue("");
  });
});
