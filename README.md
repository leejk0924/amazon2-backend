# Amazon2 - Blog Management Service
네이버 블로그 모임을 위한 관리 및 조회 서비스입니다.

---
## 1.프로젝트 개요
기존에 프로토타입으로 운영했으나, 성능 이슈 및 설계 미스로 인해 프로젝트를 리뉴얼하고자 한다.
아래는 이전에 경험했던 한계점들이다.
- DB 없이 파일 시스템으로 관리 인원을 저장하여 변경이 어러움
- 관리 인원 수가 점차 증가하여 응답의 지연이 점차 늘어남
- 라즈베리파이 단일 서버 운영으로 인해 불안정한 서비스

본 프로젝트는 위 문제를 해결하기 위해 재설계된 프로젝트이다.

---
## 2. 목표
- 관리 인원의 포스팅 수를 확인 응답 속도 개선
- 인원 관리 편의성 개선
- 장애 발생 시 복구 가능한 구조 설계 및 모니터링 시스템 구축
- AWS 이전을 고려한 확장 가능한 구조

---
## 3. 기술 스택
### Backend
- SpringBoot
- JPA

### Database
- MySQL

### Frontend
현재 :
- HTML / CSS / JS
- Nginx 통한 정적 파일 제공 (Backend의 REST API 기능에 집중을 위해)

향후 :
- AWS 이전하면서 React 기반 SPA로 전환할 예정
- CloudFront + S3를 통한 정적 배포 예정

### Infra
- Docker / Docker Compose
- Nginx

## 4. 시스템 아키텍처
(추후 다이어그램으로 추가 예정)

## 5. 문서
- [요구사항 명세서](/docs/Requirements.md)
- [ERD 다이어그램](/docs/ERD.md)

## 6. API 명세서 (Swagger)
[API 명세서](http://localhost:8080/swagger-ui/index.html)
[API 문서](http://localhost:8080/v3/api-docs)
