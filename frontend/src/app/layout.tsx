import "./globals.css";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "ZenTraffic",
  description: "Smart urban traffic management dashboard"
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
