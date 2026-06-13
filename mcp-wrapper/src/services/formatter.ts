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

    // description 정규화: 공백을 하이픈으로, 특수문자 제거 (한글/영문/숫자/하이픈만 유지)
    const normalizedDesc = description
      .replace(/\s+/g, '-')
      .replace(/[^a-z0-9a-zA-Z\-가-힣]/g, '');

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
