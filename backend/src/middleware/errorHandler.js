const STATUS_BY_CODE = {
  PROVIDER_NOT_CONFIGURED: 503,
  AI_UNAVAILABLE: 503,
  AI_RATE_LIMITED: 503,
  AI_TIMEOUT: 504,
  INVALID_AI_RESPONSE: 502,
  AI_ERROR: 502
};

const MESSAGE_BY_CODE = {
  PROVIDER_NOT_CONFIGURED: 'AI provider is not configured on the server.',
  AI_UNAVAILABLE: 'AI provider is unreachable.',
  AI_RATE_LIMITED: 'AI provider rate limit reached.',
  AI_TIMEOUT: 'AI request timed out.',
  INVALID_AI_RESPONSE: 'AI returned an unusable response.',
  AI_ERROR: 'AI request failed.'
};

// eslint-disable-next-line no-unused-vars
export function errorHandler(error, req, res, next) {
  const code = error?.code && STATUS_BY_CODE[error.code] ? error.code : 'AI_ERROR';
  const status = STATUS_BY_CODE[code];

  // Full detail in the server log only - never in the client response.
  console.error(`[solve] ${code}: ${error?.message || error}`, error?.details || '');

  res.status(status).json({
    status: 'error',
    code,
    message: MESSAGE_BY_CODE[code]
  });
}

export function notFoundHandler(req, res) {
  res.status(404).json({ status: 'error', code: 'NOT_FOUND', message: 'Unknown endpoint.' });
}
