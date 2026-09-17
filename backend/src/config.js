import 'dotenv/config';

function int(value, fallback) {
  const parsed = Number.parseInt(value ?? '', 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export const config = {
  port: int(process.env.PORT, 3000),
  provider: (process.env.AI_PROVIDER || 'openai').toLowerCase(),
  maxImageBytes: int(process.env.MAX_IMAGE_MB, 8) * 1024 * 1024,
  aiTimeoutMs: int(process.env.AI_TIMEOUT_MS, 45000),
  rateLimitMax: int(process.env.RATE_LIMIT_MAX, 60),
  corsOrigin: process.env.CORS_ORIGIN || '*',

  openai: {
    apiKey: process.env.OPENAI_API_KEY,
    model: process.env.OPENAI_MODEL || 'gpt-4o-mini',
    baseUrl: process.env.OPENAI_BASE_URL || 'https://api.openai.com/v1'
  },

  gemini: {
    apiKey: process.env.GEMINI_API_KEY,
    model: process.env.GEMINI_MODEL || 'gemini-1.5-flash',
    baseUrl:
      process.env.GEMINI_BASE_URL || 'https://generativelanguage.googleapis.com/v1beta'
  }
};
