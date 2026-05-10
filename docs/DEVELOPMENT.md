# 로컬 개발 가이드 (Local Development Guide)

이 문서는 프로젝트의 로컬 개발 환경 설정 및 유용한 팁을 제공합니다.

---

## 1. 로컬 변경사항 관리 (Managing Local Changes)

때로는 커밋하지 않고 임시로 변경사항을 저장해야 할 때가 있습니다. `git stash`는 좋은 방법이지만, 여러 변경사항을 관리하기에는 불편할 수 있습니다.

이 프로젝트에서는 `.gitignore`에 등록된 `patches/` 디렉토리를 사용하여 로컬 변경사항을 파일로 관리하는 방법을 권장합니다.

### 목적

- `git stash`의 복잡함을 피하고, 여러 변경사항을 이름이 있는 파일로 명확하게 관리합니다.
- 아직 커밋하기에는 이르지만, 다른 브랜치로 전환하기 전에 현재 작업을 백업하고 싶을 때 유용합니다.

### 사용 방법

#### 1. 패치 파일 생성하기 (현재 변경사항 저장)

현재 작업 디렉토리의 변경사항(Staged되지 않은 변경사항)을 `.patch` 파일로 저장합니다.

```bash
git diff > patches/my-feature-in-progress.patch
```

> **팁:** 패치 파일의 이름은 작업 내용을 알 수 있도록 명확하게 짓는 것이 좋습니다. (예: `refactor-member-service.patch`)

#### 2. 패치 파일 적용하기 (저장한 변경사항 불러오기)

저장해둔 패치 파일을 현재 작업 디렉토리에 적용합니다.

```bash
git apply patches/my-feature-in-progress.patch
```

#### 3. 패치 적용 취소하기

적용했던 패치 파일의 내용을 다시 원래대로 되돌립니다.

```bash
git apply -R patches/my-feature-in-progress.patch
```

### GitHub Gist를 활용한 패치 공유

로컬 변경사항을 다른 개발자와 공유하거나, 여러 기기에서 동기화하고 싶을 때는 GitHub Gist를 활용할 수 있습니다.

1.  **Gist 생성:** [GitHub Gist](https://gist.github.com/)에서 새 Gist를 생성합니다.
2.  **패치 파일 업로드:** 생성한 `.patch` 파일의 내용을 복사하여 Gist에 붙여넣거나 파일을 업로드합니다.
3.  **패치 적용:** Gist의 'Raw' 버튼을 클릭하여 URL을 복사한 후, 아래 명령어로 적용합니다.
    *   **주의:** Gist URL 뒤에 `/raw/<파일명>`까지 포함된 주소를 사용해야 합니다. (HTML 페이지가 아닌 순수 파일 내용을 가져오기 위함)

```bash
# 예시: curl -L https://gist.github.com/user/gist_id/raw/filename.patch | git apply
curl -L <Gist Raw URL>/raw/<filename.patch> | git apply
```

### 주의사항

- `patches/` 디렉토리는 `.gitignore`에 등록되어 있으므로, 이 디렉토리의 파일들은 원격 저장소에 푸시되지 않습니다. **개인 로컬 환경에서만 사용해야 합니다.**
- 패치 파일은 생성 시점의 코드 상태에 의존적입니다. 원본 코드가 많이 변경되면 패치가 적용되지 않을 수 있습니다.
- 컨플릭트(Conflict)가 발생하면 `git apply`는 실패하며, 수동으로 해결해야 합니다.

---

## 2. Git Worktree 활용 (Working with Multiple Branches)

`git worktree`를 사용하면 하나의 저장소에서 여러 브랜치를 동시에 체크아웃하여 작업할 수 있습니다. 브랜치 전환(`git checkout`) 없이 핫픽스 처리나 코드 리뷰를 동시에 진행할 때 유용합니다.

### 사용 방법

#### 1. 새 워크트리 생성 (새 브랜치와 함께)

```bash
# git worktree add <path> <branch>
git worktree add ../amazon2-hotfix hotfix/urgent-bug-fix
```
위 명령어는 현재 프로젝트 상위 디렉토리에 `amazon2-hotfix`라는 폴더를 만들고, 그곳에 `hotfix/urgent-bug-fix` 브랜치를 체크아웃합니다.

#### 2. 기존 브랜치로 워크트리 생성

```bash
git worktree add ../amazon2-review feature/new-login
```

#### 3. 워크트리 목록 확인

```bash
git worktree list
```

#### 4. 워크트리 삭제

작업이 끝난 워크트리 디렉토리를 삭제하고, git 정보를 정리합니다.

```bash
# 1. 디렉토리 삭제
rm -rf ../amazon2-hotfix

# 2. 워크트리 정보 정리
git worktree prune
```

### 활용 팁

- **긴급 버그 수정:** 현재 작업 중인 내용을 `stash`하거나 커밋하지 않고, 별도 워크트리에서 핫픽스 브랜치를 띄워 수정 후 바로 푸시할 수 있습니다.
- **코드 리뷰:** PR이 올라온 브랜치를 별도 워크트리로 띄워놓고, 로컬에서 직접 실행해보며 리뷰할 수 있습니다.
- **의존성 격리:** 서로 다른 자바 버전이나 라이브러리 버전을 테스트해야 할 때 유용합니다.
