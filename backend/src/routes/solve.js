import { Router } from 'express';
import { config } from '../config.js';
import { listProviders, solveImage } from '../services/aiService.js';

export const solveRouter = Router();

const ALLOWED_MIME = new Set(['image/jpeg', 'image/jpg', 'image/png', 'image/webp']);
const BASE64_RE = /^[A-Za-z0-9+/=\r\n]+$/;

solveRouter.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    provider: config.provider,
    providers: listProviders()
  });
});

solveRouter.post('/solve', async (req, res, next) => {
  try {
    const { imageBase64, mimeType = 'image/jpeg', language = 'sr' } = req.body || {};

    if (typeof imageBase64 !== 'string' || imageBase64.length < 64) {
      return res.status(422).json({
        status: 'error',
        code: 'INVALID_IMAGE',
        message: 'imageBase64 is missing or too small.'
      });
    }

    const normalized = imageBase64.includes(',')
      ? imageBase64.slice(imageBase64.indexOf(',') + 1)
      : imageBase64;

    if (!BASE64_RE.test(normalized)) {
      return res.status(422).json({
        status: 'error',
        code: 'INVALID_IMAGE',
        message: 'imageBase64 is not valid base64.'
      });
    }

    const approximateBytes = Math.floor((normalized.length * 3) / 4);
    if (approximateBytes > config.maxImageBytes) {
      return res.status(413).json({
        status: 'error',
        code: 'IMAGE_TOO_LARGE',
        message: 'The image is too large.'
      });
    }

    if (!ALLOWED_MIME.has(String(mimeType).toLowerCase())) {
      return res.status(415).json({
        status: 'error',
        code: 'UNSUPPORTED_MEDIA_TYPE',
        message: 'Only JPEG, PNG and WebP images are supported.'
      });
    }

    const result = await solveImage({
      imageBase64: normalized,
      mimeType: String(mimeType).toLowerCase() === 'image/jpg' ? 'image/jpeg' : mimeType,
      language
    });

    if (result.status !== 'ok') {
      // Deliberately no answer: the app shows the "photograph it again" message.
      return res.json({
        status: 'unclear',
        message: 'Problem could not be recognised reliably.'
      });
    }

    return res.json({
      status: 'ok',
      problem: result.problem,
      steps: result.steps,
      solution: result.solution,
      confidence: result.confidence
    });
  } catch (error) {
    return next(error);
  }
});
