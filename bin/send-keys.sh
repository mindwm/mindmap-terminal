#!/usr/bin/env bash
TMUX_SESSION_NAME="$1"
shift
TMUX_PANE_ID="0"

tmux send-keys -t "${TMUX_SESSION_NAME}:${TMUX_PANE_ID}" "$*" 
sleep 0.2
tmux send-keys -t "${TMUX_SESSION_NAME}:${TMUX_PANE_ID}" "$(printf "\r")"
