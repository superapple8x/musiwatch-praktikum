#!/usr/bin/env bash
# logrun.sh — run a command supplied on stdin, append command + output + exit
# status to an append-only phase log, and still show output to the caller.
#
# Usage:
#   ./logrun.sh "label" <<'CMD'
#   ...commands...
#   CMD
#
# Env:
#   LOG_FILE  full path to the log (default: $PWD/logs/$PHASE/shell-history.txt)
#   LOG_DIR   directory for the default log target
#   PHASE     phase label used in the default path (default: today)
set -u

LABEL="${1:-unnamed}"
PHASE="${PHASE:-$(date +%F)}"
LOG_DIR="${LOG_DIR:-$PWD/logs/$PHASE}"
LOG_FILE="${LOG_FILE:-$LOG_DIR/shell-history.txt}"

mkdir -p "$(dirname "$LOG_FILE")"
CMD="$(cat)"

# Header goes straight to the file (not stdout) so the caller only sees output.
{
  echo
  echo "### $(date -Is) | $LABEL"
  echo "\$ bash <<'CMD'"
  printf '%s\n' "$CMD"
  echo "CMD"
} >> "$LOG_FILE"

bash <<<"$CMD" 2>&1 | tee -a "$LOG_FILE"
rc=${PIPESTATUS[0]}

echo "rc=$rc" | tee -a "$LOG_FILE"
exit "$rc"
