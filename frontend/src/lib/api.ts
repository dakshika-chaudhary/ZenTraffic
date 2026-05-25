export type Road = {
  id: number;
  roadName: string;
  city: string;
  currentDensity: number;
  avgSpeed: number;
  status: string;
  congestionScore: number;
};

export type RouteResponse = {
  path: string[];
  distanceKm: number;
  estimatedMinutes: number;
  congestionScore: number;
  timeSavedMinutes: number;
  strategy: string;
};

export type AnalyticsSummary = {
  monitoredRoads: number;
  congestedRoads: number;
  averageSpeed: number;
  hotspots: Road[];
  roadStatusBreakdown: Record<string, number>;
};

export type UserProfile = {
  id: number;
  name: string;
  email: string;
  role: string;
  createdAt: string;
};

export type AuthResponse = {
  token: string;
  refreshToken: string;
  expiresInSeconds: number;
  refreshExpiresInSeconds: number;
  profile: UserProfile;
};

export type RegistrationOtpResponse = {
  message: string;
  email: string;
  expiresInSeconds: number;
  devOtp?: string;
};

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

export async function api<T>(path: string, token?: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {})
    },
    cache: "no-store"
  });
  if (!response.ok) {
    throw new ApiError(response.status, response.statusText);
  }
  return response.json() as Promise<T>;
}

export class ApiError extends Error {
  constructor(public status: number, statusText: string) {
    super(`${status} ${statusText}`);
  }
}
