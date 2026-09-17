import { config } from '../config.js';
import { openaiProvider } from './providers/openaiProvider.js';
import { geminiProvider } from './providers/geminiProvider.js';

/**
 * Provider abstraction. Adding a new AI vendor means writing one file that
 * exports { name, isConfigured(), solve({ imageBase64, mimeType, language }) }
 * and registering it here. The Android app never changes.
 */
const providers = {
  openai: openaiProvider,
  gemini: geminiProvider
};

export function getProvider(name = config.provider) {
  const provider = providers[name];
  if (!provider) {
    const error = new Error(`Unknown AI provider: ${name}`);
    error.code = 'PROVIDER_NOT_CONFIGURED';
    throw error;
  }
  if (!provider.isConfigured()) {
    const error = new Error(`Missing API key for provider: ${name}`);
    error.code = 'PROVIDER_NOT_CONFIGURED';
    throw error;
  }
  return provider;
}

export function listProviders() {
  return Object.values(providers).map((provider) => ({
    name: provider.name,
    configured: provider.isConfigured()
  }));
}

/**
 * Turns raw model text into the strict contract the Android app expects.
 * Anything unexpected becomes "unclear" - we never invent an answer.
 */
export function parseAiAnswer(rawText) {
  const cleaned = String(rawText)
    .replace(/^\s*```(?:json)?/i, '')
    .replace(/```\s*$/i, '')
    .trim();

  let parsed;
  try {
    parsed = JSON.parse(cleaned);
  } catch {
    const match = cleaned.match(/\{[\s\S]*\}/);
    if (!match) return { status: 'unclear' };
    try {
      parsed = JSON.parse(match[0]);
    } catch {
      return { status: 'unclear' };
    }
  }

  if (!parsed || typeof parsed !== 'object') return { status: 'unclear' };
  if (String(parsed.status).toLowerCase() !== 'ok') return { status: 'unclear' };

  const problem = typeof parsed.problem === 'string' ? parsed.problem.trim() : '';
  const solution = typeof parsed.solution === 'string' ? parsed.solution.trim() : '';
  const steps = Array.isArray(parsed.steps)
    ? parsed.steps
        .filter((step) => typeof step === 'string')
        .map((step) => step.trim())
        .filter(Boolean)
        .slice(0, 40)
    : [];

  const confidence =
    typeof parsed.confidence === 'number' && Number.isFinite(parsed.confidence)
      ? Math.min(Math.max(parsed.confidence, 0), 1)
      : null;

  if (problem === '' || solution === '') return { status: 'unclear' };
  if (confidence !== null && confidence < 0.55) return { status: 'unclear' };

  return { status: 'ok', problem, steps, solution, confidence };
}

export async function solveImage({ imageBase64, mimeType, language }) {
  const provider = getProvider();
  const rawText = await provider.solve({ imageBase64, mimeType, language });
  return parseAiAnswer(rawText);
}
