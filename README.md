# ChapChap Backend



<p align="center">
  <img src="docs/images/chapchap-main-image.png" width="100%" alt="ChapChap 서비스 대표 이미지" />
</p>

<p align="center">
  <a href="https://chapchap.kr">Web</a> · iOS (준비 중)
</p>

## Overview

ChapChap은 일상의 소비를 기록하고, 방문한 동네와 장소를 돌아보며 반복되는 소비 속 나만의 취향을 발견하는 서비스입니다.

**ChapChap Backend**는 Web·App 클라이언트가 공통으로 사용하는 Spring Boot API 서버입니다.

Gradle Multi-Module 기반의 Modular Monolith 구조를 따르며,<br>
Domain-Driven Design 원칙에 따라 도메인 경계를 정의하고 각 모듈 내부에는 Clean Architecture를 적용했습니다.

[아키텍처와 설계 원칙](docs/architecture.md) · [Code·Git 컨벤션](docs/conventions.md)

## Features

| Domain | Features |
| --- | --- |
| 계정 | Web·iOS 공통 소셜 로그인(PKCE), 약관 동의, JWT 발급·재발급, 프로필·회원탈퇴 |
| 소비기록 | 날짜·시간·금액·카테고리·장소 기록, 방문 장소 조회, 스티커 획득 |
| 영수증 OCR | 영수증 이미지 인식, 매장명·주소·거래 일시·결제금액 초안 생성 |
| 장소 | Google Place ID 기반 중복 방지, 방문 장소 사진 조회, SGIS 행정동 변환, 장소 좋아요 |
| 추천 | 지도 중심 좌표 주변의 미방문 인기 장소와 최근 30일 선호 카테고리 장소 추천 |
| 리포트 | 현재 소비 현황과 월간 통계·소비 페르소나 제공 |
| 알림 | 알림 목록·읽음 처리, 월간 리포트 완료 및 금요일 소비 기록 리마인드 FCM 푸시 |

## Tech Stack

| Area | Technologies |
| --- | --- |
| **Core Stack** | <img src="https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white&labelColor=007396" alt="Java 21" /> <img src="https://img.shields.io/badge/Spring_Boot-3.5.16-6DB33F?style=flat-square&logo=springboot&logoColor=white&labelColor=6DB33F" alt="Spring Boot 3.5.16" /> <img src="https://img.shields.io/badge/Gradle-8.14.3-02303A?style=flat-square&logo=gradle&logoColor=white&labelColor=02303A" alt="Gradle 8.14.3" /> |
| **Web & API** | <img src="https://img.shields.io/badge/Spring_Web_MVC-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring Web MVC" /> <img src="https://img.shields.io/badge/Jakarta_Validation-ED8B00?style=flat-square&logo=jakartaee&logoColor=white" alt="Jakarta Bean Validation" /> <img src="https://img.shields.io/badge/Spring_RestClient-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring RestClient" /> <img src="https://img.shields.io/badge/springdoc--openapi-2.8.17-2E7D32?style=flat-square&logo=openapiinitiative&logoColor=white&labelColor=2E7D32" alt="springdoc-openapi 2.8.17" /> <img src="https://img.shields.io/badge/Swagger_UI-2E7D32?style=flat-square&logo=swagger&logoColor=white" alt="Swagger UI" /> |
| **Security** | <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white" alt="Spring Security" /> <img src="https://img.shields.io/badge/Kakao_OAuth_2.0-FEE500?style=flat-square&logo=kakao&logoColor=000000" alt="Kakao OAuth 2.0" /> <img src="https://img.shields.io/badge/Google_OpenID_Connect-4285F4?style=flat-square&logo=google&logoColor=white" alt="Google OpenID Connect" /> <img src="https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white" alt="JWT" /> |
| **Data** | <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring Data JPA" /> <img src="https://img.shields.io/badge/QueryDSL-0769AD?style=flat-square&logo=openjdk&logoColor=white" alt="QueryDSL" /> <img src="https://img.shields.io/badge/PostgreSQL-17-4169E1?style=flat-square&logo=postgresql&logoColor=white&labelColor=4169E1" alt="PostgreSQL 17" /> <img src="https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white" alt="Flyway" /> |
| **Geospatial** | <img src="https://img.shields.io/badge/PostGIS-3.5-336791?style=flat-square&logo=postgresql&logoColor=white&labelColor=336791" alt="PostGIS 3.5" /> <img src="https://img.shields.io/badge/Hibernate_Spatial-59666C?style=flat-square&logo=hibernate&logoColor=white" alt="Hibernate Spatial" /> <img src="https://img.shields.io/badge/JTS-007396?style=flat-square&logo=openjdk&logoColor=white" alt="JTS" /> |
| **Cache & Rate Limiting** | <img src="https://img.shields.io/badge/Redis-7.4-DC382D?style=flat-square&logo=redis&logoColor=white&labelColor=DC382D" alt="Redis 7.4" /> <img src="https://img.shields.io/badge/Lua_Script-2C2D72?style=flat-square&logo=lua&logoColor=white" alt="Lua Script" /> |
| **Integrations** | <img src="https://img.shields.io/badge/CLOVA_General_OCR-03C75A?style=flat-square&logo=naver&logoColor=white" alt="CLOVA General OCR" /> <img src="https://img.shields.io/badge/Google_Places_API-4285F4?style=flat-square&logo=googlemaps&logoColor=white" alt="Google Places API" /> <img src="https://img.shields.io/badge/SGIS_OpenAPI-2255A4?style=flat-square" alt="SGIS OpenAPI" /> <img src="https://img.shields.io/badge/Firebase_Cloud_Messaging-FFCA28?style=flat-square&logo=firebase&logoColor=black" alt="Firebase Cloud Messaging" /> |
| **Infrastructure** | <img src="https://img.shields.io/badge/Amazon_EC2-FF9900?style=flat-square&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTE5LjM1IDEwLjA0QTcuNDkgNy40OSAwIDAgMCAxMiA0QzkuMTEgNCA2LjYgNS42NCA1LjM1IDguMDRBNiA2IDAgMCAwIDYgMjBoMTNhNSA1IDAgMCAwIC4zNS05Ljk2WiIvPjwvc3ZnPg%3D%3D" alt="Amazon EC2" /> <img src="https://img.shields.io/badge/Amazon_RDS-527FFF?style=flat-square&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTE5LjM1IDEwLjA0QTcuNDkgNy40OSAwIDAgMCAxMiA0QzkuMTEgNCA2LjYgNS42NCA1LjM1IDguMDRBNiA2IDAgMCAwIDYgMjBoMTNhNSA1IDAgMCAwIC4zNS05Ljk2WiIvPjwvc3ZnPg%3D%3D" alt="Amazon RDS" /> <img src="https://img.shields.io/badge/Amazon_ECR-FF9900?style=flat-square&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTE5LjM1IDEwLjA0QTcuNDkgNy40OSAwIDAgMCAxMiA0QzkuMTEgNCA2LjYgNS42NCA1LjM1IDguMDRBNiA2IDAgMCAwIDYgMjBoMTNhNSA1IDAgMCAwIC4zNS05Ljk2WiIvPjwvc3ZnPg%3D%3D" alt="Amazon ECR" /> <img src="https://img.shields.io/badge/Amazon_S3-569A31?style=flat-square&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTE5LjM1IDEwLjA0QTcuNDkgNy40OSAwIDAgMCAxMiA0QzkuMTEgNCA2LjYgNS42NCA1LjM1IDguMDRBNiA2IDAgMCAwIDYgMjBoMTNhNSA1IDAgMCAwIC4zNS05Ljk2WiIvPjwvc3ZnPg%3D%3D" alt="Amazon S3" /> <img src="https://img.shields.io/badge/AWS_SSM-FF9900?style=flat-square&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTE5LjM1IDEwLjA0QTcuNDkgNy40OSAwIDAgMCAxMiA0QzkuMTEgNCA2LjYgNS42NCA1LjM1IDguMDRBNiA2IDAgMCAwIDYgMjBoMTNhNSA1IDAgMCAwIC4zNS05Ljk2WiIvPjwvc3ZnPg%3D%3D" alt="AWS Systems Manager" /> <img src="https://img.shields.io/badge/AWS_KMS-FF9900?style=flat-square&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTE5LjM1IDEwLjA0QTcuNDkgNy40OSAwIDAgMCAxMiA0QzkuMTEgNCA2LjYgNS42NCA1LjM1IDguMDRBNiA2IDAgMCAwIDYgMjBoMTNhNSA1IDAgMCAwIC4zNS05Ljk2WiIvPjwvc3ZnPg%3D%3D" alt="AWS KMS" /> <img src="https://img.shields.io/badge/Terraform-844FBA?style=flat-square&logo=terraform&logoColor=white" alt="Terraform" /> |
| **Deployment** | <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker" /> <img src="https://img.shields.io/badge/Docker_Compose-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker Compose" /> <img src="https://img.shields.io/badge/Caddy-2-1F88C0?style=flat-square&logo=caddy&logoColor=white&labelColor=1F88C0" alt="Caddy 2" /> <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white" alt="GitHub Actions" /> |
| **Observability** | <img src="https://img.shields.io/badge/Spring_Boot_Actuator-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot Actuator" /> <img src="https://img.shields.io/badge/SLF4J_%2B_Logback-007396?style=flat-square&logo=openjdk&logoColor=white" alt="SLF4J and Logback" /> <img src="https://img.shields.io/badge/Amazon_CloudWatch_Logs-FF4F8B?style=flat-square&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI%2BPHBhdGggZmlsbD0iI2ZmZiIgZD0iTTE5LjM1IDEwLjA0QTcuNDkgNy40OSAwIDAgMCAxMiA0QzkuMTEgNCA2LjYgNS42NCA1LjM1IDguMDRBNiA2IDAgMCAwIDYgMjBoMTNhNSA1IDAgMCAwIC4zNS05Ljk2WiIvPjwvc3ZnPg%3D%3D" alt="Amazon CloudWatch Logs" /> |
| **Testing** | <img src="https://img.shields.io/badge/JUnit_5-25A162?style=flat-square&logo=junit5&logoColor=white" alt="JUnit 5" /> <img src="https://img.shields.io/badge/Mockito-78A641?style=flat-square&logo=junit5&logoColor=white" alt="Mockito" /> <img src="https://img.shields.io/badge/Testcontainers-2496ED?style=flat-square&logo=testcontainers&logoColor=white" alt="Testcontainers" /> <img src="https://img.shields.io/badge/ArchUnit-5C2D91?style=flat-square&logo=junit5&logoColor=white" alt="ArchUnit" /> |

## Architecture

### System Architecture

AWS 인프라는 Terraform으로 관리하며, 하나의 Application EC2에서 Production과 Development의 애플리케이션·Redis 컨테이너를 분리해 운영합니다.<br>
Caddy는 `/api`와 `/dev/api` 요청을 각 환경으로 라우팅하고, Production은 Blue/Green, Development는 단일 컨테이너 교체 방식으로 배포합니다.<br>
RDS는 Private Subnet에 배치하고 데이터베이스와 S3 버킷은 환경별로 분리하며, 로그는 CloudWatch에 수집합니다.

<p align="center">
  <img src="docs/images/system-architecture-isometric.png" width="100%" alt="ChapChap 시스템 아키텍처 구성도" />
</p>

### Multi-Module Architecture

ChapChap은 하나의 애플리케이션으로 배포하는 Modular Monolith입니다. 도메인마다 Gradle 모듈을 분리하고, `app-server`가 실행 시점에 모듈을 조립합니다.

```mermaid
flowchart TB
    App["app-server<br/>실행 및 모듈 조립"]

    subgraph DomainModules["도메인 모듈"]
        direction LR
        Account["module-account<br/>계정 및 인증"]
        Report["module-report<br/>현재 현황 및 월간 리포트"]
        Recommendation["module-recommendation<br/>주변 장소 추천"]
        Consumption["module-consumption<br/>소비기록 관리 및 등록"]
        Place["module-place<br/>장소 및 위치 조회"]
        Notification["module-notification<br/>알림 관리 및 Push 발송"]

        Account --> Consumption
        Consumption --> Place
        Report --> Consumption
        Report --> Place
        Report --> Account
        Report --> Notification
        Notification --> Account
        Recommendation --> Consumption
        Recommendation --> Place
    end

    Core["module-core<br/>기술적 공통 요소"]

    App -->|"전체 조립"| DomainModules
    App --> Core
    DomainModules -->|"모든 모듈 참조"| Core
```

| Module | Responsibility |
| --- | --- |
| `app-server` | Spring Boot 실행, 전역 Web·Security·Scheduling 설정, 도메인 모듈 조립 |
| `module-account` | 계정·소셜 로그인·토큰 발급 및 사용자 상태 관리 |
| `module-consumption` | 소비기록 관리와 수기·영수증 OCR 기반 등록 |
| `module-place` | 장소·좋아요 관리, 위치 기반 조회와 외부 장소 정보 연동 |
| `module-recommendation` | 방문 이력·선호 카테고리를 조합한 주변 장소 추천 |
| `module-report` | 현재 소비 현황과 월간 소비 리포트 생성·조회 |
| `module-notification` | 알림 조회·읽음 처리와 FCM 푸시 발송 |
| `module-core` | 공통 API 응답·예외·인증 애노테이션, JPA 공통 요소와 테스트 Fixture |

도메인 모듈은 `api → application → domain`, `infra → application/domain` 방향을 따릅니다.<br>
다른 도메인의 Entity나 Repository를 직접 사용하지 않고, 필요한 모듈의 공개 Application API와 DTO를 통해 연동합니다.<br>
이 규칙은 ArchUnit 테스트로 검증합니다.

## Team

<table align="center">
  <tr>
    <td align="center" width="220">
      <a href="https://github.com/dhgudtmxhs">
        <img src="https://github.com/dhgudtmxhs.png?size=160" width="120" alt="오형석 GitHub 프로필" /><br />
        <strong>오형석</strong>
      </a><br />
      <sub>@dhgudtmxhs</sub>
    </td>
    <td align="center" width="220">
      <a href="https://github.com/p0o0y">
        <img src="https://github.com/p0o0y.png?size=160" width="120" alt="박가영 GitHub 프로필" /><br />
        <strong>박가영</strong>
      </a><br />
      <sub>@p0o0y</sub>
    </td>
  </tr>
</table>
