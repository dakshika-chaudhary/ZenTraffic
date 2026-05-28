"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { Activity, Bell, CheckCircle2, Clock3, Gauge, LogIn, LogOut, MapPinned, Moon, Navigation, RadioTower, Route as RouteIcon, ShieldCheck, Sparkles, Sun, TrafficCone, UserPlus, X, Zap } from "lucide-react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { AnalyticsSummary, ApiError, AuthResponse, RegistrationOtpResponse, Road, RouteResponse, UserProfile, api } from "@/lib/api";
import { TrafficMap } from "@/components/TrafficMap";

type View = "driver" | "police" | "admin";
type AuthMode = "login" | "register" | "otp";
type Theme = "light" | "dark";
type AuthSession = { token: string; refreshToken: string; profile: UserProfile };

const SESSION_KEY = "zentraffic.session";
const THEME_KEY = "zentraffic.theme";
const locations = ["Sector 18", "Botanical Garden", "DND Flyway", "Akshardham", "Kalindi Kunj", "Greater Noida"];

export default function Home() {
  const [view, setView] = useState<View>("driver");
  const [theme, setTheme] = useState<Theme>("light");
  const [session, setSession] = useState<AuthSession | null>(null);
  const [authOpen, setAuthOpen] = useState(false);
  const [authMode, setAuthMode] = useState<AuthMode>("login");
  const [authMessage, setAuthMessage] = useState("");
  const [roads, setRoads] = useState<Road[]>([]);
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null);
  const [route, setRoute] = useState<RouteResponse | null>(null);
  const [routeResultOpen, setRouteResultOpen] = useState(false);
  const [alerts, setAlerts] = useState<string[]>([]);
  const [source, setSource] = useState("Sector 18");
  const [destination, setDestination] = useState("Akshardham");

  const token = session?.token ?? "";
  const authReady = token.length > 0;
  const activeRoads = useMemo(() => roads.filter((road) => road.congestionScore >= 45), [roads]);
  const reliefScore = Math.max(0, 100 - (summary?.congestedRoads ?? activeRoads.length) * 12);

  useEffect(() => {
    const savedTheme = window.localStorage.getItem(THEME_KEY) as Theme | null;
    if (savedTheme === "light" || savedTheme === "dark") setTheme(savedTheme);
    const saved = window.localStorage.getItem(SESSION_KEY);
    if (!saved) return;
    try {
      const stored = JSON.parse(saved) as AuthSession;
      setSession(stored);
      void refreshSession(stored.refreshToken);
    } catch {
      window.localStorage.removeItem(SESSION_KEY);
    }
  }, []);

  useEffect(() => window.localStorage.setItem(THEME_KEY, theme), [theme]);

  useEffect(() => {
    if (!session?.refreshToken) return;
    const interval = window.setInterval(() => void refreshSession(session.refreshToken), 12 * 60 * 60 * 1000);
    return () => window.clearInterval(interval);
  }, [session?.refreshToken]);

  useEffect(() => {
    if (!authReady) return;
    void refresh();
    const interval = window.setInterval(() => void refresh(), 10000);
    return () => window.clearInterval(interval);
  }, [authReady, token]);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS("http://localhost:8085/ws/traffic"),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe("/topic/alerts", (message) => setAlerts((items) => [message.body, ...items].slice(0, 8)));
        client.subscribe("/topic/congestion", (message) => setAlerts((items) => [message.body, ...items].slice(0, 8)));
        client.subscribe("/topic/traffic", () => { if (token) void refresh(); });
      }
    });
    client.activate();
    return () => { void client.deactivate(); };
  }, [token]);

  function saveSession(response: AuthResponse) {
    const next = { token: response.token, refreshToken: response.refreshToken, profile: response.profile };
    setSession(next);
    window.localStorage.setItem(SESSION_KEY, JSON.stringify(next));
    setAuthOpen(false);
    setAuthMessage("");
  }

  async function refreshSession(refreshToken: string) {
    try {
      saveSession(await api<AuthResponse>("/auth/refresh", undefined, { method: "POST", body: JSON.stringify({ refreshToken }) }));
    } catch {
      logout();
    }
  }

  function logout() {
    if (session?.refreshToken) void api("/auth/logout", undefined, { method: "POST", body: JSON.stringify({ refreshToken: session.refreshToken }) }).catch(() => undefined);
    setSession(null);
    setRoads([]);
    setSummary(null);
    setRoute(null);
    setRouteResultOpen(false);
    window.localStorage.removeItem(SESSION_KEY);
  }

  async function withAuth(action: () => Promise<void>) {
    if (!session?.token) {
      setAuthMode("login");
      setAuthMessage("Sign in to unlock live routes, incident reporting, and relief analytics.");
      setAuthOpen(true);
      return;
    }
    try {
      await action();
    } catch (error) {
      if (error instanceof ApiError && error.status === 401 && session.refreshToken) {
        await refreshSession(session.refreshToken);
        setAuthOpen(true);
        setAuthMessage("Session refreshed. Run the action again.");
        return;
      }
      throw error;
    }
  }

  async function refresh() {
    if (!token) return;
    const [live, analytics] = await Promise.all([api<Road[]>("/traffic/live", token), api<AnalyticsSummary>("/analytics/summary", token)]);
    setRoads(live);
    setSummary(analytics);
  }

  async function calculateRoute() {
    await withAuth(async () => {
      const nextRoute = await api<RouteResponse>("/route/calculate", token, { method: "POST", body: JSON.stringify({ source, destination, strategy: "DIJKSTRA", emergencyVehicle: view === "police" }) });
      setRoute(nextRoute);
      setRouteResultOpen(true);
    });
  }

  async function reportAccident() {
    await withAuth(async () => {
      await api("/traffic/report", token, { method: "POST", body: JSON.stringify({ userId: session?.profile.id ?? 1, roadId: 3, reportType: "ACCIDENT", severity: 4, description: "Driver reported possible accident", vehicleCount: 88, observedSpeed: 12 }) });
      await refresh();
    });
  }

  return (
    <main className={`${theme} min-h-screen`} style={{ background: "var(--bg)", color: "var(--ink)" }}>
      <div className="min-h-screen" style={{ background: "linear-gradient(135deg, color-mix(in srgb, var(--flow) 12%, transparent), transparent 28%), linear-gradient(45deg, transparent 66%, color-mix(in srgb, var(--signal) 12%, transparent))" }}>
        <Header view={view} setView={setView} theme={theme} setTheme={setTheme} session={session} logout={logout} openLogin={() => { setAuthMode("login"); setAuthOpen(true); }} />

        <div className="mx-auto max-w-7xl px-5 pb-8 pt-5">
          <section className="mb-5 grid gap-4 lg:grid-cols-[1fr_320px]">
            <div className="overflow-hidden rounded-md border p-5 shadow-[var(--shadow)]" style={cardStyle}>
              <div className="mb-4 flex flex-wrap items-center gap-2">
                <Badge tone="flow">Relief mode</Badge><Badge tone="signal">Kafka live</Badge><Badge tone="info">15-day secure session</Badge>
              </div>
              <h2 className="max-w-3xl text-3xl font-semibold leading-tight md:text-4xl">Move traffic from pressure to flow.</h2>
              <p className="mt-3 max-w-2xl text-sm leading-6" style={{ color: "var(--muted)" }}>Sign in once, verify with OTP, and use the dashboard to spot congestion, report incidents, and calculate calmer routes across the city grid.</p>
            </div>
            <div className="rounded-md border p-5 shadow-[var(--shadow)]" style={cardStyle}>
              <div className="mb-2 flex items-center justify-between"><span className="text-sm font-medium" style={{ color: "var(--muted)" }}>Relief score</span><Zap size={18} style={{ color: "var(--signal)" }} /></div>
              <div className="text-5xl font-semibold">{reliefScore}</div>
              <div className="mt-3 h-2 rounded-full" style={{ background: "var(--line)" }}><div className="h-2 rounded-full" style={{ width: `${reliefScore}%`, background: "linear-gradient(90deg, var(--flow), var(--signal))" }} /></div>
              <p className="mt-3 text-sm" style={{ color: "var(--muted)" }}>Higher means fewer active congestion points in the current view.</p>
            </div>
          </section>

          <div className="grid gap-5 lg:grid-cols-[330px_1fr]">
            <aside className="space-y-4">
              <ControlCard title="Account" icon={<ShieldCheck size={18} />}>
                {session ? (
                  <div className="space-y-2 text-sm" style={{ color: "var(--muted)" }}><p className="text-base font-semibold" style={{ color: "var(--ink)" }}>{session.profile.name}</p><p>{session.profile.email}</p><Badge tone="flow">{session.profile.role.toLowerCase()} access</Badge></div>
                ) : (
                  <div className="space-y-3"><p className="text-sm leading-6" style={{ color: "var(--muted)" }}>Live services are gated. The login popup appears automatically when you try a protected action.</p><button onClick={() => { setAuthMode("register"); setAuthOpen(true); }} className="flex w-full items-center justify-center gap-2 rounded border px-3 py-2 font-medium" style={{ borderColor: "var(--line-strong)", background: "var(--surface-strong)" }}><UserPlus size={18} /> Create account</button></div>
                )}
              </ControlCard>
              <ControlCard title="Route Relief" icon={<MapPinned size={18} />}><SelectField value={source} onChange={setSource} /><SelectField value={destination} onChange={setDestination} /><button onClick={calculateRoute} className="mt-2 flex w-full items-center justify-center gap-2 rounded px-3 py-2 font-medium text-white" style={{ background: "var(--flow)" }}><Navigation size={18} /> Calculate calmer route</button></ControlCard>
              <ControlCard title="Incident Alerts" icon={<Bell size={18} />}><button onClick={reportAccident} className="mb-3 w-full rounded px-3 py-2 font-medium text-white" style={{ background: "var(--alert)" }}>Report Accident</button><div className="space-y-2 text-sm">{alerts.length === 0 ? <p style={{ color: "var(--muted)" }}>No realtime alerts yet. New Kafka/WebSocket events will appear here.</p> : alerts.map((alert, index) => <p key={index} className="rounded border p-2" style={{ borderColor: "var(--line)", background: "color-mix(in srgb, var(--signal) 15%, var(--surface))" }}>{alert}</p>)}</div></ControlCard>
            </aside>

            <section className="space-y-5">
              <div className="grid gap-3 md:grid-cols-4">
                <Metric icon={<RadioTower size={18} />} label="Roads" value={summary?.monitoredRoads ?? roads.length} helper="monitored now" />
                <Metric icon={<Gauge size={18} />} label="Congested" value={summary?.congestedRoads ?? activeRoads.length} helper="needs relief" />
                <Metric icon={<Activity size={18} />} label="Avg Speed" value={`${summary?.averageSpeed ?? 0} km/h`} helper="network pace" />
                <Metric icon={<ShieldCheck size={18} />} label="Mode" value={view === "police" ? "Emergency" : view} helper="current lens" />
              </div>
              <TrafficMap roads={roads} />
              {route && <RouteResultHighlight route={route} source={source} destination={destination} onOpen={() => setRouteResultOpen(true)} />}
              <section className="grid gap-5 lg:grid-cols-2">
                <Panel title={view === "driver" ? "Driver Assistance" : view === "police" ? "Police Dashboard" : "Admin Analytics"}>{(summary?.hotspots ?? roads).slice(0, 5).map((road) => <RoadRow key={road.id} road={road} />)}{roads.length === 0 && <p className="text-sm" style={{ color: "var(--muted)" }}>Sign in to load live roads and hotspot rankings.</p>}</Panel>
                <Panel title="Relief Rules"><div className="space-y-3 text-sm leading-6" style={{ color: "var(--muted)" }}><Rule color="var(--flow)" text="Healthy roads stay green when density is controlled and speeds remain steady." /><Rule color="var(--signal)" text="Scores above 45 trigger signal timing suggestions and route penalties." /><Rule color="var(--alert)" text="Accidents and heavy congestion publish Kafka events for instant dashboard alerts." /></div></Panel>
              </section>
            </section>
          </div>
        </div>
        {authOpen && <AuthModal mode={authMode} setMode={setAuthMode} message={authMessage} setMessage={setAuthMessage} onClose={() => setAuthOpen(false)} onLogin={saveSession} />}
        {routeResultOpen && route && <RouteResultModal route={route} source={source} destination={destination} onClose={() => setRouteResultOpen(false)} />}
      </div>
    </main>
  );
}

const cardStyle = { borderColor: "var(--line)", background: "var(--surface)" };

function Header({ view, setView, theme, setTheme, session, logout, openLogin }: { view: View; setView: (view: View) => void; theme: Theme; setTheme: (theme: Theme) => void; session: AuthSession | null; logout: () => void; openLogin: () => void }) {
  return (
    <header className="sticky top-0 z-30 border-b backdrop-blur-xl" style={{ borderColor: "var(--line)", background: "color-mix(in srgb, var(--surface) 86%, transparent)" }}>
      <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-5 py-4">
        <div className="flex items-center gap-3"><div className="flex h-11 w-11 items-center justify-center rounded-md" style={{ background: "var(--road)", color: "var(--signal)" }}><TrafficCone size={22} /></div><div><h1 className="text-2xl font-semibold tracking-normal">ZenTraffic</h1><p className="text-sm" style={{ color: "var(--muted)" }}>Traffic relief, live routing, and incident intelligence</p></div></div>
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-1 rounded-md border p-1" style={{ borderColor: "var(--line)", background: "var(--surface-strong)" }}>{(["driver", "police", "admin"] as View[]).map((item) => <button key={item} onClick={() => setView(item)} className="rounded px-3 py-2 text-sm capitalize" style={view === item ? { background: "var(--ink)", color: "var(--surface)" } : { color: "var(--muted)" }}>{item}</button>)}</div>
          <button onClick={() => setTheme(theme === "light" ? "dark" : "light")} className="flex h-10 w-10 items-center justify-center rounded border" style={{ borderColor: "var(--line)", background: "var(--surface)", color: "var(--ink)" }} title="Toggle theme">{theme === "light" ? <Moon size={18} /> : <Sun size={18} />}</button>
          {session ? <div className="flex items-center gap-2"><span className="hidden text-sm sm:inline" style={{ color: "var(--muted)" }}>{session.profile.name}</span><button onClick={logout} className="flex h-10 w-10 items-center justify-center rounded border" style={{ borderColor: "var(--line)", background: "var(--surface)" }} title="Sign out"><LogOut size={18} /></button></div> : <button onClick={openLogin} className="flex items-center gap-2 rounded px-3 py-2 text-sm font-medium" style={{ background: "var(--ink)", color: "var(--surface)" }}><LogIn size={17} /> Sign in</button>}
        </div>
      </div>
    </header>
  );
}

function AuthModal({ mode, setMode, message, setMessage, onClose, onLogin }: { mode: AuthMode; setMode: (mode: AuthMode) => void; message: string; setMessage: (message: string) => void; onClose: () => void; onLogin: (response: AuthResponse) => void }) {
  const [name, setName] = useState(""); const [email, setEmail] = useState(""); const [password, setPassword] = useState(""); const [role, setRole] = useState<View>("driver"); const [otp, setOtp] = useState(""); const [devOtp, setDevOtp] = useState(""); const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setMessage("");
    try {
      if (mode === "login") onLogin(await api<AuthResponse>("/auth/login", undefined, { method: "POST", body: JSON.stringify({ email, password }) }));
      else if (mode === "register") { const response = await api<RegistrationOtpResponse>("/auth/registration/otp", undefined, { method: "POST", body: JSON.stringify({ name, email, password, role }) }); setDevOtp(response.devOtp ?? ""); setMode("otp"); setMessage("Enter the OTP to finish registration."); }
      else { await api<AuthResponse>("/auth/registration/verify", undefined, { method: "POST", body: JSON.stringify({ email, otp }) }); setMode("login"); setPassword(""); setOtp(""); setDevOtp(""); setMessage("Registration complete. Sign in to start a 15-day session."); }
    } catch (error) { setMessage(error instanceof Error ? error.message : "Authentication failed"); } finally { setBusy(false); }
  }
  return <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 px-4 backdrop-blur-sm"><div className="w-full max-w-md rounded-md border p-5 shadow-[var(--shadow)]" style={cardStyle}><div className="mb-4 flex items-start justify-between gap-4"><div><h2 className="text-xl font-semibold">{mode === "login" ? "Sign in" : mode === "register" ? "Create account" : "Verify OTP"}</h2><p className="mt-1 text-sm" style={{ color: "var(--muted)" }}>{mode === "login" ? "Your refresh session stays valid for 15 days." : mode === "register" ? "Create an account and verify it with OTP." : "Use the generated OTP to activate the account."}</p></div><button onClick={onClose} className="rounded px-2 py-1 text-xl leading-none" style={{ color: "var(--muted)" }} aria-label="Close">x</button></div><form onSubmit={submit} className="space-y-3">{mode === "register" && <Input value={name} setValue={setName} placeholder="Full name" /> }<Input value={email} setValue={setEmail} placeholder="Email" type="email" disabled={mode === "otp"} />{mode !== "otp" && <Input value={password} setValue={setPassword} placeholder="Password" type="password" />}{mode === "register" && <select value={role} onChange={(event) => setRole(event.target.value as View)} className="w-full rounded border p-2" style={{ borderColor: "var(--line)" }}><option value="driver">Driver</option><option value="police">Police</option><option value="admin">Admin</option></select>}{mode === "otp" && <Input value={otp} setValue={setOtp} placeholder="OTP" className="tracking-[0.3em]" />}{devOtp && <p className="rounded border p-2 text-sm" style={{ borderColor: "color-mix(in srgb, var(--flow) 38%, transparent)", background: "color-mix(in srgb, var(--flow) 12%, var(--surface))" }}>Development OTP: <span className="font-semibold">{devOtp}</span></p>}{message && <p className="rounded border p-2 text-sm" style={{ borderColor: "var(--line)", background: "var(--surface-strong)", color: "var(--muted)" }}>{message}</p>}<button disabled={busy} className="flex w-full items-center justify-center gap-2 rounded px-3 py-2 font-medium disabled:opacity-60" style={{ background: "var(--ink)", color: "var(--surface)" }}>{mode === "login" ? <LogIn size={18} /> : <UserPlus size={18} />}{busy ? "Please wait" : mode === "login" ? "Sign in" : mode === "register" ? "Send OTP" : "Verify registration"}</button></form><div className="mt-4 flex justify-center gap-2 text-sm"><button onClick={() => { setMode(mode === "login" ? "register" : "login"); setMessage(""); }} className="font-medium" style={{ color: "var(--flow)" }}>{mode === "login" ? "Create an account" : "Back to sign in"}</button></div></div></div>;
}

function RouteResultHighlight({ route, source, destination, onOpen }: { route: RouteResponse; source: string; destination: string; onOpen: () => void }) {
  const tone = getCongestionTone(route.congestionScore);
  return (
    <section className="result-highlight relative overflow-hidden rounded-md border p-4 shadow-[var(--shadow)]" style={{ borderColor: tone.color, background: "linear-gradient(135deg, color-mix(in srgb, var(--flow) 16%, var(--surface)), var(--surface) 46%, color-mix(in srgb, var(--signal) 16%, var(--surface)))" }}>
      <div className="result-shimmer" />
      <div className="relative z-10 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <div className="mb-2 inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-semibold uppercase" style={{ borderColor: tone.color, color: tone.color, background: "color-mix(in srgb, var(--surface) 78%, transparent)" }}>
            <Sparkles size={14} /> Final route result
          </div>
          <h2 className="text-2xl font-semibold leading-tight">Best route from {source} to {destination}</h2>
          <p className="mt-2 text-sm" style={{ color: "var(--muted)" }}>{tone.summary}</p>
        </div>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:min-w-[520px]">
          <ResultStat icon={<RouteIcon size={17} />} label="Distance" value={`${route.distanceKm} km`} />
          <ResultStat icon={<Clock3 size={17} />} label="ETA" value={`${route.estimatedMinutes} min`} />
          <ResultStat icon={<Gauge size={17} />} label="Pressure" value={tone.label} color={tone.color} />
          <ResultStat icon={<Zap size={17} />} label="Saved" value={`${route.timeSavedMinutes} min`} />
        </div>
      </div>
      <div className="relative z-10 mt-4 flex flex-col gap-3 rounded border p-3 md:flex-row md:items-center md:justify-between" style={{ borderColor: "var(--line)", background: "color-mix(in srgb, var(--surface) 86%, transparent)" }}>
        <RoutePath path={route.path} />
        <button onClick={onOpen} className="inline-flex shrink-0 items-center justify-center gap-2 rounded px-3 py-2 text-sm font-semibold text-white" style={{ background: "var(--ink)" }}>
          <Sparkles size={16} /> View popup
        </button>
      </div>
    </section>
  );
}

function RouteResultModal({ route, source, destination, onClose }: { route: RouteResponse; source: string; destination: string; onClose: () => void }) {
  const tone = getCongestionTone(route.congestionScore);
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-4 py-6 backdrop-blur-md" role="dialog" aria-modal="true" aria-labelledby="route-result-title">
      <div className="result-modal w-full max-w-3xl overflow-hidden rounded-md border shadow-[0_28px_90px_rgba(0,0,0,0.38)]" style={{ borderColor: tone.color, background: "var(--surface)" }}>
        <div className="relative overflow-hidden p-5 text-white" style={{ background: "linear-gradient(135deg, #12342b, #1f6f55 54%, #d59d2b)" }}>
          <div className="absolute inset-0 opacity-25" style={{ background: "linear-gradient(90deg, transparent, rgba(255,255,255,.32), transparent)" }} />
          <div className="relative z-10 flex items-start justify-between gap-4">
            <div>
              <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-white/35 bg-white/15 px-3 py-1 text-xs font-semibold uppercase">
                <CheckCircle2 size={15} /> Route calculated
              </div>
              <h2 id="route-result-title" className="text-3xl font-semibold leading-tight">Your best route is ready</h2>
              <p className="mt-2 text-sm text-white/82">{source} to {destination} with {route.timeSavedMinutes} minutes saved.</p>
            </div>
            <button onClick={onClose} className="flex h-10 w-10 shrink-0 items-center justify-center rounded border border-white/35 bg-white/15 text-white" aria-label="Close route result">
              <X size={18} />
            </button>
          </div>
        </div>

        <div className="p-5">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <ResultStat icon={<RouteIcon size={18} />} label="Distance" value={`${route.distanceKm} km`} />
            <ResultStat icon={<Clock3 size={18} />} label="Estimated time" value={`${route.estimatedMinutes} min`} />
            <ResultStat icon={<Gauge size={18} />} label="Traffic pressure" value={tone.label} color={tone.color} />
            <ResultStat icon={<Zap size={18} />} label="Time saved" value={`${route.timeSavedMinutes} min`} />
          </div>

          <div className="mt-5 rounded border p-4" style={{ borderColor: "var(--line)", background: "var(--surface-strong)" }}>
            <div className="mb-3 flex items-center justify-between gap-3">
              <h3 className="font-semibold">Recommended path</h3>
              <span className="rounded-full px-2.5 py-1 text-xs font-semibold" style={{ background: `color-mix(in srgb, ${tone.color} 14%, transparent)`, color: tone.color }}>{route.strategy}</span>
            </div>
            <RoutePath path={route.path} prominent />
          </div>

          <div className="mt-5 rounded border p-4 text-sm leading-6" style={{ borderColor: tone.color, background: `color-mix(in srgb, ${tone.color} 10%, var(--surface))`, color: "var(--muted)" }}>
            <span className="font-semibold" style={{ color: "var(--ink)" }}>{tone.label}:</span> {tone.summary}
          </div>

          <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:justify-end">
            <button onClick={onClose} className="inline-flex items-center justify-center gap-2 rounded px-4 py-2 font-semibold text-white" style={{ background: "var(--flow)" }}>
              <CheckCircle2 size={18} /> Use this route
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function ResultStat({ icon, label, value, color }: { icon: React.ReactNode; label: string; value: React.ReactNode; color?: string }) {
  return (
    <div className="rounded border p-3" style={{ borderColor: "var(--line)", background: "color-mix(in srgb, var(--surface) 88%, transparent)" }}>
      <div className="flex items-center gap-2 text-xs font-medium uppercase" style={{ color: "var(--muted)" }}>{icon}{label}</div>
      <div className="mt-2 text-lg font-semibold" style={{ color: color ?? "var(--ink)" }}>{value}</div>
    </div>
  );
}

function RoutePath({ path, prominent = false }: { path: string[]; prominent?: boolean }) {
  return (
    <div className={`flex flex-wrap items-center gap-2 ${prominent ? "text-sm" : "text-xs"}`}>
      {path.map((stop, index) => (
        <span key={`${stop}-${index}`} className="flex items-center gap-2">
          <span className="route-step rounded-full border px-3 py-1 font-semibold" style={{ borderColor: "var(--line-strong)", background: "var(--surface)", color: "var(--ink)" }}>{stop}</span>
          {index < path.length - 1 && <span style={{ color: "var(--muted)" }}>to</span>}
        </span>
      ))}
    </div>
  );
}

function getCongestionTone(score: number) {
  if (score >= 75) return { label: "Heavy", color: "var(--alert)", summary: "This is still the strongest available route, but traffic pressure is high. Use it with caution and expect slower movement." };
  if (score >= 45) return { label: "Moderate", color: "var(--signal)", summary: "This route balances distance and congestion, avoiding the worst pressure points while keeping travel time controlled." };
  return { label: "Smooth", color: "var(--flow)", summary: "This route has low traffic pressure and should give the calmest trip in the current city conditions." };
}

function Input({ value, setValue, placeholder, type = "text", disabled, className = "" }: { value: string; setValue: (value: string) => void; placeholder: string; type?: string; disabled?: boolean; className?: string }) { return <input value={value} onChange={(event) => setValue(event.target.value)} className={`w-full rounded border p-2 ${className}`} style={{ borderColor: "var(--line)" }} placeholder={placeholder} type={type} required disabled={disabled} />; }
function SelectField({ value, onChange }: { value: string; onChange: (value: string) => void }) { return <select value={value} onChange={(event) => onChange(event.target.value)} className="mb-2 w-full rounded border p-2" style={{ borderColor: "var(--line)" }}>{locations.map((item) => <option key={item}>{item}</option>)}</select>; }
function ControlCard({ title, icon, children }: { title: string; icon: React.ReactNode; children: React.ReactNode }) { return <section className="rounded-md border p-4 shadow-[var(--shadow)]" style={cardStyle}><div className="mb-3 flex items-center gap-2 font-semibold">{icon}{title}</div>{children}</section>; }
function Metric({ icon, label, value, helper }: { icon?: React.ReactNode; label: string; value: React.ReactNode; helper?: string }) { return <div className="rounded-md border p-3 shadow-[var(--shadow)]" style={cardStyle}><div className="flex items-center gap-2 text-sm" style={{ color: "var(--muted)" }}>{icon}{label}</div><div className="mt-1 text-xl font-semibold">{value}</div>{helper && <div className="mt-1 text-xs" style={{ color: "var(--muted)" }}>{helper}</div>}</div>; }
function Panel({ title, children }: { title: string; children: React.ReactNode }) { return <section className="rounded-md border p-4 shadow-[var(--shadow)]" style={cardStyle}><h2 className="mb-3 text-lg font-semibold">{title}</h2>{children}</section>; }
function Badge({ children, tone }: { children: React.ReactNode; tone: "flow" | "signal" | "alert" | "info" }) { const color = tone === "flow" ? "var(--flow)" : tone === "signal" ? "var(--signal)" : tone === "alert" ? "var(--alert)" : "var(--info)"; return <span className="inline-flex rounded-full border px-2.5 py-1 text-xs font-medium" style={{ borderColor: color, color, background: `color-mix(in srgb, ${color} 12%, transparent)` }}>{children}</span>; }
function Rule({ color, text }: { color: string; text: string }) { return <div className="flex gap-3"><span className="mt-2 h-2.5 w-2.5 shrink-0 rounded-full" style={{ background: color }} /><p>{text}</p></div>; }
function RoadRow({ road }: { road: Road }) { return <div className="flex items-center justify-between border-b py-3 last:border-0" style={{ borderColor: "var(--line)" }}><div><div className="font-medium">{road.roadName}</div><div className="text-sm" style={{ color: "var(--muted)" }}>{road.status} | {road.avgSpeed} km/h</div></div><span className="rounded px-2 py-1 text-sm font-medium" style={{ background: road.congestionScore >= 75 ? "var(--alert)" : road.congestionScore >= 45 ? "var(--signal)" : "var(--flow)", color: road.congestionScore >= 45 ? "#151515" : "#fff" }}>{road.congestionScore}</span></div>; }
