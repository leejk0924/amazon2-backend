import { Tool } from '@modelcontextprotocol/sdk/types.js';
import { GitHubClient } from '../services/github-client';
import { Formatter } from '../services/formatter';
import { CreatePRInput, CreatePROutput } from '../types';
import { logger } from '../utils/logger';

/**
 * PR 생성 도구 정의
 * @param githubClient GitHub API 클라이언트 (타입 정의를 위해 필요)
 * @param formatter 포매터 (타입 정의를 위해 필요)
 * @returns Tool 배열
 */
export function createPRTools(githubClient: GitHubClient, formatter: Formatter): Tool[] {
  void githubClient;
  void formatter;
  return [{
    name: 'create_pr_from_issue',
    description: '이슈를 기반으로 PR을 자동 생성합니다.',
    inputSchema: {
      type: 'object' as const,
      properties: {
        issueNumber: {
          type: 'number',
          description: '이슈 번호'
        },
        currentBranch: {
          type: 'string',
          description: '현재 branch'
        }
      },
      required: ['issueNumber', 'currentBranch']
    }
  }];
}

/**
 * 이슈에서 PR 생성 처리
 * @param githubClient GitHub API 클라이언트
 * @param formatter 포매터
 * @param input PR 생성 입력 파라미터
 * @returns PR 생성 결과
 */
export async function handleCreatePRFromIssue(
  githubClient: GitHubClient,
  formatter: Formatter,
  input: CreatePRInput
): Promise<CreatePROutput> {
  try {
    logger.info(`이슈 #${input.issueNumber}에서 PR 생성 시작`);

    // 이슈 정보 조회
    const issue = await githubClient.getIssue(input.issueNumber);
    logger.debug(`이슈 조회 완료: ${issue.title}`);

    // Branch 명 생성
    const branchName = formatter.generateBranchName('feature', issue.number, issue.title);
    logger.debug(`Branch 명 생성: ${branchName}`);

    // PR 제목 생성
    const prTitle = formatter.generatePRTitle(issue.number, issue.title);
    logger.debug(`PR 제목 생성: ${prTitle}`);

    // PR 설명 생성
    const prDescription = formatter.generatePRDescription(issue);
    logger.debug(`PR 설명 생성 완료`);

    // PR 생성
    const prResult = await githubClient.createPullRequest(
      branchName,
      input.currentBranch,
      prTitle,
      prDescription
    );
    logger.info(`PR #${prResult.prNumber} 생성 완료: ${prResult.prUrl}`);

    return {
      prNumber: prResult.prNumber,
      prUrl: prResult.prUrl,
      branchName,
      title: prTitle
    };
  } catch (error) {
    logger.error(`PR 생성 실패`, error as Error);
    throw error;
  }
}
