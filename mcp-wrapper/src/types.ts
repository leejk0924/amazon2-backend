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
