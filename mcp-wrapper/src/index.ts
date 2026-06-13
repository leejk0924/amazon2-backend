import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { ListToolsRequestSchema, CallToolRequestSchema } from '@modelcontextprotocol/sdk/types.js';
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

const server = new Server(
  {
    name: 'amazon2-git-mcp',
    version: '1.0.0',
  },
  {
    capabilities: {
      tools: {}
    }
  }
);

const prTools = createPRTools(githubClient, formatter);
const issueTools = createIssueTools(githubClient);
const contributingTools = createContributingTools(parser);

const allTools = [...prTools, ...issueTools, ...contributingTools];

server.setRequestHandler(ListToolsRequestSchema, async () => {
  logger.debug(`${allTools.length}개의 도구 반환`);
  return { tools: allTools };
});

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;
  logger.info(`도구 실행: ${name}`);

  try {
    switch (name) {
      case 'create_pr_from_issue': {
        const input = (args || {}) as unknown as CreatePRInput;
        const result = await handleCreatePRFromIssue(githubClient, formatter, input);
        return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
      }
      case 'create_issue_from_claude': {
        const input = (args || {}) as unknown as CreateIssueInput;
        const result = await handleCreateIssueFromClaude(githubClient, input);
        return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
      }
      case 'get_issue_details': {
        const { issueNumber } = args as { issueNumber: number };
        const result = await handleGetIssueDetails(githubClient, issueNumber);
        return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
      }
      case 'parse_contributing_md': {
        const result = await handleParseContributingMd(parser);
        return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
      }
      default:
        return { content: [{ type: 'text', text: `알 수 없는 도구: ${name}` }], isError: true };
    }
  } catch (error) {
    logger.error(`도구 실행 실패: ${name}`, error as Error);
    return { content: [{ type: 'text', text: `도구 실행 실패: ${(error as Error).message}` }], isError: true };
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
