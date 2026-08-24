#!/usr/bin/env bash
# Vault(dev 모드) 기동 확인 + KV 시크릿 엔진 활성화.
#
# 중요 - 범위 안내: Backend는 현재 Vault에서 시크릿을 읽어오도록 구현되어 있지 않다
# (application.yml은 ${DB_PASSWORD}/${JWT_SECRET} 환경변수를 직접 읽음, 실제 값 주입은
# backend-secrets Kubernetes Secret으로 이루어짐 - app/backend-deployment.yaml 참고).
# 이 스크립트는 설계상 배치된 Vault 컴포넌트를 "정상 기동 + 사용 가능 상태"로 만들어두는 것까지만
# 다룬다. Backend-Vault 실연동(spring-cloud-vault 도입 등)은 별도 작업으로 남겨둔다.
#
# 사용법: infra/scripts/vault-init.sh
# 전제조건: kubectl이 k3s 클러스터를 가리키고 있어야 하고, vault Deployment의 파드가 Running 상태여야 함.

set -euo pipefail

NAMESPACE="quickchat-data"

POD=$(kubectl get pod -n "$NAMESPACE" -l app=vault -o jsonpath='{.items[0].metadata.name}')

if [ -z "$POD" ]; then
  echo "vault pod를 찾을 수 없습니다. 'kubectl get pods -n $NAMESPACE'로 상태를 확인하세요." >&2
  exit 1
fi

echo "Vault pod: $POD"

ROOT_TOKEN=$(kubectl get secret vault-credentials -n "$NAMESPACE" -o jsonpath='{.data.VAULT_DEV_ROOT_TOKEN_ID}' | base64 -d)

echo "Vault 상태 확인:"
kubectl exec -n "$NAMESPACE" "$POD" -- env VAULT_ADDR="http://127.0.0.1:8200" VAULT_TOKEN="$ROOT_TOKEN" vault status

echo "KV(v2) 시크릿 엔진 활성화 확인 (secret/ 경로, 이미 있으면 건너뜀):"
if kubectl exec -n "$NAMESPACE" "$POD" -- env VAULT_ADDR="http://127.0.0.1:8200" VAULT_TOKEN="$ROOT_TOKEN" vault secrets list | grep -q '^secret/'; then
  echo "secret/ 경로가 이미 활성화되어 있습니다."
else
  kubectl exec -n "$NAMESPACE" "$POD" -- env VAULT_ADDR="http://127.0.0.1:8200" VAULT_TOKEN="$ROOT_TOKEN" vault secrets enable -path=secret kv-v2
fi

echo "완료. 참고: dev 모드는 인메모리 저장이라 vault 파드가 재시작되면 여기서 활성화한 내용도 사라집니다."
