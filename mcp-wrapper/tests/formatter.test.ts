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
