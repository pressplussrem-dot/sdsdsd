import test from 'node:test';
import assert from 'node:assert/strict';
import { parseAiAnswer } from './aiService.js';

test('parses a well formed answer', () => {
  const result = parseAiAnswer(
    JSON.stringify({
      status: 'ok',
      problem: '2x + 5 = 15',
      steps: ['2x = 15 - 5', '2x = 10', 'x = 5'],
      solution: 'x = 5',
      confidence: 0.95
    })
  );
  assert.equal(result.status, 'ok');
  assert.equal(result.solution, 'x = 5');
  assert.equal(result.steps.length, 3);
});

test('strips markdown code fences', () => {
  const raw = '```json\n{"status":"ok","problem":"1+1","steps":[],"solution":"2","confidence":0.9}\n```';
  assert.equal(parseAiAnswer(raw).status, 'ok');
});

test('low confidence becomes unclear', () => {
  const raw = JSON.stringify({
    status: 'ok',
    problem: '2x + 5 = 15',
    steps: [],
    solution: 'x = 5',
    confidence: 0.2
  });
  assert.equal(parseAiAnswer(raw).status, 'unclear');
});

test('garbage becomes unclear instead of an invented answer', () => {
  assert.equal(parseAiAnswer('I cannot read this photo').status, 'unclear');
  assert.equal(parseAiAnswer('').status, 'unclear');
  assert.equal(parseAiAnswer('{"status":"ok","problem":"","solution":""}').status, 'unclear');
});
