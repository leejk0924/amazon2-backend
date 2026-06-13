import { GitHubIssue, CreateIssueOutput } from '../types';
export declare class GitHubClient {
    private api;
    private repo;
    constructor(token: string, repo: string);
    getIssue(issueNumber: number): Promise<GitHubIssue>;
    createIssue(title: string, body: string, labels: string[]): Promise<CreateIssueOutput>;
    createPullRequest(headBranch: string, baseBranch: string, title: string, body: string): Promise<{
        prNumber: number;
        prUrl: string;
    }>;
}
//# sourceMappingURL=github-client.d.ts.map