import { GitHubIssue, BranchPattern } from '../types';
export declare class Formatter {
    private branchPattern;
    constructor(branchPattern: BranchPattern);
    generateBranchName(type: string, issueNumber: number, description: string): string;
    generatePRTitle(issueNumber: number, issueTitle: string): string;
    generatePRDescription(issue: GitHubIssue): string;
    isValidBranchType(type: string): boolean;
}
//# sourceMappingURL=formatter.d.ts.map