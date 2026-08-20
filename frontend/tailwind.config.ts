import type { Config } from "tailwindcss";

// NFR Requirements(Frontend) Question 5 답변 A: 스타일링은 Tailwind CSS로 결정.
const config: Config = {
  content: ["./src/**/*.{js,ts,jsx,tsx,mdx}"],
  theme: {
    extend: {},
  },
  plugins: [],
};

export default config;
