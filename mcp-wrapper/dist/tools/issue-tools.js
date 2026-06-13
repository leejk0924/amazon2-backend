"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.createIssueTools = createIssueTools;
exports.handleCreateIssueFromClaude = handleCreateIssueFromClaude;
exports.handleGetIssueDetails = handleGetIssueDetails;
const logger_1 = require("../utils/logger");
const CLAUDE_CODE_LABEL = process.env.CLAUDE_CODE_LABEL || 'claude-code-generated';
/**
 * 이슈 관련 도구 정의
 * @param _githubClient GitHub API 클라이언트
 * @returns Tool 배열
 */
function createIssueTools(_githubClient) {
    return [
        {
            name: 'create_issue_from_claude',
            description: 'Claude Code에서 발견한 버그나 기능을 GitHub 이슈로 생성합니다.',
            inputSchema: {
                type: 'object',
                properties: {
                    title: {
                        type: 'string',
                        description: '이슈 제목'
                    },
                    body: {
                        type: 'string',
                        description: '이슈 본문'
                    },
                    issueType: {
                        type: 'string',
                        enum: ['feature', 'bug', 'docs', 'refactor'],
                        description: '이슈 유형'
                    }
                },
                required: ['title', 'body', 'issueType']
            }
        },
        {
            name: 'get_issue_details',
            description: '이슈 번호로 상세 정보를 조회합니다.',
            inputSchema: {
                type: 'object',
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
/**
 * Claude Code에서 이슈 생성 처리
 * @param githubClient GitHub API 클라이언트
 * @param input 이슈 생성 입력 파라미터
 * @returns 이슈 생성 결과
 */
async function handleCreateIssueFromClaude(githubClient, input) {
    try {
        logger_1.logger.info(`Claude Code 이슈 생성 시작: ${input.title}`);
        // 라벨 설정
        const labels = [CLAUDE_CODE_LABEL, input.issueType];
        logger_1.logger.debug(`라벨 설정: ${labels.join(', ')}`);
        // 이슈 생성
        const result = await githubClient.createIssue(input.title, input.body, labels);
        logger_1.logger.info(`이슈 #${result.issueNumber} 생성 완료: ${result.issueUrl}`);
        return result;
    }
    catch (error) {
        logger_1.logger.error(`이슈 생성 실패`, error);
        throw error;
    }
}
/**
 * 이슈 상세 정보 조회 처리
 * @param githubClient GitHub API 클라이언트
 * @param issueNumber 이슈 번호
 * @returns 이슈 상세 정보
 */
async function handleGetIssueDetails(githubClient, issueNumber) {
    try {
        logger_1.logger.info(`이슈 #${issueNumber} 상세 정보 조회`);
        const issue = await githubClient.getIssue(issueNumber);
        logger_1.logger.debug(`이슈 조회 완료: ${issue.title}`);
        return issue;
    }
    catch (error) {
        logger_1.logger.error(`이슈 조회 실패`, error);
        throw error;
    }
}
//# sourceMappingURL=issue-tools.js.map