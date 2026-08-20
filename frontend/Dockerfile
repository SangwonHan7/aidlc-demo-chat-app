# 멀티스테이지 빌드. Infrastructure Design(Frontend) Question 1 답변 A: Next.js Node 서버(`next start`)로 서빙.
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json ./
RUN npm install

FROM node:20-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# Next.js는 "use client" 컴포넌트에서 쓰는 NEXT_PUBLIC_* 값을 런타임이 아니라 `next build` 시점에
# 정적으로 인라인한다 - 컨테이너 실행 시 -e/environment로 주입해도 이미 빌드된 클라이언트 JS에는
# 반영되지 않는다. 그래서 반드시 build-arg로 받아 ENV로 내려준 뒤 빌드해야 한다
# (deployment-architecture.md "환경변수 주입" 참고. 기본값은 로컬 docker-compose 기준).
ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
ARG NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_WS_URL=$NEXT_PUBLIC_WS_URL
RUN npm run build

FROM node:20-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/next.config.js ./
COPY --from=build /app/.next ./.next
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/package.json ./package.json

EXPOSE 3000
CMD ["npm", "run", "start"]
