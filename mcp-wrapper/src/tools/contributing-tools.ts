import { Tool } from '@modelcontextprotocol/sdk/types.js';
import { ContributingParser } from '../services/contributing-parser';
import { logger } from '../utils/logger';

/**
 * CONTRIBUTING.md를 파싱하는 MCP 도구들을 생성합니다.
 * @param parser ContributingParser 인스턴스 (타입 정의를 위해 필요)
 * @returns MCP Tool 배열
 */
export function createContributingTools(parser: ContributingParser): Tool[] {
  void parser;
  return [{
    name: 'parse_contributing_md',
    description: 'CONTRIBUTING.md에서 프로젝트의 규칙을 파싱합니다.',
    inputSchema: {
      type: 'object' as const,
      properties: {},
      required: []
    }
  }];
}

/**
 * CONTRIBUTING.md 파싱 도구의 핸들러
 * @param parser ContributingParser 인스턴스
 * @returns 파싱된 규칙 정보
 */
export async function handleParseContributingMd(parser: ContributingParser): Promise<any> {
  try {
    logger.info('CONTRIBUTING.md 파싱 시작');
    const rules = parser.getRules();
    logger.debug(`파싱 완료: 커밋 타입 ${rules.commitTypes.length}개, 브랜치 타입 ${rules.branchPattern.types.length}개`);

    return {
      success: true,
      data: rules
    };
  } catch (error) {
    logger.error('CONTRIBUTING.md 파싱 실패', error as Error);
    throw error;
  }
}
