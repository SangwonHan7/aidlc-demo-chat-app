/** @type {import('next').NextConfig} */
// Infrastructure Design(Frontend) Question 1 답변 A: 정적 export가 아니라 Next.js Node 서버(`next start`)로
// 서빙한다 - `/api/health` Route Handler가 동작하려면 서버 런타임이 필요하기 때문.
const nextConfig = {
  reactStrictMode: true,
};

module.exports = nextConfig;
