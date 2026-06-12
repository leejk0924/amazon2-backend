# API Documenter - OpenAPI YAML 가이드

## 파일 위치

`docs/openapi/{domain}-api.yaml`

---

## OpenAPI 3.0 기본 스펙

```yaml
openapi: 3.0.0
info:
  title: {Domain} API
  description: {Domain} 관련 REST API
  version: 1.0.0
  contact:
    name: Amazon2 Team
    email: team@example.com

servers:
  - url: http://localhost:8080
    description: 로컬 개발
  - url: https://api.example.com
    description: 운영 환경

tags:
  - name: {Domain}
    description: {Domain} 관련 API

paths:
  /api/{domain}:
    post:
      tags:
        - {Domain}
      summary: "{Domain} 생성"
      description: "새로운 {Domain}을 생성합니다."
      operationId: create{Domain}
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/{Domain}CreateRequest'
      responses:
        '201':
          description: "생성 성공"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/{Domain}Response'
        '400':
          description: "잘못된 요청"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
    
    get:
      tags:
        - {Domain}
      summary: "{Domain} 전체 조회"
      description: "모든 {Domain}을 조회합니다."
      operationId: findAll{Domain}s
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: "조회 성공"
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/{Domain}Response'
  
  /api/{domain}/{id}:
    get:
      tags:
        - {Domain}
      summary: "{Domain} 상세 조회"
      description: "ID로 {Domain}을 조회합니다."
      operationId: findById{Domain}
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: "조회 성공"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/{Domain}Response'
        '404':
          description: "리소스 없음"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
    
    patch:
      tags:
        - {Domain}
      summary: "{Domain} 수정"
      description: "ID로 {Domain}을 수정합니다."
      operationId: update{Domain}
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/{Domain}UpdateRequest'
      responses:
        '200':
          description: "수정 성공"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/{Domain}Response'
        '404':
          description: "리소스 없음"
    
    delete:
      tags:
        - {Domain}
      summary: "{Domain} 삭제"
      description: "ID로 {Domain}을 삭제합니다."
      operationId: delete{Domain}
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '204':
          description: "삭제 성공"
        '404':
          description: "리소스 없음"

components:
  schemas:
    {Domain}CreateRequest:
      type: object
      required:
        - name
        - description
      properties:
        name:
          type: string
          description: "{Domain} 이름"
          example: "Example {Domain} Name"
        description:
          type: string
          description: "{Domain} 설명"
          example: "Example description"
        status:
          type: string
          enum: [ACTIVE, INACTIVE]
          description: "{Domain} 상태"
          example: "ACTIVE"
    
    {Domain}UpdateRequest:
      type: object
      properties:
        name:
          type: string
          example: "Updated {Domain} Name"
        description:
          type: string
          example: "Updated description"
        status:
          type: string
          enum: [ACTIVE, INACTIVE]
          example: "INACTIVE"
    
    {Domain}Response:
      type: object
      properties:
        id:
          type: integer
          format: int64
          example: 1
        name:
          type: string
          example: "Example {Domain} Name"
        description:
          type: string
          example: "Example description"
        status:
          type: string
          example: "ACTIVE"
        createdAt:
          type: string
          format: date-time
          example: "2024-01-01T00:00:00"
        updatedAt:
          type: string
          format: date-time
          example: "2024-01-02T00:00:00"
    
    ErrorResponse:
      type: object
      properties:
        errorCode:
          type: string
          example: "400"
        message:
          type: string
          example: "잘못된 요청입니다"
        timestamp:
          type: string
          format: date-time
          example: "2024-01-01T00:00:00"
```

---

## Schema 정의 규칙

### 필드 정의

```yaml
{fieldName}:
  type: string|integer|boolean|array|object
  description: "필드 설명"
  example: "필드 예시"
  format: date-time|int64|double
  enum: [값1, 값2]
  required: true|false
```

### 복합 타입

```yaml
address:
  type: object
  properties:
    street:
      type: string
    city:
      type: string
    zipCode:
      type: string

tags:
  type: array
  items:
    type: string
```

---

## 참고사항

- operationId는 유일해야 함
- parameters의 순서대로 Swagger UI에 표시
- schema는 components/schemas에서 정의하고 $ref로 참조
- 모든 응답 코드별로 responses 정의
- required 필드는 명시적으로 지정
