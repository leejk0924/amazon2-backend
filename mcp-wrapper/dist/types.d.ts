export interface GitHubIssue {
    number: number;
    title: string;
    body: string;
    labels: string[];
    state: 'open' | 'closed';
    created_at: string;
    updated_at: string;
    html_url?: string;
}
export interface GitHubPullRequest {
    number: number;
    title: string;
    body: string;
    head: {
        ref: string;
    };
    base: {
        ref: string;
    };
    state: 'open' | 'closed' | 'merged';
    created_at: string;
    updated_at: string;
    html_url?: string;
}
export interface ContributingRules {
    commitTypes: CommitType[];
    branchPattern: BranchPattern;
    prTemplate: string;
}
export interface CommitType {
    type: string;
    description: string;
    example: string;
}
export interface BranchPattern {
    format: string;
    types: string[];
}
export interface CreatePRInput {
    issueNumber: number;
    currentBranch: string;
}
export interface CreatePROutput {
    prNumber: number;
    prUrl: string;
    branchName: string;
    title: string;
}
export interface CreateIssueInput {
    title: string;
    body: string;
    issueType: 'feature' | 'bug' | 'docs' | 'refactor';
}
export interface CreateIssueOutput {
    issueNumber: number;
    issueUrl: string;
    labels: string[];
}
export interface Logger {
    info(message: string): void;
    error(message: string, error?: Error): void;
    debug(message: string): void;
    warn(message: string): void;
}
//# sourceMappingURL=types.d.ts.map