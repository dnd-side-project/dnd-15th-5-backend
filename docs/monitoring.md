## 모니터링

### 📊 1. 수집 구성

| 대상 | 수집 방식 | 지표 |
| --- | --- | --- |
| EC2 | Alloy Unix exporter | CPU, 메모리, 부하, 디스크 용량·I/O |
| 운영 Blue/Green | Micrometer → Actuator → Alloy | JVM 메모리·GC, HTTP 요청·응답 시간, DB 커넥션 풀 |
| Alloy | Alloy self exporter | 시작 시각, 수집 오류, 전송 대기·실패·재시도 |

- Alloy가 60초마다 지표를 수집하고 Grafana Cloud가 저장·시각화를 담당한다.
- 개발 앱은 별도로 수집하지 않는다. EC2 전체 사용량에는 개발 앱과 Redis도 포함된다.
- 로그는 기존 CloudWatch Logs를 사용한다. Loki와 알림 웹훅은 구성하지 않는다.

**포트 및 실행 설정**

- 운영 앱은 `prod,monitoring` 프로필로 실행한다. 일반 API는 `8080`, Actuator의 `health`·`prometheus`는 `9091`을 사용한다.
- `9091`은 호스트에 공개하지 않고 Caddy도 연결하지 않는다. Alloy는 같은 Docker 네트워크에서 접근한다.
- Blue/Green을 각각 수집한다. 배포 중에는 두 슬롯을 모두 수집하며, 중지된 슬롯의 `up=0`은 정상이다.
- Alloy 자원 제한은 CPU 0.5개, 메모리 384 MiB다. 전송 대기 데이터는 `alloy-data` 볼륨에 최대 2시간 보관한다.

### 🚀 2. 배포

1. Terraform으로 모니터링 토큰 경로의 저장·조회 및 KMS 권한을 적용한다.

   ```bash
   terraform -chdir=infra/terraform plan
   terraform -chdir=infra/terraform apply
   ```

2. Grafana Cloud에서 `metrics:write` 권한의 토큰을 발급하고 GitHub Actions Secret `GRAFANA_CLOUD_API_KEY`에 등록한다.
3. `develop → main`을 반영해 운영 CD를 실행한다.
4. 배포 후 Grafana Cloud에서 지표 수신을 확인한다.

**CD 처리**

- 토큰은 Parameter Store의 `/chapchap/prod/deploy/monitoring-env`에 SecureString으로 저장한다.
- EC2에서는 `/app/chapchap/.env.monitoring`으로 내려받아 Alloy 환경 변수로 전달한다. 파일 권한은 `600`이다.
- Alloy 설정 검증 후 컨테이너를 실행한다. 설정·토큰·Alloy 실행 구성이 바뀔 때만 재생성한다.
- 토큰 교체 시 GitHub Secret을 갱신하고 다시 배포한다. 서버에서 직접 수정한 설정은 다음 배포 때 덮어쓴다.

### 🔎 3. 지표 조회

Grafana Cloud의 Explore에서 Prometheus 데이터 소스를 선택한다.

```promql
up{job="integrations/node_exporter", instance="chapchap-ec2"}
up{job="chapchap/backend", environment="prod"}
up{job="integrations/alloy-check", instance="chapchap-ec2"}
```

| 확인 항목 | PromQL |
| --- | --- |
| EC2 CPU 사용률 (%) | `100 * (1 - avg(rate(node_cpu_seconds_total{instance="chapchap-ec2",mode="idle"}[5m])))` |
| EC2 메모리 사용률 (%) | `100 * (1 - node_memory_MemAvailable_bytes{instance="chapchap-ec2"} / node_memory_MemTotal_bytes{instance="chapchap-ec2"})` |
| EC2 루트 디스크 사용률 (%) | `100 * (1 - node_filesystem_avail_bytes{instance="chapchap-ec2",mountpoint="/"} / node_filesystem_size_bytes{instance="chapchap-ec2",mountpoint="/"})` |
| 슬롯별 JVM Heap 사용량 | `sum by (slot) (jvm_memory_used_bytes{job="chapchap/backend",area="heap"})` |
| API별 초당 요청 수 | `sum by (uri) (rate(http_server_requests_seconds_count{job="chapchap/backend",uri!~"/actuator.*"}[5m]))` |
| 서버 오류율 (%) | `100 * ((sum(rate(http_server_requests_seconds_count{job="chapchap/backend",uri!~"/actuator.*",status=~"5.."}[5m])) or vector(0)) / sum(rate(http_server_requests_seconds_count{job="chapchap/backend",uri!~"/actuator.*"}[5m])))` |
| API별 p95 응답 시간 (초) | `histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket{job="chapchap/backend",uri!~"/actuator.*"}[5m])))` |

- 응답 시간 버킷은 0.1·0.3·1·3·10초로 설정한다. p95는 해당 구간의 근삿값이다.
- 네트워크 등 수집하지 않는 지표는 범용 Node Exporter 대시보드에서 빈 패널로 표시될 수 있다.

### 🛠️ 4. EC2 상태 확인

```bash
docker logs --tail 100 alloy
docker stats --no-stream alloy
docker exec app-blue wget -qO- http://localhost:9091/actuator/health
```

| 증상 | 확인 항목 |
| --- | --- |
| Cloud 전송 401·403 | 토큰, 스택, `metrics:write` 권한 |
| Cloud 전송 429 | Grafana Cloud 사용량·수집 한도 |
| 활성 슬롯의 `up=0` | 앱 실행 상태, `prod,monitoring` 프로필, 9091 연결 |
| Alloy 반복 재시작 | 컨테이너 로그, 메모리 제한 |

전송 장애가 2시간을 넘으면 보관 기간이 지난 지표는 유실될 수 있다.
