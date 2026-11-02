/**
 * The address of the STOMP WebSocket endpoint.
 *
 * VITE_WS_URL is used only when it is a complete address (ws:// or wss://, ending in /ws).
 * A value such as "https://host" without the path would connect to the wrong place and be
 * refused, so it is ignored. Then the address is derived from the REST API address, which
 * serves the WebSocket on the same host: https://host -> wss://host/ws.
 */
export function resolveWebSocketUrl(
  explicitUrl: string | undefined,
  apiUrl: string | undefined,
): string {
  if (explicitUrl && /^wss?:\/\/[^/]+(\/.*)?\/ws$/.test(explicitUrl.trim())) {
    return explicitUrl.trim()
  }

  const base = (apiUrl ?? 'http://localhost:8080').trim().replace(/\/+$/, '')
  return base.replace(/^http/, 'ws') + '/ws'
}
