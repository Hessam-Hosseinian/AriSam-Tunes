#!/usr/bin/env sh
set -eu

ip route get 1.1.1.1 2>/dev/null | awk '{
  for (i = 1; i <= NF; i++) {
    if ($i == "src") {
      print $(i + 1)
      exit
    }
  }
}' | head -n 1
