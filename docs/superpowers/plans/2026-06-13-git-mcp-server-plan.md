# Git MCP Server 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** GitHub의 공식 MCP 서버를 기반으로 CONTRIBUTING.md 규칙을 자동 적용하는 Node.js Wrapper를 amazon2-backend에 통합하여, Claude Code에서 PR/이슈를 자동으로 생성하고 관리할 수 있게 한다.

**Architecture:** 
- Node.js 기반 MCP Wrapper가 CONTRIBUTING.md를 파싱하고 규칙을 적용
- GitHub 공식 MCP 서버와 HTTP 통신으로 GitHub API 호출
- Docker Compose로 기존 Spring Boot 환경에 통합
- Stdio 기반 MCP 프로토콜로 Claude Code와 통신

**Tech Stack:**
- TypeScript/Node.js (MCP Wrapper)
- @modelcontextprotocol/sdk (MCP 프로토콜)
- GitHub REST API
- Docker & Docker Compose
- Jest (테스트)

---

## 파일 구조

```
amazon2-backend/
├── mcp-wrapper/  ✨ 신규
│   ├── Dockerfile
│   ├── package.json
│   ├── tsconfig.json
│   ├── .env.example
│   ├── .dockerignore
│   ├── README.md
│   ├── src/
│   │   ├── index.ts
│   │   ├── types.ts
│   │   ├── services/
│   │   │   ├── contributing-parser.ts
│   │   │   ├── formatter.ts
│   │   │   └── github-client.ts
│   │   ├── tools/
│   │   │   ├── pr-tools.ts
│   │   │   ├── issue-tools.ts
│   │   │   └── contributing-tools.ts
│   │   └── utils/
│   │       └── logger.ts
│   └── tests/
│       ├── contributing-parser.test.ts
│       ├── formatter.test.ts
│       └── tools.test.ts
├── docker-compose.yml (수정)
├── .env (수정)
└── .env.example (수정)
```

---

## Task 1: MCP Wrapper 프로젝트 초기화

**Files:**
- Create: `mcp-wrapper/package.json`
- Create: `mcp-wrapper/tsconfig.json`
- Create: `mcp-wrapper/.env.example`
- Create: `mcp-wrapper/.dockerignore`
- Create: `mcp-wrapper/.gitignore`

- [ ] **Step 1: package.json 생성**

```bash
cd mcp-wrapper
cat > package.json << 'EOF'
{
  "name": "amazon2-git-mcp",
  "version": "1.0.0",
  "description": "GitHub MCP Server for amazon2-backend project",
  "main": "dist/index.js",
  "scripts": {
    "dev": "ts-node src/index.ts",
    "build": "tsc",
    "start": "node dist/index.js",
    "test": "jest",
    "test:watch": "jest --watch",
    "test:coverage": "jest --coverage"
  },
  "keywords": ["mcp", "github", "amazon2"],
  "author": "",
  "license": "MIT",
  "dependencies": {
    "@modelcontextprotocol/sdk": "^0.7.0",
    "axios": "^1.6.0",
    "dotenv": "^16.0.0"
  },
  "devDependencies": {
    "@types/jest": "^29.5.0",
    "@types/node": "^20.0.0",
    "jest": "^29.5.0",
    "ts-jest": "^29.1.0",
    "ts-node": "^10.9.0",
    "typescript": "^5.0.0"
  }
}
EOF
```

- [ ] **Step 2: tsconfig.json 생성**

```bash
cat > tsconfig.json << 'EOF'
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "commonjs",
    "lib": ["ES2020"],
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "strictFunctionTypes": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noImplicitReturns": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist", "tests"]
}
EOF
```

- [ ] **Step 3: .env.example 생성**

```bash
cat > .env.example << 'EOF'
# GitHub 인증
GITHUB_TOKEN=ghp_xxxx...

# 저장소 정보
REPOSITORY=jk/amazon2-backend

# MCP 설정
MCP_IMPLEMENTATION=stdio
LOG_LEVEL=info

# CONTRIBUTING.md 경로 (Docker 환경에서)
CONTRIBUTING_MD_PATH=/app/docs/CONTRIBUTING.md

# Claude Code 라벨
CLAUDE_CODE_LABEL=claude-code-generated
CLAUDE_CODE_LABEL_COLOR=0366d6
EOF
```

- [ ] **Step 4: .dockerignore 생성**

```bash
cat > .dockerignore << 'EOF'
node_modules
npm-debug.log
dist
tests
.git
.env
*.md
.gitignore
EOF
```

- [ ] **Step 5: .gitignore 생성**

```bash
cat > .gitignore << 'EOF'
node_modules/
dist/
*.log
.env
.env.local
.DS_Store
coverage/
EOF
```

- [ ] **Step 6: npm install 실행**

```bash
npm install
```

Expected: `package-lock.json` 생성, node_modules 설치 완료

- [ ] **Step 7: Commit**

```bash
cd ../
git add mcp-wrapper/package.json mcp-wrapper/tsconfig.json mcp-wrapper/.env.example mcp-wrapper/.dockerignore mcp-wrapper/.gitignore mcp-wrapper/package-lock.json
git commit -m "feat: MCP Wrapper 프로젝트 초기화

- TypeScript 개발 환경 설정
- MCP SDK, axios, dotenv 의존성 추가
- 개발/프로덕션 구성 분리

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>"
```

---

## Task 2: 타입 정의 (types.ts)

**Files:**
- Create: `mcp-wrapper/src/types.ts`

- [ ] **Step 1: types.ts 생성**

```typescript
// mcp-wrapper/src/types.ts

// GitHub 이슈 정보
export interface GitHubIssue {
  number: number;
  title: string;
  body: string;
  labels: string[];
  state: 'open' | 'closed';
  created_at: string;
  updated_at: string;
  html_url?: string;
}

// GitHub PR 정보
export interface GitHubPullRequest {
  number: number;
  title: string;
  body: string;
  head: {
    ref: string;  // branch명
  };
  base: {
    ref: string;  // base branch
  };
  state: 'open' | 'closed' | 'merged';
  created_at: string;
  updated_at: string;
  html_url?: string;
}

// CONTRIBUTING.md에서 파싱한 규칙
export interface ContributingRules {
  commitTypes: CommitType[];
  branchPattern: BranchPattern;
  prTemplate: string;
}

// 커밋 타입 (CONTRIBUTING.md에서)
export interface CommitType {
  type: string;
  description: string;
  example: string;
}

// Branch 네이밍 규칙
export interface BranchPattern {
  format: string;  // "feature/#<issue>-<description>"
  types: string[]; // ["feature", "fix", "docs", ...]
}

// PR 생성 입력
export interface CreatePRInput {
  issueNumber: number;
  currentBranch: string;
}

// PR 생성 출력
export interface CreatePROutput {
  prNumber: number;
  prUrl: string;
  branchName: string;
  title: string;
}

// 이슈 생성 입력
export interface CreateIssueInput {
  title: string;
  body: string;
  issueType: 'feature' | 'bug' | 'docs' | 'refactor';
}

// 이슈 생성 출력
export interface CreateIssueOutput {
  issueNumber: number;
  issueUrl: string;
  labels: string[];
}

// 로거 인터페이스
export interface Logger {
  info(message: string): void;
  error(message: string, error?: Error): void;
  debug(message: string): void;
  warn(message: string): void;
}
```

- [ ] **Step 2: Commit**

```bash
git add mcp-wrapper/src/types.ts
git commit -m "feat: MCP Wrapper 타입 정의

- GitHub API 응답 타입 정의
- CONTRIBUTING.md 규칙 타입
- 입출력 인터페이스 정의

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>"
```

---

## Task 3: Logger 유틸리티 구현

**Files:**
- Create: `mcp-wrapper/src/utils/logger.ts`

- [ ] **Step 1: logger.ts 구현**

```typescript
// mcp-wrapper/src/utils/logger.ts
import { Logger } from '../types';

const LOG_LEVEL = process.env.LOG_LEVEL || 'info';
const LEVELS = { debug: 0, info: 1, warn: 2, error: 3 };
const currentLevel = LEVELS[LOG_LEVEL as keyof typeof LEVELS] || 1;

export const logger: Logger = {
  info(message: string) {
    if (currentLevel <= LEVELS.info) {
      console.log(`[INFO] ${new Date().toISOString()} - ${message}`);
    }
  },
  
  error(message: string, error?: Error) {
    if (currentLevel <= LEVELS.error) {
      console.error(`[ERROR] ${new Date().toISOString()} - ${message}`);
      if (error) {
        console.error(error.stack);
      }
    }
  },
  
  debug(message: string) {
    if (currentLevel <= LEVELS.debug) {
      console.log(`[DEBUG] ${new Date().toISOString()} - ${message}`);
    }
  },
  
  warn(message: string) {
    if (currentLevel <= LEVELS.warn) {
      console.warn(`[WARN] ${new Date().toISOString()} - ${message}`);
    }
  }
};
```

- [ ] **Step 2: Commit**

```bash
git add mcp-wrapper/src/utils/logger.ts
git commit -m "feat: Logger 유틸리티 구현

- LOG_LEVEL 환경변수 기반 로깅
- debug/info/warn/error 레벨 지원

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>"
```

---

## Task 4: CONTRIBUTING.md 파서 구현 (TDD)

**Files:**
- Create: `mcp-wrapper/tests/contributing-parser.test.ts`
- Create: `mcp-wrapper/src/services/contributing-parser.ts`

- [ ] **Step 1: 테스트 파일 작성**

```typescript
// mcp-wrapper/tests/contributing-parser.test.ts
import { ContributingParser } from '../src/services/contributing-parser';
import * as fs from 'fs';
import * as path from 'path';

describe('ContributingParser', () => {
  let parser: ContributingParser;
  const testFilePath = path.join(__dirname, '../CONTRIBUTING_TEST.md');

  beforeAll(() => {
    // 테스트용 CONTRIBUTING.md 생성
    const content = `# Contributing Guide

## 1. 커밋 메시지 규칙 (Conventional Commits)

### 타입 (Type)

| 타입         | 설명                          |
|------------|-----------------------------|
| \`Feat\`     | 새 기능 추가                     |
| \`Fix\`      | 버그 수정                       |
| \`Docs\`     | 문서 작성/수정                    |
| \`Refactor\` | 코드 구조 개선                     |

## 2. 브랜치 전략

### 브랜치 네이밍
\`\`\`
<type>/<이슈번호>-<설명>
\`\`\`

### 브랜치 타입
- \`feature/\` : 신규 기능 개발
- \`fix/\` : 버그 수정
- \`docs/\` : 문서 작업
`;
    fs.writeFileSync(testFilePath, content);
    parser = new ContributingParser(testFilePath);
  });

  afterAll(() => {
    if (fs.existsSync(testFilePath)) {
      fs.unlinkSync(testFilePath);
    }
  });

  test('파일에서 커밋 타입을 추출한다', () => {
    const types = parser.getCommitTypes();
    expect(types).toContainEqual(
      expect.objectContaining({ type: 'Feat' })
    );
  });

  test('branch 패턴을 추출한다', () => {
    const pattern = parser.getBranchPattern();
    expect(pattern.format).toContain('<type>');
    expect(pattern.format).toContain('<이슈번호>');
    expect(pattern.types).toContain('feature');
    expect(pattern.types).toContain('fix');
  });

  test('PR 템플릿을 생성한다', () => {
    const template = parser.getPRTemplate();
    expect(template).toBeTruthy();
  });
});
```

- [ ] **Step 2: 테스트 실행 (실패 확인)**

```bash
cd mcp-wrapper
npm test -- tests/contributing-parser.test.ts
```

Expected: FAIL (ContributingParser 클래스 없음)

- [ ] **Step 3: ContributingParser 구현**

```typescript
// mcp-wrapper/src/services/contributing-parser.ts
import * as fs from 'fs';
import { ContributingRules, CommitType, BranchPattern } from '../types';
import { logger } from '../utils/logger';

export class ContributingParser {
  private content: string;
  private rules: ContributingRules | null = null;

  constructor(filePath: string) {
    try {
      this.content = fs.readFileSync(filePath, 'utf-8');
    } catch (error) {
      logger.error(`CONTRIBUTING.md를 읽을 수 없습니다: ${filePath}`, error as Error);
      this.content = '';
    }
  }

  getCommitTypes(): CommitType[] {
    if (!this.content) return [];

    // 테이블에서 타입 추출 (| 타입 | 설명 |)
    const typeRegex = /\|\s*`(\w+)`\s*\|\s*([^|]+)\s*\|/g;
    const types: CommitType[] = [];
    let match;

    while ((match = typeRegex.exec(this.content)) !== null) {
      types.push({
        type: match[1],
        description: match[2].trim(),
        example: `${match[1]}: 예시`
      });
    }

    return types;
  }

  getBranchPattern(): BranchPattern {
    // 브랜치 패턴 추출 (```<type>/<이슈번호>-<설명>```)
    const patternRegex = /```\n(<type>\/[^`]+)\n```/;
    const match = this.content.match(patternRegex);
    const format = match ? match[1] : 'feature/#<issue>-<description>';

    // 브랜치 타입 추출 (- `feature/` :)
    const typeRegex = /- `(\w+)\/`\s*:/g;
    const types: string[] = [];
    let typeMatch;

    while ((typeMatch = typeRegex.exec(this.content)) !== null) {
      types.push(typeMatch[1]);
    }

    return {
      format,
      types: types.length > 0 ? types : ['feature', 'fix', 'docs', 'refactor']
    };
  }

  getPRTemplate(): string {
    // 기본 PR 템플릿
    return `## 개요
이 PR이 무엇을 구현하는지 간단히 설명

## 변경사항
- 주요 변경 사항 1
- 주요 변경 사항 2

## 테스트 방법
- 어떻게 테스트했는지

## 관련 이슈
Closes #`;
  }

  getRules(): ContributingRules {
    if (!this.rules) {
      this.rules = {
        commitTypes: this.getCommitTypes(),
        branchPattern: this.getBranchPattern(),
        prTemplate: this.getPRTemplate()
      };
    }
    return this.rules;
  }
}
```

- [ ] **Step 4: 테스트 실행 (성공 확인)**

```bash
npm test -- tests/contributing-parser.test.ts
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd ../
git add mcp-wrapper/src/services/contributing-parser.ts mcp-wrapper/tests/contributing-parser.test.ts
git commit -m "feat: CONTRIBUTING.md 파서 구현 (TDD)

- 커밋 타입 추출
- Branch 패턴 파싱
- PR 템플릿 생성
- Jest 테스트 작성

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>"
```

---

## Task 5: 포매터 구현 (TDD)

**Files:**
- Create: `mcp-wrapper/tests/formatter.test.ts`
- Create: `mcp-wrapper/src/services/formatter.ts`

- [ ] **Step 1: 테스트 파일 작성**

```typescript
// mcp-wrapper/tests/formatter.test.ts
import { Formatter } from '../src/services/formatter';
import { GitHubIssue, BranchPattern } from '../src/types';

describe('Formatter', () => {
  const branchPattern: BranchPattern = {
    format: 'feature/#<issue>-<description>',
    types: ['feature', 'fix', 'docs']
  };
  const formatter = new Formatter(branchPattern);

  test('이슈 번호와 설명으로 branch명을 생성한다', () => {
    const branchName = formatter.generateBranchName('feature', 5, '회원-조회-api-구현');
    expect(branchName).toBe('feature/#5-회원-조회-api-구현');
  });

  test('PR 제목을 생성한다', () => {
    const title = formatter.generatePRTitle(5, '회원 조회 API 구현');
    expect(title).toBe('#5: 회원 조회 API 구현');
  });

  test('PR 설명을 생성한다 (이슈 정보 포함)', () => {
    const issue: GitHubIssue = {
      number: 5,
      title: '회원 조회 API 구현',
      body: '회원 정보를 조회하는 API가 필요합니다.',
      labels: ['feature'],
      state: 'open',
      created_at: '2026-06-13',
      updated_at: '2026-06-13'
    };
    const description = formatter.generatePRDescription(issue);
    expect(description).toContain('개요');
    expect(description).toContain('회원 정보를 조회하는 API가 필요합니다.');
    expect(description).toContain('Closes #5');
  });

  test('Branch 타입을 검증한다', () => {
    expect(formatter.isValidBranchType('feature')).toBe(true);
    expect(formatter.isValidBranchType('invalid')).toBe(false);
  });
});
```

- [ ] **Step 2: 테스트 실행 (실패 확인)**

```bash
cd mcp-wrapper
npm test -- tests/formatter.test.ts
```

Expected: FAIL

- [ ] **Step 3: Formatter 구현**

```typescript
// mcp-wrapper/src/services/formatter.ts
import { GitHubIssue, BranchPattern } from '../types';
import { logger } from '../utils/logger';

export class Formatter {
  private branchPattern: BranchPattern;

  constructor(branchPattern: BranchPattern) {
    this.branchPattern = branchPattern;
  }

  generateBranchName(type: string, issueNumber: number, description: string): string {
    if (!this.isValidBranchType(type)) {
      logger.warn(`유효하지 않은 branch 타입: ${type}`);
      type = 'feature';
    }

    // description 정규화: 공백 제거, 하이픈으로 변환
    const normalizedDesc = description
      .toLowerCase()
      .replace(/\s+/g, '-')
      .replace(/[^a-z0-9-]/g, '');

    return `${type}/#${issueNumber}-${normalizedDesc}`;
  }

  generatePRTitle(issueNumber: number, issueTitle: string): string {
    return `#${issueNumber}: ${issueTitle}`;
  }

  generatePRDescription(issue: GitHubIssue): string {
    return `## 개요
${issue.body || issue.title}

## 변경사항
- 이슈 #${issue.number}의 구현 내용

## 테스트 방법
- 해당 기능을 테스트했습니다

## 관련 이슈
Closes #${issue.number}`;
  }

  isValidBranchType(type: string): boolean {
    return this.branchPattern.types.includes(type);
  }
}
```

- [ ] **Step 4: 테스트 실행 (성공 확인)**

```bash
npm test -- tests/formatter.test.ts
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd ../
git add mcp-wrapper/src/services/formatter.ts mcp-wrapper/tests/formatter.test.ts
git commit -m "feat: 포매터 구현 (TDD)

- Branch명 자동 생성 (CONTRIBUTING.md 규칙 적용)
- PR 제목 생성
- PR 설명 생성 (이슈 정보 포함)
- Branch 타입 검증

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>"
```

---

## Task 6: GitHub 클라이언트 구현

**Files:**
- Create: `mcp-wrapper/tests/github-client.test.ts`
- Create: `mcp-wrapper/src/services/github-client.ts`

- [ ] **Step 1: 테스트 파일 작성 (mock 사용)**

```typescript
// mcp-wrapper/tests/github-client.test.ts
import { GitHubClient } from '../src/services/github-client';
import axios from 'axios';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('GitHubClient', () => {
  const token = 'test-token';
  const repo = 'jk/amazon2-backend';
  let client: GitHubClient;

  beforeEach(() => {
    client = new GitHubClient(token, repo);
    jest.clearAllMocks();
  });

  test('이슈를 조회한다', async () => {
    mockedAxios.get.mockResolvedValue({
      data: {
        number: 5,
        title: '테스트',
        body: '테스트 본문',
        labels: [],
        state: 'open',
        created_at: '2026-06-13',
        updated_at: '2026-06-13'
      }
    });

    const issue = await client.getIssue(5);
    expect(issue.number).toBe(5);
    expect(issue.title).toBe('테스트');
  });

  test('이슈를 생성한다', async () => {
    mockedAxios.post.mockResolvedValue({
      data: {
        number: 42,
        title: '새 이슈',
        html_url: 'https://github.com/jk/amazon2-backend/issues/42',
        labels: ['feature']
      }
    });

    const result = await client.createIssue('새 이슈', '본문', ['feature']);
    expect(result.issueNumber).toBe(42);
  });

  test('PR을 생성한다', async () => {
    mockedAxios.post.mockResolvedValue({
      data: {
        number: 25,
        title: 'PR 제목',
        html_url: 'https://github.com/jk/amazon2-backend/pull/25',
        head: { ref: 'feature/#5-test' }
      }
    });

    const result = await client.createPullRequest(
      'feature/#5-test',
      'main',
      'PR 제목',
      'PR 본문'
    );
    expect(result.prNumber).toBe(25);
  });
});
```

- [ ] **Step 2: 테스트 실행 (실패)**

```bash
cd mcp-wrapper
npm test -- tests/github-client.test.ts
```

Expected: FAIL

- [ ] **Step 3: GitHubClient 구현**

```typescript
// mcp-wrapper/src/services/github-client.ts
import axios, { AxiosInstance } from 'axios';
import { GitHubIssue, GitHubPullRequest, CreateIssueOutput } from '../types';
import { logger } from '../utils/logger';

export class GitHubClient {
  private api: AxiosInstance;
  private repo: string;

  constructor(token: string, repo: string) {
    this.api = axios.create({
      baseURL: 'https://api.github.com',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'application/vnd.github.v3+json'
      }
    });
    this.repo = repo;
  }

  async getIssue(issueNumber: number): Promise<GitHubIssue> {
    try {
      const response = await this.api.get<GitHubIssue>(
        `/repos/${this.repo}/issues/${issueNumber}`
      );
      return response.data;
    } catch (error) {
      logger.error(`이슈 #${issueNumber} 조회 실패`, error as Error);
      throw error;
    }
  }

  async createIssue(
    title: string,
    body: string,
    labels: string[]
  ): Promise<CreateIssueOutput> {
    try {
      const response = await this.api.post<GitHubIssue>(
        `/repos/${this.repo}/issues`,
        {
          title,
          body,
          labels
        }
      );

      return {
        issueNumber: response.data.number,
        issueUrl: response.data.html_url || `https://github.com/${this.repo}/issues/${response.data.number}`,
        labels: response.data.labels
      };
    } catch (error) {
      logger.error(`이슈 생성 실패: ${title}`, error as Error);
      throw error;
    }
  }

  async createPullRequest(
    headBranch: string,
    baseBranch: string,
    title: string,
    body: string
  ): Promise<{ prNumber: number; prUrl: string }> {
    try {
      const response = await this.api.post<GitHubPullRequest>(
        `/repos/${this.repo}/pulls`,
        {
          head: headBranch,
          base: baseBranch,
          title,
          body
        }
      );

      return {
        prNumber: response.data.number,
        prUrl: response.data.html_url || `https://github.com/${this.repo}/pull/${response.data.number}`
      };
    } catch (error) {
      logger.error(`PR 생성 실패: ${title}`, error as Error);
      throw error;
    }
  }
}
```

- [ ] **Step 4: 테스트 실행 (성공)**

```bash
npm test -- tests/github-client.test.ts
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd ../
git add mcp-wrapper/src/services/github-client.ts mcp-wrapper/tests/github-client.test.ts
git commit -m "feat: GitHub 클라이언트 구현

- GitHub API 호출 (Axios)
- 이슈 조회/생성 기능
- PR 생성 기능
- 에러 처리 및 로깅
- Jest Mock 테스트

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>"
```

---

## Task 7: MCP 도구 구현 - PR & Issue Tools

**Files:**
- Create: `mcp-wrapper/src/tools/pr-tools.ts`
- Create: `mcp-wrapper/src/tools/issue-tools.ts`

- [ ] **Step 1: PR Tools 구현**

```typescript
// mcp-wrapper/src/tools/pr-tools.ts
import { Tool } from '@modelcontextprotocol/sdk/shared/messages';
import { GitHubClient } from '../services/github-client';
import { Formatter } from '../services/formatter';
import { CreatePRInput } from '../types';
import { logger } from '../utils/logger';

export function createPRTools(
  githubClient: GitHubClient,
  formatter: Formatter
): Tool[] {
  return [
    {
      name: 'create_pr_from_issue',
      description: '이슈를 기반으로 PR을 자동 생성합니다. CONTRIBUTING.md 규칙에 따라 branch명과 PR 정보가 자동으로 생성됩니다.',
      inputSchema: {
        type: 'object' as const,
        properties: {
          issueNumber: {
            type: 'number',
            description: '이슈 번호 (예: 5)'
          },
          currentBranch: {
            type: 'string',
            description: '현재 branch (예: main)'
          }
        },
        required: ['issueNumber', 'currentBranch']
      }
    }
  ];
}

export async function handleCreatePRFromIssue(
  githubClient: GitHubClient,
  formatter: Formatter,
  input: CreatePRInput
): Promise<{
  prNumber: number;
  prUrl: string;
  branchName: string;
  title: string;
}> {
  try {
    logger.info(`이슈 #${input.issueNumber}에서 PR 생성 시작`);

    // 1. 이슈 정보 조회
    const issue = await githubClient.getIssue(input.issueNumber);
    logger.debug(`이슈 조회 완료: ${issue.title}`);

    // 2. Branch명 생성
    const branchName = formatter.generateBranchName(
      'feature',
      issue.number,
      issue.title
    );
    logger.debug(`Branch명 생성: ${branchName}`);

    // 3. PR 제목/설명 생성
    const prTitle = formatter.generatePRTitle(issue.number, issue.title);
    const prDescription = formatter.generatePRDescription(issue);

    // 4. PR 생성
    const { prNumber, prUrl } = await githubClient.createPullRequest(
      branchName,
      input.currentBranch,
      prTitle,
      prDescription
    );

    logger.info(`PR #${prNumber} 생성 완료: ${prUrl}`);

    return {
      prNumber,
      prUrl,
      branchName,
      title: prTitle
    };
  } catch (error) {
    logger.error(`PR 생성 실패`, error as Error);
    throw error;
  }
}
```

- [ ] **Step 2: Issue Tools 구현**

```typescript
// mcp-wrapper/src/tools/issue-tools.ts
import { Tool } from '@modelcontextprotocol/sdk/shared/messages';
import { GitHubClient } from '../services/github-client';
import { CreateIssueInput } from '../types';
import { logger } from '../utils/logger';

const CLAUDE_CODE_LABEL = process.env.CLAUDE_CODE_LABEL || 'claude-code-generated';

export function createIssueTools(githubClient: GitHubClient): Tool[] {
  return [
    {
      name: 'create_issue_from_claude',
      description: 'Claude Code에서 발견한 버그나 기능을 GitHub 이슈로 생성합니다.',
      inputSchema: {
        type: 'object' as const,
        properties: {
          title: {
            type: 'string',
            description: '이슈 제목 (50자 이내)'
          },
          body: {
            type: 'string',
            description: '이슈 본문'
          },
          issueType: {
            type: 'string',
            enum: ['feature', 'bug', 'docs', 'refactor'],
            description: '이슈 타입'
          }
        },
        required: ['title', 'body', 'issueType']
      }
    },
    {
      name: 'get_issue_details',
      description: '이슈 번호로 상세 정보를 조회합니다.',
      inputSchema: {
        type: 'object' as const,
        properties: {
          issueNumber: {
            type: 'number',
            description: '이슈 번호'
          }
        },
        required: ['issueNumber']
      }
    }
  ];
}

export async function handleCreateIssueFromClaude(
  githubClient: GitHubClient,
  input: CreateIssueInput
): Promise<{
  issueNumber: number;
  issueUrl: string;
  labels: string[];
}> {
  try {
    logger.info(`Claude Code 이슈 생성 시작: ${input.title}`);

    const labels = [CLAUDE_CODE_LABEL, input.issueType];
    const result = await githubClient.createIssue(
      input.title,
      input.body,
      labels
    );

    logger.info(`이슈 #${result.issueNumber} 생성 완료: ${result.issueUrl}`);
    return result;
  } catch (error) {
    logger.error(`이슈 생성 실패`, error as Error);
    throw error;
  }
}

export async function handleGetIssueDetails(
  githubClient: GitHubClient,
  issueNumber: number
): Promise<any> {
  try {
    logger.info(`이슈 #${issueNumber} 상세 정보 조회`);
    const issue = await githubClient.getIssue(issueNumber);
    return issue;
  } catch (error) {
    logger.error(`이슈 조회 실패`, error as Error);
    throw error;
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add mcp-wrapper/src/tools/pr-tools.ts mcp-wrapper/src/tools/issue-tools.ts
git commit -m "feat: MCP PR/Issue 도구 구현

- create_pr_from_issue: 이슈 기반 PR 자동 생성
- create_issue_from_claude: Claude Code 이슈 생성
- get_issue_details: 이슈 조회
- 자동 라벨 관리 (claude-code-generated)

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>"
```

---

## Task 8: MCP 도구 구현 - Contributing Tools

**Files:**
- Create: `mcp-wrapper/src/tools/contributing-tools.ts`

- [ ] **Step 1: Contributing Tools 구현**

```typescript
// mcp-wrapper/src/tools/contributing-tools.ts
import { Tool } from '@modelcontextprotocol/sdk/shared/messages';
import { ContributingParser } from '../services/contributing-parser';
import { logger } from '../utils/logger';

export function createContributingTools(parser: ContributingParser): Tool[] {
  return [
    {
      name: 'parse_contributing_md',
      description: 'CONTRIBUTING.md에서 프로젝트의 규칙을 파싱합니다.',
      inputSchema: {
        type: 'object' as const,
        properties: {},
        required: []
      }
    }
  ];
}

export async function handleParseContributingMd(
  parser: ContributingParser
): Promise<any> {
  try {
    logger.info('CONTRIBUTING.md 파싱');
    const rules = parser.getRules();
    logger.debug(`파싱 완료: 커밋 타입 ${rules.commitTypes.length}개`);
    return rules;
  } catch (error) {
    logger.error('CONTRIBUTING.md 파싱 실패', error as Error);
    throw error;
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add mcp-wrapper/src/tools/contributing-tools.ts
git commit -m "feat: MCP Contributing 도구 구현

- parse_contributing_md: CONTRIBUTING.md 파싱 및 반환

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>"
```

---

## Task 9: MCP 서버 진입점 구현 (index.ts)

**Files:**
- Create: `mcp-wrapper/src/index.ts`

- [ ] **Step 1: index.ts 구현**

```typescript
// mcp-wrapper/src/index.ts
import { Server } from '@modelcontextprotocol/sdk/server/stdio';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/shared/messages';
import * as dotenv from 'dotenv';

import { ContributingParser } from './services/contributing-parser';
import { Formatter } from './services/formatter';
import { GitHubClient } from './services/github-client';
import {
  createPRTools,
  handleCreatePRFromIssue,
} from './tools/pr-tools';
import {
  createIssueTools,
  handleCreateIssueFromClaude,
  handleGetIssueDetails,
} from './tools/issue-tools';
import {
  createContributingTools,
  handleParseContributingMd,
} from './tools/contributing-tools';
import { logger } from './utils/logger';
import { CreatePRInput, CreateIssueInput } from './types';

dotenv.config();

const GITHUB_TOKEN = process.env.GITHUB_TOKEN;
const REPOSITORY = process.env.REPOSITORY;
const CONTRIBUTING_MD_PATH = process.env.CONTRIBUTING_MD_PATH || './docs/CONTRIBUTING.md';

if (!GITHUB_TOKEN || !REPOSITORY) {
  logger.error('필수 환경 변수 누락: GITHUB_TOKEN, REPOSITORY');
  process.exit(1);
}

const parser = new ContributingParser(CONTRIBUTING_MD_PATH);
const rules = parser.getRules();
const formatter = new Formatter(rules.branchPattern);
const githubClient = new GitHubClient(GITHUB_TOKEN, REPOSITORY);

const server = new Server({
  name: 'amazon2-git-mcp',
  version: '1.0.0',
});

const prTools = createPRTools(githubClient, formatter);
const issueTools = createIssueTools(githubClient);
const contributingTools = createContributingTools(parser);

const allTools = [...prTools, ...issueTools, ...contributingTools];

server.setRequestHandler(ListToolsRequestSchema, async () => {
  logger.debug(`${allTools.length}개의 도구 반환`);
  return {
    tools: allTools,
  };
});

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request;
  logger.info(`도구 실행: ${name}`);

  try {
    switch (name) {
      case 'create_pr_from_issue': {
        const input = args as CreatePRInput;
        const result = await handleCreatePRFromIssue(githubClient, formatter, input);
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case 'create_issue_from_claude': {
        const input = args as CreateIssueInput;
        const result = await handleCreateIssueFromClaude(githubClient, input);
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case 'get_issue_details': {
        const { issueNumber } = args as { issueNumber: number };
        const result = await handleGetIssueDetails(githubClient, issueNumber);
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      case 'parse_contributing_md': {
        const result = await handleParseContributingMd(parser);
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify(result, null, 2),
            },
          ],
        };
      }

      default:
        return {
          content: [
            {
              type: 'text',
              text: `알 수 없는 도구: ${name}`,
            },
          ],
          isError: true,
        };
    }
  } catch (error) {
    logger.error(`도구 실행 실패: ${name}`, error as Error);
    return {
      content: [
        {
          type: 'text',
          text: `도구 실행 실패: ${(error as Error).message}`,
        },
      ],
      isError: true,
    };
  }
});

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  logger.info('Amazon2 Git MCP Server 시작');
  logger.info(`저장소: ${REPOSITORY}`);
  logger.info(`CONTRIBUTING.md: ${CONTRIBUTING_MD_PATH}`);
  logger.info(`등록된 도구: ${allTools.length}개`);
}

main().catch((error) => {
  logger.error('서버 시작 실패', error);
  process.exit(1);
});
```

- [ ] **Step 2: 빌드 및 테스트**

```bash
cd mcp-wrapper
npm run build
```

Expected: `dist/index.js` 생성

- [ ] **Step 3: Commit**

```bash
cd ../
git add mcp-wrapper/src/index.ts mcp-wrapper/dist/
git commit -m "feat: MCP 서버 진입점 구현

- Stdio 기반 MCP 서버 구현
- 모든 도구 등록
- 도구 실행 핸들러 구현
- 에러 처리 및 로깅

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>"
```

---

## Task 10: Docker 구성 및 통합

**Files:**
- Create: `mcp-wrapper/Dockerfile`
- Modify: `docker-compose.yml`
- Modify: `.env` (또는 `.env.example`)

- [ ] **Step 1: Dockerfile 생성**

```dockerfile
# mcp-wrapper/Dockerfile
FROM node:20-alpine

WORKDIR /app

COPY package*.json ./
COPY tsconfig.json ./
COPY src/ ./src/

RUN npm ci --only=production && npm run build && rm -rf src/

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD node -e "process.exit(0)"

CMD ["npm", "start"]
```

- [ ] **Step 2: docker-compose.yml 수정**

기존 docker-compose.yml에 다음 서비스 추가 (services 섹션에):

```yaml
  github-mcp-wrapper:
    build:
      context: ./mcp-wrapper
      dockerfile: Dockerfile
    container_name: amazon2-git-mcp-wrapper
    ports:
      - "3000:3000"
    environment:
      GITHUB_TOKEN: ${GITHUB_TOKEN}
      REPOSITORY: ${REPOSITORY}
      LOG_LEVEL: info
      CONTRIBUTING_MD_PATH: /app/docs/CONTRIBUTING.md
    depends_on:
      - github-mcp
    restart: unless-stopped
    networks:
      - amazon2

  github-mcp:
    image: ghcr.io/github/github-mcp-server:latest
    container_name: amazon2-github-mcp
    ports:
      - "3001:3001"
    environment:
      GITHUB_PERSONAL_ACCESS_TOKEN: ${GITHUB_TOKEN}
      GITHUB_TOOLSETS: "issues,pull_requests"
      GITHUB_READ_ONLY: "false"
      LOG_LEVEL: info
    restart: unless-stopped
    networks:
      - amazon2
```

기존 서비스(app, db)에 networks 추가:

```yaml
    networks:
      - amazon2
```

파일 끝에 networks 추가:

```yaml
networks:
  amazon2:
    driver: bridge
```

- [ ] **Step 3: docker-compose 문법 검증**

```bash
docker-compose config > /dev/null && echo "✅ 문법 OK"
```

Expected: ✅ 문법 OK

- [ ] **Step 4: .env.example 수정**

```bash
# 기존 .env.example에 다음 추가
cat >> .env.example << 'EOF'

# GitHub MCP 설정
GITHUB_TOKEN=ghp_xxxx...
REPOSITORY=jk/amazon2-backend
EOF
```

- [ ] **Step 5: Commit**

```bash
git add mcp-wrapper/Dockerfile docker-compose.yml .env.example
git commit -m "feat: Docker 구성 (github-mcp 통합)

- Dockerfile: Node.js 기반 MCP Wrapper
- docker-compose.yml: github-mcp-wrapper, github-mcp 서비스 추가
- network 설정 (amazon2)
- 환경 변수 설정

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>"
```

---

## Task 11: README 작성

**Files:**
- Create: `mcp-wrapper/README.md`

- [ ] **Step 1: README.md 작성**

```markdown
# Amazon2 Git MCP Server

GitHub MCP 서버를 통합한 amazon2-backend용 PR/이슈 자동화 도구입니다.

## 기능

- **PR 자동 생성** - 이슈를 기반으로 CONTRIBUTING.md 규칙 자동 적용
- **이슈 자동 생성** - Claude Code에서 발견한 버그/기능을 이슈로 등록
- **규칙 자동 반영** - CONTRIBUTING.md 규칙이 MCP에 자동 반영

## 빠른 시작

\`\`\`bash
# 저장소 루트에서 실행
docker-compose up -d

# Claude Code에서 MCP 서버 설정
# URL: http://localhost:3000
\`\`\`

## 환경 변수

| 변수 | 설명 |
|------|------|
| `GITHUB_TOKEN` | GitHub PAT (필수) |
| `REPOSITORY` | 대상 저장소 (필수) |
| `LOG_LEVEL` | 로그 레벨 |

## MCP 도구

- `create_pr_from_issue` - 이슈→PR 자동 생성
- `create_issue_from_claude` - Claude Code 이슈 생성
- `get_issue_details` - 이슈 조회
- `parse_contributing_md` - 규칙 파싱
```

- [ ] **Step 2: Commit**

```bash
git add mcp-wrapper/README.md
git commit -m "docs: MCP Wrapper README 작성

- 기능 설명
- 빠른 시작
- 환경 변수
- MCP 도구 목록

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>"
```

---

## Task 12: 통합 테스트 및 검증

**Files:**
- 테스트 실행 및 빌드 검증

- [ ] **Step 1: 모든 단위 테스트 실행**

```bash
cd mcp-wrapper
npm test
```

Expected: 모든 테스트 PASS

- [ ] **Step 2: 빌드 검증**

```bash
npm run build
```

Expected: dist/ 폴더 생성

- [ ] **Step 3: Docker 빌드 테스트**

```bash
cd ..
docker-compose build github-mcp-wrapper
```

Expected: 빌드 성공

- [ ] **Step 4: docker-compose 문법 최종 검증**

```bash
docker-compose config > /dev/null && echo "✅ 완료"
```

Expected: ✅ 완료

- [ ] **Step 5: Commit**

```bash
git commit -m "test: MCP Wrapper 통합 테스트 완료

- 단위 테스트 모두 성공
- TypeScript 빌드 성공
- Docker 빌드 성공
- docker-compose 검증 완료

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>" --allow-empty
```

---

## 완료 체크리스트

- [ ] Task 1: MCP Wrapper 프로젝트 초기화
- [ ] Task 2: 타입 정의
- [ ] Task 3: Logger 유틸리티
- [ ] Task 4: CONTRIBUTING.md 파서 (TDD)
- [ ] Task 5: 포매터 (TDD)
- [ ] Task 6: GitHub 클라이언트
- [ ] Task 7: PR & Issue Tools
- [ ] Task 8: Contributing Tools
- [ ] Task 9: MCP 서버 진입점
- [ ] Task 10: Docker 구성
- [ ] Task 11: README 작성
- [ ] Task 12: 통합 테스트
