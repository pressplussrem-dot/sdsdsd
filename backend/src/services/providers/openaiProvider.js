import { config } from '../../config.js';
import { SYSTEM_PROMPT, userPrompt } from '../../utils/prompt.js';
import { fetchJson } from '../../utils/http.js';

/**
 * OpenAI (or any OpenAI-compatible endpoint) vision provider.
 * The API key is read from the environment and never leaves the server.
 */
export const openaiProvider = {
  name: 'openai',

  isConfigured() {
    return Boolean(config.openai.apiKey);
  },

  async solve({ imageBase64, mimeType, language }) {
    const body = {
      model: config.openai.model,
      max_tokens: 900,
      temperature: 0,
      response_format: { type: 'json_object' },
      messages: [
        { role: 'system', content: SYSTEM_PROMPT },
        {
          role: 'user',
          content: [
            { type: 'text', text: userPrompt(language) },
            {
              type: 'image_url',
              image_url: { url: `data:${mimeType};base64,${imageBase64}`, detail: 'high' }
            }
          ]
        }
      ]
    };

    const json = await fetchJson(`${config.openai.baseUrl}/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${config.openai.apiKey}`
      },
      body: JSON.stringify(body)
    });

    const text = json?.choices?.[0]?.message?.content;
    if (typeof text !== 'string' || text.trim() === '') {
      const error = new Error('Empty response from OpenAI');
      error.code = 'INVALID_AI_RESPONSE';
      throw error;
    }
    return text;
  }
};
