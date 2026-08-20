import { describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { CreateChannelModal } from "./CreateChannelModal";

describe("CreateChannelModal (example-based)", () => {
  it("reflects the channel name length in the counter as the user types", () => {
    render(<CreateChannelModal onClose={vi.fn()} />);
    const nameInput = screen.getByTestId("create-channel-modal-name-input");

    expect(screen.getByTestId("create-channel-modal-name-counter")).toHaveTextContent("0/50");

    fireEvent.change(nameInput, { target: { value: "general" } });
    expect(screen.getByTestId("create-channel-modal-name-counter")).toHaveTextContent("7/50");
  });

  it("shows a validation error and does not close when the name is blank", () => {
    const onClose = vi.fn();
    render(<CreateChannelModal onClose={onClose} />);

    fireEvent.click(screen.getByTestId("create-channel-modal-submit-button"));

    expect(screen.getByTestId("create-channel-modal-error")).toHaveTextContent("채널 이름을 입력");
    expect(onClose).not.toHaveBeenCalled();
  });

  it("calls onClose when the cancel button is clicked", () => {
    const onClose = vi.fn();
    render(<CreateChannelModal onClose={onClose} />);

    fireEvent.click(screen.getByTestId("create-channel-modal-cancel-button"));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("defaults visibility to PUBLIC and allows switching to INVITE_ONLY", () => {
    render(<CreateChannelModal onClose={vi.fn()} />);

    expect(screen.getByTestId("create-channel-modal-visibility-public")).toBeChecked();
    expect(screen.getByTestId("create-channel-modal-visibility-invite-only")).not.toBeChecked();

    fireEvent.click(screen.getByTestId("create-channel-modal-visibility-invite-only"));
    expect(screen.getByTestId("create-channel-modal-visibility-invite-only")).toBeChecked();
  });
});
