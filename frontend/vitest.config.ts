import path from "path";
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

// NFR Requirements(Frontend) Question 2 답변 A: fast-check(PBT) + Vitest 조합.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    globals: true,
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
