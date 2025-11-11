#!/bin/bash

# Heimdall Kubernetes 배포 스크립트

set -e

NAMESPACE=${NAMESPACE:-default}
IMAGE_TAG=${IMAGE_TAG:-latest}

echo "🛡️  Heimdall Kubernetes 배포 시작"
echo "===================================="
echo "네임스페이스: $NAMESPACE"
echo "이미지 태그: $IMAGE_TAG"
echo ""

# 1. 네임스페이스 생성 (없는 경우)
echo "📦 네임스페이스 확인 및 생성..."
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# 2. ConfigMap 적용
echo "📝 ConfigMap 적용..."
kubectl apply -f k8s/configmap.yaml -n $NAMESPACE

# 3. Secret 적용
echo "🔐 Secret 적용..."
kubectl apply -f k8s/secret.yaml -n $NAMESPACE

# 4. Deployment 적용
echo "🚀 Deployment 적용..."
kubectl apply -f k8s/deployment.yaml -n $NAMESPACE

# 5. Service 적용
echo "🌐 Service 적용..."
kubectl apply -f k8s/service.yaml -n $NAMESPACE

# 6. HPA 적용
echo "📈 HorizontalPodAutoscaler 적용..."
kubectl apply -f k8s/hpa.yaml -n $NAMESPACE

# 7. PodDisruptionBudget 적용
echo "🛡️  PodDisruptionBudget 적용..."
kubectl apply -f k8s/pdb.yaml -n $NAMESPACE

# 8. NetworkPolicy 적용 (선택사항)
if [ -f k8s/networkpolicy.yaml ]; then
    echo "🔒 NetworkPolicy 적용..."
    kubectl apply -f k8s/networkpolicy.yaml -n $NAMESPACE
fi

# 9. Ingress 적용 (선택사항)
if [ -f k8s/ingress.yaml ]; then
    echo "🌍 Ingress 적용..."
    kubectl apply -f k8s/ingress.yaml -n $NAMESPACE
fi

# 10. ServiceMonitor 적용 (Prometheus Operator가 있는 경우)
if [ -f k8s/servicemonitor.yaml ]; then
    echo "📊 ServiceMonitor 적용..."
    kubectl apply -f k8s/servicemonitor.yaml -n $NAMESPACE || echo "⚠️  ServiceMonitor 적용 실패 (Prometheus Operator가 설치되지 않았을 수 있습니다)"
fi

echo ""
echo "✅ 배포 완료!"
echo ""

# 배포 상태 확인
echo "📊 배포 상태 확인 중..."
echo ""

echo "Pods:"
kubectl get pods -n $NAMESPACE -l app=heimdall

echo ""
echo "Services:"
kubectl get svc -n $NAMESPACE -l app=heimdall

echo ""
echo "HPA:"
kubectl get hpa -n $NAMESPACE heimdall-hpa

echo ""
echo "=========================================="
echo "🎉 Heimdall이 성공적으로 배포되었습니다!"
echo "=========================================="
echo ""
echo "다음 명령으로 상태를 확인할 수 있습니다:"
echo "  kubectl get pods -n $NAMESPACE -l app=heimdall"
echo "  kubectl logs -f -n $NAMESPACE deployment/heimdall"
echo "  kubectl port-forward -n $NAMESPACE svc/heimdall-service 8080:8080"
echo ""
