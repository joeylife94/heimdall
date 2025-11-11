#!/bin/bash

# Heimdall Kubernetes 삭제 스크립트

set -e

NAMESPACE=${NAMESPACE:-default}

echo "🛡️  Heimdall Kubernetes 리소스 삭제"
echo "===================================="
echo "네임스페이스: $NAMESPACE"
echo ""

read -p "정말로 $NAMESPACE 네임스페이스의 Heimdall 리소스를 삭제하시겠습니까? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "❌ 삭제가 취소되었습니다."
    exit 0
fi

echo ""
echo "🗑️  리소스 삭제 중..."

# ServiceMonitor 삭제
if kubectl get servicemonitor heimdall-metrics -n $NAMESPACE &> /dev/null; then
    echo "📊 ServiceMonitor 삭제..."
    kubectl delete -f k8s/servicemonitor.yaml -n $NAMESPACE || true
fi

# Ingress 삭제
if kubectl get ingress heimdall-ingress -n $NAMESPACE &> /dev/null; then
    echo "🌍 Ingress 삭제..."
    kubectl delete -f k8s/ingress.yaml -n $NAMESPACE || true
fi

# NetworkPolicy 삭제
if kubectl get networkpolicy heimdall-network-policy -n $NAMESPACE &> /dev/null; then
    echo "🔒 NetworkPolicy 삭제..."
    kubectl delete -f k8s/networkpolicy.yaml -n $NAMESPACE || true
fi

# PodDisruptionBudget 삭제
echo "🛡️  PodDisruptionBudget 삭제..."
kubectl delete -f k8s/pdb.yaml -n $NAMESPACE || true

# HPA 삭제
echo "📈 HorizontalPodAutoscaler 삭제..."
kubectl delete -f k8s/hpa.yaml -n $NAMESPACE || true

# Service 삭제
echo "🌐 Service 삭제..."
kubectl delete -f k8s/service.yaml -n $NAMESPACE || true

# Deployment 삭제
echo "🚀 Deployment 삭제..."
kubectl delete -f k8s/deployment.yaml -n $NAMESPACE || true

# ConfigMap 삭제 (선택사항)
read -p "ConfigMap도 삭제하시겠습니까? (yes/no): " delete_config
if [ "$delete_config" = "yes" ]; then
    echo "📝 ConfigMap 삭제..."
    kubectl delete -f k8s/configmap.yaml -n $NAMESPACE || true
fi

# Secret 삭제 (선택사항)
read -p "Secret도 삭제하시겠습니까? (yes/no): " delete_secret
if [ "$delete_secret" = "yes" ]; then
    echo "🔐 Secret 삭제..."
    kubectl delete -f k8s/secret.yaml -n $NAMESPACE || true
fi

echo ""
echo "✅ 삭제 완료!"
echo ""

# 남아있는 리소스 확인
echo "남아있는 Heimdall 리소스:"
kubectl get all -n $NAMESPACE -l app=heimdall || echo "리소스 없음"

echo ""
echo "=========================================="
echo "🎉 Heimdall 리소스가 삭제되었습니다!"
echo "=========================================="
