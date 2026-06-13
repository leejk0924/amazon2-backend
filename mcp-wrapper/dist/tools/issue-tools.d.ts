import { Tool } from '@modelcontextprotocol/sdk/types.js';
import { GitHubClient } from '../services/github-client';
import { CreateIssueInput, CreateIssueOutput, GitHubIssue } from '../types';
/**
 * 이슈 관련 도구 정의
 * @param _githubClient GitHub API 클라이언트
 * @returns Tool 배열
 */
export declare function createIssueTools(_githubClient: GitHubClient): Tool[];
/**
 * Claude Code에서 이슈 생성 처리
 * @param githubClient GitHub API 클라이언트
 * @param input 이슈 생성 입력 파라미터
 * @returns 이슈 생성 결과
 */
export declare function handleCreateIssueFromClaude(githubClient: GitHubClient, input: CreateIssueInput): Promise<CreateIssueOutput>;
/**
 * 이슈 상세 정보 조회 처리
 * @param githubClient GitHub API 클라이언트
 * @param issueNumber 이슈 번호
 * @returns 이슈 상세 정보
 */
export declare function handleGetIssueDetails(githubClient: GitHubClient, issueNumber: number): Promise<GitHubIssue>;
//# sourceMappingURL=issue-tools.d.ts.map