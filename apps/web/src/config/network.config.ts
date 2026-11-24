export const NETWORK = {
  requestTimeoutMs: 15_000,
  authUpstreamTimeoutMs: 10_000,
  catalogStaleTimeMs: 5 * 60_000,
  catalogTimeoutMs: 30_000,
  maxCollectedPages: 100,
} as const
