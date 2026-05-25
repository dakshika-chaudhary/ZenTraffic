import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#15201d",
        road: "#3d4742",
        signal: "#e6b53e",
        flow: "#2f9e77",
        alert: "#d65345"
      }
    }
  },
  plugins: []
};

export default config;
