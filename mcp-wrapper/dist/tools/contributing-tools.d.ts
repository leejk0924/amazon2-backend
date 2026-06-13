import { Tool } from '@modelcontextprotocol/sdk/types.js';
import { ContributingParser } from '../services/contributing-parser';
/**
 * CONTRIBUTING.md를 파싱하는 MCP 도구들을 생성합니다.
 * @param parser ContributingParser 인스턴스 (타입 정의를 위해 필요)
 * @returns MCP Tool 배열
 */
export declare function createContributingTools(parser: ContributingParser): Tool[];
/**
 * CONTRIBUTING.md 파싱 도구의 핸들러
 * @param parser ContributingParser 인스턴스
 * @returns 파싱된 규칙 정보
 */
export declare function handleParseContributingMd(parser: ContributingParser): Promise<any>;
//# sourceMappingURL=contributing-tools.d.ts.map