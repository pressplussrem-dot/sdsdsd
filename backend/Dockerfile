FROM node:20-alpine

WORKDIR /app

COPY package*.json ./
RUN npm install --omit=dev

COPY src ./src

ENV NODE_ENV=production
EXPOSE 3000

# The API key must be supplied at run time as an environment variable,
# for example: docker run -e OPENAI_API_KEY=... -e AI_PROVIDER=openai
CMD ["node", "src/server.js"]
