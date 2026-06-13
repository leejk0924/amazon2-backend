"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.GitHubClient = void 0;
const axios_1 = __importDefault(require("axios"));
const logger_1 = require("../utils/logger");
class GitHubClient {
    constructor(token, repo) {
        this.api = axios_1.default.create({
            baseURL: 'https://api.github.com',
            headers: {
                Authorization: `Bearer ${token}`,
                Accept: 'application/vnd.github.v3+json'
            }
        });
        this.repo = repo;
    }
    async getIssue(issueNumber) {
        try {
            const response = await this.api.get(`/repos/${this.repo}/issues/${issueNumber}`);
            return response.data;
        }
        catch (error) {
            logger_1.logger.error(`이슈 #${issueNumber} 조회 실패`, error);
            throw error;
        }
    }
    async createIssue(title, body, labels) {
        try {
            const response = await this.api.post(`/repos/${this.repo}/issues`, {
                title,
                body,
                labels
            });
            return {
                issueNumber: response.data.number,
                issueUrl: response.data.html_url || `https://github.com/${this.repo}/issues/${response.data.number}`,
                labels: response.data.labels
            };
        }
        catch (error) {
            logger_1.logger.error(`이슈 생성 실패: ${title}`, error);
            throw error;
        }
    }
    async createPullRequest(headBranch, baseBranch, title, body) {
        try {
            const response = await this.api.post(`/repos/${this.repo}/pulls`, {
                head: headBranch,
                base: baseBranch,
                title,
                body
            });
            return {
                prNumber: response.data.number,
                prUrl: response.data.html_url || `https://github.com/${this.repo}/pull/${response.data.number}`
            };
        }
        catch (error) {
            logger_1.logger.error(`PR 생성 실패: ${title}`, error);
            throw error;
        }
    }
}
exports.GitHubClient = GitHubClient;
//# sourceMappingURL=github-client.js.map