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
exports.ContributingParser = void 0;
const fs = __importStar(require("fs"));
const logger_1 = require("../utils/logger");
class ContributingParser {
    constructor(filePath) {
        this.rules = null;
        try {
            this.content = fs.readFileSync(filePath, 'utf-8');
        }
        catch (error) {
            logger_1.logger.error(`CONTRIBUTING.md를 읽을 수 없습니다: ${filePath}`, error);
            this.content = '';
        }
    }
    // 커밋 타입을 테이블에서 추출
    getCommitTypes() {
        if (!this.content) {
            return [];
        }
        // 테이블에서 타입 추출 (| `Type` | 설명 | 예시 |)
        // 정규식: | `타입` | 설명 | 예시 |
        const typeRegex = /\|\s*`(\w+)`\s*\|\s*([^|]+)\s*\|/g;
        const types = [];
        let match;
        while ((match = typeRegex.exec(this.content)) !== null) {
            // 헤더 행(타입, 설명, 예시)은 스킵
            if (match[1] === '타입' || match[1] === 'Type') {
                continue;
            }
            types.push({
                type: match[1],
                description: match[2].trim(),
                example: `${match[1]}: 예시`
            });
        }
        return types;
    }
    // 브랜치 패턴 추출
    getBranchPattern() {
        // 브랜치 패턴 추출 (<type>/<이슈번호>-<설명>)
        const patternRegex = /```\n(<type>\/[^\n`]+)\n```/;
        const match = this.content.match(patternRegex);
        const format = match ? match[1] : 'feature/#<issue>-<description>';
        // 브랜치 타입 추출 (- `feature/` :)
        const typeRegex = /- `(\w+)\/`\s*:/g;
        const types = [];
        let typeMatch;
        while ((typeMatch = typeRegex.exec(this.content)) !== null) {
            types.push(typeMatch[1]);
        }
        return {
            format,
            types: types.length > 0 ? types : ['feature', 'fix', 'docs', 'refactor']
        };
    }
    // PR 템플릿 생성
    getPRTemplate() {
        return `## 개요
이 PR이 무엇을 구현하는지 간단히 설명

## 변경사항
- 주요 변경 사항 1
- 주요 변경 사항 2

## 테스트 방법
- 어떻게 테스트했는지
- 스크린샷/로그 필요시 첨부

## 관련 이슈
Closes #`;
    }
    // 모든 규칙을 캐시하여 반환
    getRules() {
        if (!this.rules) {
            this.rules = {
                commitTypes: this.getCommitTypes(),
                branchPattern: this.getBranchPattern(),
                prTemplate: this.getPRTemplate()
            };
        }
        return this.rules;
    }
}
exports.ContributingParser = ContributingParser;
//# sourceMappingURL=contributing-parser.js.map