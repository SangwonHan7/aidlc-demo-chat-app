#!/usr/bin/env bash
# chat-messages 토픽을 명시적으로 생성한다.
#
# 참고: Kafka는 기본적으로 auto.create.topics.enable=true라서 Backend가 처음 publish/subscribe할 때
# 토픽이 자동으로 생겨도 동작은 한다(docker-compose 환경도 지금까지 이 방식으로 동작해왔다). 다만
# 자동 생성 시 partition=1, replication=1로 생성되어 파티션 수를 나중에 조정하기 까다롭고, 첫 요청이
# 몰릴 때 타이밍 이슈가 생길 수 있어 배포 시점에 명시적으로 만들어두는 편이 안전하다.
#
# 사용법: infra/scripts/kafka-topics.sh
# 전제조건: kubectl이 k3s 클러스터를 가리키고 있어야 하고, kafka StatefulSet의 파드가 Running 상태여야 함.

set -euo pipefail

NAMESPACE="quickchat-data"
TOPIC="chat-messages"
PARTITIONS=3
REPLICATION_FACTOR=1

POD=$(kubectl get pod -n "$NAMESPACE" -l app=kafka -o jsonpath='{.items[0].metadata.name}')

if [ -z "$POD" ]; then
  echo "kafka pod를 찾을 수 없습니다. 'kubectl get pods -n $NAMESPACE'로 상태를 확인하세요." >&2
  exit 1
fi

echo "Kafka pod: $POD"
echo "토픽 생성/확인: $TOPIC (partitions=$PARTITIONS, replication-factor=$REPLICATION_FACTOR)"

kubectl exec -n "$NAMESPACE" "$POD" -- kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --if-not-exists \
  --topic "$TOPIC" \
  --partitions "$PARTITIONS" \
  --replication-factor "$REPLICATION_FACTOR"

echo "현재 토픽 목록:"
kubectl exec -n "$NAMESPACE" "$POD" -- kafka-topics.sh --bootstrap-server localhost:9092 --list
