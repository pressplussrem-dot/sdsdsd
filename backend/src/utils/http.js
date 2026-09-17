import { config } from '../config.js';

/**
 * fetch + timeout + uniform error codes, shared by all providers.
 * Errors carry a .code the route handler maps onto the JSON response.
 */
export async function fetchJson(url, options) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), config.aiTimeoutMs);

  let response;
  try {
    response = await fetch(url, { ...options, signal: controller.signal });
  } catch (cause) {
    const error = new Error(
      cause?.name === 'AbortError' ? 'AI request timed out' : 'AI provider unreachable'
    );
    error.code = cause?.name === 'AbortError' ? 'AI_TIMEOUT' : 'AI_UNAVAILABLE';
    throw error;
  } finally {
    clearTimeout(timer);
  }

  const raw = await response.text();

  if (!response.ok) {
    const error = new Error(`AI provider responded with ${response.status}`);
    error.code = response.status === 429 ? 'AI_RATE_LIMITED' : 'AI_ERROR';
    error.status = response.status;
    // Never echo the provider payload back to the client: it can contain
    // account details. Log it server side only.
    error.details = raw.slice(0, 500);
    throw error;
  }

  try {
    return JSON.parse(raw);
  } catch (cause) {
    const error = new Error('AI provider returned invalid JSON');
    error.code = 'INVALID_AI_RESPONSE';
    throw error;
  }
}
