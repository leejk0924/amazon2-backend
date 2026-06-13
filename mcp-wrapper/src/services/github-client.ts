import axios, { AxiosInstance } from 'axios';
import { GitHubIssue, GitHubPullRequest, CreateIssueOutput } from '../types';
import { logger } from '../utils/logger';

export class GitHubClient {
  private api: AxiosInstance
  private repo: string;

  constructor(token: string, repo: string) {
    this.api = axios.create({
      baseURL: 'https://api.github.com',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'application/vnd.github.v3+json'
      }
    });
    this.repo = repo;
  }

  async getIssue(issueNumber: number): Promise<GitHubIssue> {
    try {
      const response = await this.api.get<GitHubIssue>(
        `/repos/${this.repo}/issues/${issueNumber}`
      );
      return response.data;
    } catch (error) {
      logger.error(`이슈 #${issueNumber} 조회 실패`, error as Error);
      throw error;
    }
  }

  async createIssue(
    title: string,
    body: string,
    labels: string[]
  ): Promise<CreateIssueOutput> {
    try {
      const response = await this.api.post<GitHubIssue>(
        `/repos/${this.repo}/issues`,
        {
          title,
          body,
          labels
        }
      );

      return {
        issueNumber: response.data.number,
        issueUrl: response.data.html_url || `https://github.com/${this.repo}/issues/${response.data.number}`,
        labels: response.data.labels
      };
    } catch (error) {
      logger.error(`이슈 생성 실패: ${title}`, error as Error);
      throw error;
    }
  }

  async createPullRequest(
    headBranch: string,
    baseBranch: string,
    title: string,
    body: string
  ): Promise<{ prNumber: number; prUrl: string }> {
    try {
      const response = await this.api.post<GitHubPullRequest>(
        `/repos/${this.repo}/pulls`,
        {
          head: headBranch,
          base: baseBranch,
          title,
          body
        }
      );

      return {
        prNumber: response.data.number,
        prUrl: response.data.html_url || `https://github.com/${this.repo}/pull/${response.data.number}`
      };
    } catch (error) {
      logger.error(`PR 생성 실패: ${title}`, error as Error);
      throw error;
    }
  }
}
