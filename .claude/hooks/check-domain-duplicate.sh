#!/usr/bin/env bash
set -euo pipefail

input=$(cat)
file_path=$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty')

if [ -z "$file_path" ]; then
  exit 0
fi

if [[ ! "$file_path" =~ /src/main/java/com/jk/amazon2/([^/]+)/entity/[^/]+\.java$ ]]; then
  exit 0
fi

domain="${BASH_REMATCH[1]}"

if [ -e "$file_path" ]; then
  exit 0
fi

domain_dir="${file_path%/entity/*}"
existing=$(find "$domain_dir" -type f 2>/dev/null || true)

if [ -n "$existing" ]; then
  reason=$(printf '도메인 "%s" 디렉토리(%s)에 이미 파일이 존재합니다:\n%s\n\n사용자에게 목록을 보고하고 병합/교체/취소 여부를 확인받은 후에만 계속 진행하세요.' "$domain" "$domain_dir" "$existing")
  jq -n --arg reason "$reason" '{hookSpecificOutput:{hookEventName:"PreToolUse",permissionDecision:"deny",permissionDecisionReason:$reason}}'
fi

exit 0
