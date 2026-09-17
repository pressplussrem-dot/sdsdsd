import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import rateLimit from 'express-rate-limit';

import { config } from './config.js';
import { solveRouter } from './routes/solve.js';
import { errorHandler, notFoundHandler } from './middleware/errorHandler.js';
import { listProviders } from './services/aiService.js';

const app = express();

app.disable('x-powered-by');
app.use(helmet());
app.use(
  cors({
    origin: config.corsOrigin === '*' ? true : config.corsOrigin.split(',').map((o) => o.trim())
  })
);
app.use(morgan('tiny'));

// Images arrive as base64 JSON, so the body limit has to be generous.
app.use(express.json({ limit: `${Math.ceil(config.maxImageBytes / (1024 * 1024)) + 4}mb` }));

app.use(
  '/api',
  rateLimit({
    windowMs: 15 * 60 * 1000,
    max: config.rateLimitMax,
    standardHeaders: true,
    legacyHeaders: false,
    message: { status: 'error', code: 'RATE_LIMITED', message: 'Too many requests.' }
  })
);

app.use('/api', solveRouter);

// Malformed JSON should not look like a crash.
app.use((error, req, res, next) => {
  if (error instanceof SyntaxError && 'body' in error) {
    return res
      .status(400)
      .json({ status: 'error', code: 'BAD_REQUEST', message: 'Malformed JSON body.' });
  }
  if (error?.type === 'entity.too.large') {
    return res
      .status(413)
      .json({ status: 'error', code: 'IMAGE_TOO_LARGE', message: 'The image is too large.' });
  }
  return next(error);
});

app.use(notFoundHandler);
app.use(errorHandler);

const server = app.listen(config.port, () => {
  const configured = listProviders()
    .map((p) => `${p.name}:${p.configured ? 'configured' : 'missing key'}`)
    .join(', ');
  console.log(`SmartCalc AI backend listening on port ${config.port}`);
  console.log(`Active provider: ${config.provider} (${configured})`);
});

// Never let an unexpected error take the process down silently.
process.on('unhandledRejection', (reason) => {
  console.error('Unhandled rejection:', reason);
});
process.on('SIGTERM', () => server.close(() => process.exit(0)));

export default app;
