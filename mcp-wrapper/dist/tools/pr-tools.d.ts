import { Tool } from '@modelcontextprotocol/sdk/types.js';
import { GitHubClient } from '../services/github-client';
import { Formatter } from '../services/formatter';
import { CreatePRInput, CreatePROutput } from '../types';
/**
 * PR 생성 도구 정의
 * @param githubClient GitHub API 클라이언트 (타입 정의를 위해 필요)
 * @param formatter 포매터 (타입 정의를 위해 필요)
 * @returns Tool 배열
 */
export declare function createPRTools(githubClient: GitHubClient, formatter: Formatter): Tool[];
/**
 * 이슈에서 PR 생성 처리
 * @param githubClient GitHub API 클라이언트
 * @param formatter 포매터
 * @param input PR 생성 입력 파라미터
 * @returns PR 생성 결과
 */
export declare function handleCreatePRFromIssue(githubClient: GitHubClient, formatter: Formatter, input: CreatePRInput): Promise<CreatePROutput>;
//# sourceMappingURL=pr-tools.d.ts.map