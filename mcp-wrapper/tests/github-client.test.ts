import { GitHubClient } from '../src/services/github-client';
import axios from 'axios';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('GitHubClient', () => {
  const token = 'test-token';
  const repo = 'jk/amazon2-backend';
  let client: GitHubClient;
  let mockApiInstance: any;

  beforeEach(() => {
    // axios.create의 반환값을 mock
    mockApiInstance = {
      get: jest.fn(),
      post: jest.fn()
    };
    (mockedAxios.create as jest.Mock).mockReturnValue(mockApiInstance);

    client = new GitHubClient(token, repo);
    jest.clearAllMocks();
  });

  test('이슈를 조회한다', async () => {
    mockApiInstance.get.mockResolvedValue({
      data: {
        number: 5,
        title: '테스트',
        body: '테스트 본문',
        labels: [],
        state: 'open',
        created_at: '2026-06-13',
        updated_at: '2026-06-13'
      }
    });

    const issue = await client.getIssue(5);
    expect(issue.number).toBe(5);
    expect(issue.title).toBe('테스트');
  });

  test('이슈를 생성한다', async () => {
    mockApiInstance.post.mockResolvedValue({
      data: {
        number: 42,
        title: '새 이슈',
        html_url: 'https://github.com/jk/amazon2-backend/issues/42',
        labels: ['feature']
      }
    });

    const result = await client.createIssue('새 이슈', '본문', ['feature']);
    expect(result.issueNumber).toBe(42);
  });

  test('PR을 생성한다', async () => {
    mockApiInstance.post.mockResolvedValue({
      data: {
        number: 25,
        title: 'PR 제목',
        html_url: 'https://github.com/jk/amazon2-backend/pull/25',
        head: { ref: 'feature/#5-test' }
      }
    });

    const result = await client.createPullRequest(
      'feature/#5-test',
      'main',
      'PR 제목',
      'PR 본문'
    );
    expect(result.prNumber).toBe(25);
  });
});
