"use client";

import type { Road } from "@/lib/api";

const points: Record<string, string> = {
  "Sector 18 Main Road": "left-[18%] top-[40%]",
  "Noida-Greater Noida Expressway": "left-[70%] top-[64%]",
  "DND Flyway": "left-[38%] top-[22%]",
  "Botanical Garden Road": "left-[28%] top-[58%]",
  "Akshardham Road": "left-[56%] top-[22%]",
  "Kalindi Kunj Road": "left-[46%] top-[74%]"
};

export function TrafficMap({ roads }: { roads: Road[] }) {
  const visibleRoads = roads.length > 0 ? roads : [
    { id: 1, roadName: "Sector 18 Main Road", status: "READY", congestionScore: 28, avgSpeed: 42 } as Road,
    { id: 2, roadName: "DND Flyway", status: "READY", congestionScore: 34, avgSpeed: 48 } as Road,
    { id: 3, roadName: "Akshardham Road", status: "READY", congestionScore: 31, avgSpeed: 44 } as Road
  ];

  return (
    <section className="relative min-h-[430px] overflow-hidden rounded-md border p-4 shadow-[var(--shadow)]" style={{ borderColor: "var(--line)", background: "var(--surface)" }}>
      <div className="absolute inset-0 opacity-90" style={{ background: "radial-gradient(circle at 20% 20%, rgba(32,152,108,.22), transparent 28%), radial-gradient(circle at 84% 18%, rgba(233,183,63,.22), transparent 26%), linear-gradient(135deg, rgba(47,127,193,.12), transparent 45%)" }} />
      <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(255,255,255,.16)_1px,transparent_1px),linear-gradient(rgba(255,255,255,.16)_1px,transparent_1px)] bg-[size:52px_52px]" />

      <div className="absolute left-4 top-4 z-10 rounded border px-3 py-2 text-xs font-medium" style={{ borderColor: "var(--line)", background: "color-mix(in srgb, var(--surface) 88%, transparent)", color: "var(--muted)" }}>
        Noida relief grid
      </div>

      <RoadBand className="inset-x-[-4%] top-[45%] h-8 -rotate-6" />
      <RoadBand className="inset-y-[-8%] left-[47%] w-8 rotate-12" vertical />
      <RoadBand className="left-[10%] top-[67%] h-8 w-[78%] rotate-3" />
      <RoadBand className="left-[25%] top-[18%] h-8 w-[50%] -rotate-2" />

      <div className="absolute left-[47%] top-[45%] h-12 w-12 -translate-x-1/2 -translate-y-1/2 rounded-full border-[10px]" style={{ borderColor: "var(--road)" }} />
      <div className="absolute left-[47%] top-[45%] h-3 w-3 -translate-x-1/2 -translate-y-1/2 rounded-full" style={{ background: "var(--signal)" }} />

      {visibleRoads.map((road) => (
        <div key={road.id} className={`absolute ${points[road.roadName] ?? "left-1/2 top-1/2"} -translate-x-1/2 -translate-y-1/2`}>
          <div className="relative h-6 w-6 rounded-full border-2 border-white shadow-lg" style={{ background: road.congestionScore >= 75 ? "var(--alert)" : road.congestionScore >= 45 ? "var(--signal)" : "var(--flow)" }}>
            <span className="absolute inset-[-7px] rounded-full border opacity-50" style={{ borderColor: road.congestionScore >= 75 ? "var(--alert)" : road.congestionScore >= 45 ? "var(--signal)" : "var(--flow)" }} />
          </div>
          <div className="mt-2 w-40 rounded border px-2 py-1.5 text-xs shadow-lg backdrop-blur" style={{ borderColor: "var(--line)", background: "color-mix(in srgb, var(--surface) 88%, transparent)" }}>
            <div className="font-semibold">{road.roadName}</div>
            <div style={{ color: "var(--muted)" }}>{road.status} | score {road.congestionScore}</div>
          </div>
        </div>
      ))}
    </section>
  );
}

function RoadBand({ className, vertical = false }: { className: string; vertical?: boolean }) {
  return (
    <div className={`absolute ${className} overflow-hidden rounded-full`} style={{ background: "var(--road)" }}>
      <div
        className={vertical ? "absolute inset-y-0 left-1/2 w-[3px] -translate-x-1/2" : "absolute left-0 top-1/2 h-[3px] w-full -translate-y-1/2"}
        style={{ background: "repeating-linear-gradient(90deg, var(--road-line) 0 18px, transparent 18px 34px)" }}
      />
    </div>
  );
}
