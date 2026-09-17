import { config } from '../../config.js';
import { SYSTEM_PROMPT, userPrompt } from '../../utils/prompt.js';
import { fetchJson } from '../../utils/http.js';

/** Google Gemini vision provider. Same contract as the OpenAI provider. */
export const geminiProvider = {
  name: 'gemini',

  isConfigured() {
    return Boolean(config.gemini.apiKey);
  },

  async solve({ imageBase64, mimeType, language }) {
    const url =
      `${config.gemini.baseUrl}/models/${config.gemini.model}:generateContent` +
      `?key=${encodeURIComponent(config.gemini.apiKey)}`;

    const body = {
      systemInstruction: {
        role: 'system',
        parts: [{ text: SYSTEM_PROMPT }]
      },
      contents: [
        {
          role: 'user',
          parts: [
            { text: userPrompt(language) },
            { inline_data: { mime_type: mimeType, data: imageBase64 } }
          ]
        }
      ],
      generationConfig: {
        temperature: 0,
        maxOutputTokens: 900,
        responseMimeType: 'application/json'
      }
    };

    const json = await fetchJson(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });

    const parts = json?.candidates?.[0]?.content?.parts;
    const text = Array.isArray(parts)
      ? parts.map((part) => part?.text ?? '').join('').trim()
      : '';

    if (text === '') {
      const error = new Error('Empty response from Gemini');
      error.code = 'INVALID_AI_RESPONSE';
      throw error;
    }
    return text;
  }
};
