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

| 타입         | 설명                          | 예시                       |
|------------|-----------------------------|--------------------------|
| \`Feat\`     | 새 기능 추가                     | \`Feat: 회원 조회 API 구현\`     |
| \`Fix\`      | 버그 수정                       | \`Fix: 요일 계산 오류 수정\`       |
| \`Docs\`     | 문서 작성/수정 (코드 제외)            | \`Docs: API 접속 링크 추가\`     |
| \`Refactor\` | 코드 구조 개선 (동작 변경 없음)         | \`Refactor: 중복 로직 추출\`     |

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
    expect(types).toHaveLength(4);
    expect(types).toContainEqual(
      expect.objectContaining({ type: 'Feat' })
    );
    expect(types).toContainEqual(
      expect.objectContaining({ type: 'Fix' })
    );
    expect(types[0].description).toContain('새 기능 추가');
  });

  test('branch 패턴을 추출한다', () => {
    const pattern = parser.getBranchPattern();
    expect(pattern.format).toContain('<type>');
    expect(pattern.format).toContain('<이슈번호>');
    expect(pattern.types).toContain('feature');
    expect(pattern.types).toContain('fix');
    expect(pattern.types).toContain('docs');
  });

  test('PR 템플릿을 생성한다', () => {
    const template = parser.getPRTemplate();
    expect(template).toBeTruthy();
    expect(template).toContain('개요');
    expect(template).toContain('변경사항');
    expect(template).toContain('테스트 방법');
    expect(template).toContain('관련 이슈');
  });

  test('rules를 캐시하여 반환한다', () => {
    const rules1 = parser.getRules();
    const rules2 = parser.getRules();
    expect(rules1).toBe(rules2); // 같은 객체 참조
    expect(rules1.commitTypes).toHaveLength(4);
    expect(rules1.branchPattern.types).toContain('feature');
  });

  test('존재하지 않는 파일일 때 빈 결과를 반환한다', () => {
    const invalidParser = new ContributingParser('/nonexistent/path/CONTRIBUTING.md');
    const types = invalidParser.getCommitTypes();
    const pattern = invalidParser.getBranchPattern();

    expect(types).toEqual([]);
    expect(pattern.types.length).toBeGreaterThan(0); // 기본값 반환
  });
});
