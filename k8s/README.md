# Heimdall Kubernetes 배포 가이드

## 📋 사전 요구사항

### 1. Kubernetes 클러스터
- Kubernetes 1.23 이상
- kubectl 설치 및 설정
- 최소 3개 이상의 노드 권장

### 2. 필수 애드온 (선택사항)
- **Metrics Server**: HPA 동작을 위해 필요
- **Ingress Controller**: 외부 접근을 위해 필요 (NGINX 권장)
- **Cert-Manager**: HTTPS/TLS를 위해 필요
- **Prometheus Operator**: 모니터링을 위해 필요

## 🚀 배포 방법

### 방법 1: 스크립트 사용 (권장)

```bash
# 실행 권한 부여
chmod +x k8s/deploy.sh k8s/undeploy.sh

# 배포
cd k8s
./deploy.sh

# 또는 네임스페이스 지정
NAMESPACE=production ./deploy.sh
```

### 방법 2: 수동 배포

```bash
# 1. 네임스페이스 생성 (선택사항)
kubectl create namespace heimdall

# 2. ConfigMap 생성
kubectl apply -f k8s/configmap.yaml

# 3. Secret 생성 (먼저 secret.yaml 파일의 비밀번호를 변경하세요!)
kubectl apply -f k8s/secret.yaml

# 4. Deployment 생성
kubectl apply -f k8s/deployment.yaml

# 5. Service 생성
kubectl apply -f k8s/service.yaml

# 6. HPA 생성
kubectl apply -f k8s/hpa.yaml

# 7. PodDisruptionBudget 생성
kubectl apply -f k8s/pdb.yaml

# 8. NetworkPolicy 생성 (선택사항)
kubectl apply -f k8s/networkpolicy.yaml

# 9. Ingress 생성 (선택사항, 도메인 설정 필요)
kubectl apply -f k8s/ingress.yaml

# 10. ServiceMonitor 생성 (Prometheus Operator가 있는 경우)
kubectl apply -f k8s/servicemonitor.yaml
```

## 🔧 설정 수정

### 1. 환경 변수 변경

`k8s/configmap.yaml` 파일을 수정하여 애플리케이션 설정 변경:

```bash
kubectl edit configmap heimdall-config
# 또는
kubectl apply -f k8s/configmap.yaml
```

### 2. 비밀 정보 변경

**주의**: 프로덕션 환경에서는 반드시 비밀번호를 변경하세요!

```bash
# Base64 인코딩된 값으로 변경
echo -n 'your-new-password' | base64

# Secret 업데이트
kubectl edit secret heimdall-secret
# 또는
kubectl apply -f k8s/secret.yaml
```

### 3. 리소스 제한 조정

`k8s/deployment.yaml`에서 리소스 요청/제한 수정:

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

### 4. 레플리카 수 조정

```bash
# 수동 스케일링
kubectl scale deployment heimdall --replicas=5

# HPA 설정 변경
kubectl edit hpa heimdall-hpa
```

## 📊 상태 확인

### Pod 상태 확인

```bash
# Pod 목록 조회
kubectl get pods -l app=heimdall

# Pod 상세 정보
kubectl describe pod <pod-name>

# Pod 로그 확인
kubectl logs -f deployment/heimdall

# 여러 Pod 로그 동시 확인 (stern 사용)
stern heimdall
```

### Service 확인

```bash
# Service 목록
kubectl get svc -l app=heimdall

# Service 상세 정보
kubectl describe svc heimdall-service
```

### HPA 상태 확인

```bash
# HPA 상태 조회
kubectl get hpa heimdall-hpa

# HPA 상세 정보
kubectl describe hpa heimdall-hpa
```

### 메트릭 확인

```bash
# Pod 메트릭
kubectl top pods -l app=heimdall

# Node 메트릭
kubectl top nodes
```

## 🔍 디버깅

### Pod가 시작되지 않는 경우

```bash
# Pod 이벤트 확인
kubectl describe pod <pod-name>

# Pod 로그 확인
kubectl logs <pod-name>

# 이전 컨테이너 로그 확인 (재시작된 경우)
kubectl logs <pod-name> --previous

# Pod 내부 접속
kubectl exec -it <pod-name> -- /bin/sh
```

### Health Check 실패

```bash
# Health check 엔드포인트 직접 테스트
kubectl port-forward <pod-name> 8080:8080
curl http://localhost:8080/actuator/health

# Readiness probe 로그 확인
kubectl describe pod <pod-name> | grep -A 10 Readiness
```

### 데이터베이스 연결 문제

```bash
# Secret 확인
kubectl get secret heimdall-secret -o yaml

# 네트워크 정책 확인
kubectl get networkpolicy

# DNS 해결 테스트
kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup postgres-service
```

## 🔄 업데이트

### 이미지 업데이트

```bash
# 새 이미지로 업데이트
kubectl set image deployment/heimdall heimdall=heimdall:v2.0.0

# 롤아웃 상태 확인
kubectl rollout status deployment/heimdall

# 롤백 (필요한 경우)
kubectl rollout undo deployment/heimdall
```

### ConfigMap/Secret 업데이트 후 재시작

```bash
# ConfigMap 업데이트
kubectl apply -f k8s/configmap.yaml

# Pod 재시작 (롤링 업데이트)
kubectl rollout restart deployment/heimdall
```

## 🗑️ 삭제

### 스크립트 사용

```bash
cd k8s
./undeploy.sh
```

### 수동 삭제

```bash
# 모든 리소스 삭제
kubectl delete -f k8s/

# 또는 레이블 기반 삭제
kubectl delete all -l app=heimdall
```

## 🌐 외부 접근

### Port Forward (테스트용)

```bash
kubectl port-forward svc/heimdall-service 8080:8080
# 브라우저에서 http://localhost:8080 접속
```

### Ingress 설정

`k8s/ingress.yaml` 파일에서 도메인 수정:

```yaml
spec:
  rules:
  - host: heimdall.your-domain.com  # 여기를 변경
    http:
      paths:
      - path: /
```

그 후 DNS 레코드 설정:
```
heimdall.your-domain.com → <Ingress-External-IP>
```

### LoadBalancer 사용

```bash
# External IP 확인
kubectl get svc heimdall-external

# IP가 할당될 때까지 대기
kubectl get svc heimdall-external -w
```

## 📈 모니터링 설정

### Prometheus 연동

ServiceMonitor를 사용하여 자동으로 메트릭 수집:

```bash
kubectl apply -f k8s/servicemonitor.yaml
```

### Grafana 대시보드

1. Grafana에 접속
2. Import Dashboard 선택
3. Spring Boot 대시보드 ID: 4701 입력
4. Prometheus 데이터 소스 선택

## 🔐 보안 권장사항

### 1. Secret 관리
- 프로덕션에서는 Sealed Secrets 또는 External Secrets Operator 사용
- 비밀번호를 Git에 커밋하지 않기
- 정기적으로 비밀번호 변경

### 2. Network Policy
- NetworkPolicy를 활성화하여 Pod 간 통신 제한
- 필요한 포트만 열기

### 3. RBAC
- 최소 권한 원칙 적용
- ServiceAccount 사용

### 4. Pod Security
- SecurityContext 설정
- Non-root 사용자로 실행
- ReadOnlyRootFilesystem 활성화

## 🎯 프로덕션 체크리스트

배포 전 확인사항:

- [ ] Secret의 모든 비밀번호가 변경되었는가?
- [ ] 리소스 제한이 적절히 설정되었는가?
- [ ] Health check가 정상 동작하는가?
- [ ] HPA가 올바르게 설정되었는가?
- [ ] PodDisruptionBudget이 설정되었는가?
- [ ] NetworkPolicy가 적용되었는가?
- [ ] 모니터링이 설정되었는가?
- [ ] 백업 전략이 수립되었는가?
- [ ] 로그 수집이 설정되었는가?
- [ ] Ingress/TLS가 올바르게 설정되었는가?

## 📚 추가 참고자료

- [Kubernetes 공식 문서](https://kubernetes.io/docs/)
- [Spring Boot Kubernetes 가이드](https://spring.io/guides/gs/spring-boot-kubernetes/)
- [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator)
