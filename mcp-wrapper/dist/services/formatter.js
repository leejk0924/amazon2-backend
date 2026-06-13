"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Formatter = void 0;
const logger_1 = require("../utils/logger");
class Formatter {
    constructor(branchPattern) {
        this.branchPattern = branchPattern;
    }
    generateBranchName(type, issueNumber, description) {
        if (!this.isValidBranchType(type)) {
            logger_1.logger.warn(`유효하지 않은 branch 타입: ${type}`);
            type = 'feature';
        }
        // description 정규화: 공백을 하이픈으로, 특수문자 제거 (한글/영문/숫자/하이픈만 유지)
        const normalizedDesc = description
            .replace(/\s+/g, '-')
            .replace(/[^a-z0-9a-zA-Z\-가-힣]/g, '');
        return `${type}/#${issueNumber}-${normalizedDesc}`;
    }
    generatePRTitle(issueNumber, issueTitle) {
        return `#${issueNumber}: ${issueTitle}`;
    }
    generatePRDescription(issue) {
        return `## 개요
${issue.body || issue.title}

## 변경사항
- 이슈 #${issue.number}의 구현 내용

## 테스트 방법
- 해당 기능을 테스트했습니다

## 관련 이슈
Closes #${issue.number}`;
    }
    isValidBranchType(type) {
        return this.branchPattern.types.includes(type);
    }
}
exports.Formatter = Formatter;
//# sourceMappingURL=formatter.js.map