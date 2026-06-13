"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
const index_js_1 = require("@modelcontextprotocol/sdk/server/index.js");
const stdio_js_1 = require("@modelcontextprotocol/sdk/server/stdio.js");
const types_js_1 = require("@modelcontextprotocol/sdk/types.js");
const dotenv = __importStar(require("dotenv"));
const contributing_parser_1 = require("./services/contributing-parser");
const formatter_1 = require("./services/formatter");
const github_client_1 = require("./services/github-client");
const pr_tools_1 = require("./tools/pr-tools");
const issue_tools_1 = require("./tools/issue-tools");
const contributing_tools_1 = require("./tools/contributing-tools");
const logger_1 = require("./utils/logger");
dotenv.config();
const GITHUB_TOKEN = process.env.GITHUB_TOKEN;
const REPOSITORY = process.env.REPOSITORY;
const CONTRIBUTING_MD_PATH = process.env.CONTRIBUTING_MD_PATH || './docs/CONTRIBUTING.md';
if (!GITHUB_TOKEN || !REPOSITORY) {
    logger_1.logger.error('필수 환경 변수 누락: GITHUB_TOKEN, REPOSITORY');
    process.exit(1);
}
const parser = new contributing_parser_1.ContributingParser(CONTRIBUTING_MD_PATH);
const rules = parser.getRules();
const formatter = new formatter_1.Formatter(rules.branchPattern);
const githubClient = new github_client_1.GitHubClient(GITHUB_TOKEN, REPOSITORY);
const server = new index_js_1.Server({
    name: 'amazon2-git-mcp',
    version: '1.0.0',
}, {
    capabilities: {
        tools: {}
    }
});
const prTools = (0, pr_tools_1.createPRTools)(githubClient, formatter);
const issueTools = (0, issue_tools_1.createIssueTools)(githubClient);
const contributingTools = (0, contributing_tools_1.createContributingTools)(parser);
const allTools = [...prTools, ...issueTools, ...contributingTools];
server.setRequestHandler(types_js_1.ListToolsRequestSchema, async () => {
    logger_1.logger.debug(`${allTools.length}개의 도구 반환`);
    return { tools: allTools };
});
server.setRequestHandler(types_js_1.CallToolRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    logger_1.logger.info(`도구 실행: ${name}`);
    try {
        switch (name) {
            case 'create_pr_from_issue': {
                const input = (args || {});
                const result = await (0, pr_tools_1.handleCreatePRFromIssue)(githubClient, formatter, input);
                return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
            }
            case 'create_issue_from_claude': {
                const input = (args || {});
                const result = await (0, issue_tools_1.handleCreateIssueFromClaude)(githubClient, input);
                return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
            }
            case 'get_issue_details': {
                const { issueNumber } = args;
                const result = await (0, issue_tools_1.handleGetIssueDetails)(githubClient, issueNumber);
                return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
            }
            case 'parse_contributing_md': {
                const result = await (0, contributing_tools_1.handleParseContributingMd)(parser);
                return { content: [{ type: 'text', text: JSON.stringify(result, null, 2) }] };
            }
            default:
                return { content: [{ type: 'text', text: `알 수 없는 도구: ${name}` }], isError: true };
        }
    }
    catch (error) {
        logger_1.logger.error(`도구 실행 실패: ${name}`, error);
        return { content: [{ type: 'text', text: `도구 실행 실패: ${error.message}` }], isError: true };
    }
});
async function main() {
    const transport = new stdio_js_1.StdioServerTransport();
    await server.connect(transport);
    logger_1.logger.info('Amazon2 Git MCP Server 시작');
    logger_1.logger.info(`저장소: ${REPOSITORY}`);
    logger_1.logger.info(`CONTRIBUTING.md: ${CONTRIBUTING_MD_PATH}`);
    logger_1.logger.info(`등록된 도구: ${allTools.length}개`);
}
main().catch((error) => {
    logger_1.logger.error('서버 시작 실패', error);
    process.exit(1);
});
//# sourceMappingURL=index.js.map