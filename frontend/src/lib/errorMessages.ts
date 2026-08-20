// errorCode -> 사용자 문구 매핑. business-rules.md 에러 표시 정책(컨텍스트별 인라인).
// 코드 목록/문구는 backend/src/main/java/com/quickchat/backend/exception/*.java 및
// GlobalExceptionHandler.java(VALIDATION_ERROR, INTERNAL_ERROR)와 동기화되어 있다.
const ERROR_MESSAGES: Record<string, string> = {
  ALREADY_MEMBER: "이미 채널의 멤버입니다.",
  CHANNEL_ARCHIVED: "보관된 채널에는 메시지를 보낼 수 없습니다.",
  ACCOUNT_LOCKED: "로그인 실패 횟수가 초과되어 계정이 잠겼습니다. 잠시 후 다시 시도해주세요.",
  NOT_A_MEMBER: "채널의 멤버가 아닙니다.",
  INVALID_CREDENTIALS: "이메일 또는 비밀번호가 올바르지 않습니다.",
  RATE_LIMITED: "메시지 전송 속도가 너무 빠릅니다. 잠시 후 다시 시도해주세요.",
  EMAIL_ALREADY_EXISTS: "이미 가입된 이메일입니다.",
  CHANNEL_NOT_FOUND: "채널을 찾을 수 없습니다.",
  USER_NOT_FOUND: "사용자를 찾을 수 없습니다.",
  FORBIDDEN_ACTION: "이 작업을 수행할 권한이 없습니다.",
  VALIDATION_ERROR: "입력값을 다시 확인해주세요.",
  INTERNAL_ERROR: "예상치 못한 오류가 발생했습니다.",
};

const UNKNOWN_ERROR_MESSAGE = "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";

/** Backend가 내려준 errorCode에 대응하는 문구를 찾는다. 매핑이 없으면 fallback(주로 서버 message)을 쓴다. */
export function messageForErrorCode(errorCode: string, fallbackMessage?: string): string {
  return ERROR_MESSAGES[errorCode] ?? fallbackMessage ?? UNKNOWN_ERROR_MESSAGE;
}
