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
      logger.error(
        `CONTRIBUTING.md를 읽을 수 없습니다: ${filePath}`,
        error as Error
      );
      this.content = '';
    }
  }

  // 커밋 타입을 테이블에서 추출
  getCommitTypes(): CommitType[] {
    if (!this.content) {
      return [];
    }

    // 테이블에서 타입 추출 (| `Type` | 설명 | 예시 |)
    // 정규식: | `타입` | 설명 | 예시 |
    const typeRegex = /\|\s*`(\w+)`\s*\|\s*([^|]+)\s*\|/g;
    const types: CommitType[] = [];
    let match;

    while ((match = typeRegex.exec(this.content)) !== null) {
      // 헤더 행(타입, 설명, 예시)은 스킵
      if (match[1] === '타입' || match[1] === 'Type') {
        continue;
      }

      types.push({
        type: match[1],
        description: match[2].trim(),
        example: `${match[1]}: 예시`
      });
    }

    return types;
  }

  // 브랜치 패턴 추출
  getBranchPattern(): BranchPattern {
    // 브랜치 패턴 추출 (<type>/<이슈번호>-<설명>)
    const patternRegex = /```\n(<type>\/[^\n`]+)\n```/;
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

  // PR 템플릿 생성
  getPRTemplate(): string {
    return `## 개요
이 PR이 무엇을 구현하는지 간단히 설명

## 변경사항
- 주요 변경 사항 1
- 주요 변경 사항 2

## 테스트 방법
- 어떻게 테스트했는지
- 스크린샷/로그 필요시 첨부

## 관련 이슈
Closes #`;
  }

  // 모든 규칙을 캐시하여 반환
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
