import { ContributingRules, CommitType, BranchPattern } from '../types';
export declare class ContributingParser {
    private content;
    private rules;
    constructor(filePath: string);
    getCommitTypes(): CommitType[];
    getBranchPattern(): BranchPattern;
    getPRTemplate(): string;
    getRules(): ContributingRules;
}
//# sourceMappingURL=contributing-parser.d.ts.map