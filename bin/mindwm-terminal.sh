#!/usr/bin/env bash
SESSION_ID=$1
TMUX_SESSION_NAME="${SESSION_ID}"
#TMUX_SESSION_NAME="mindwm-terminal-`date +%s`"
WINDOW_TITLE="${TMUX_SESSION_NAME}"

#gnome-terminal -t "${WINDOW_TITLE}" -- sh -c "tmux new -s ${TMUX_SESSION_NAME}"
xterm -T ${TMUX_SESSION_NAME} -fa "Monospace" -fs 14 -e sh -c "tmux new -s ${TMUX_SESSION_NAME}" &
while :; do
    sleep 1
    tmux ls | grep "${TMUX_SESSION_NAME}" && break
done

tmux send-keys -t "${TMUX_SESSION_NAME}:0" "cd /home/bebebeka/git/mindwm-manager" c-m
tmux send-keys -t "${TMUX_SESSION_NAME}:0" 'dbus-send \
  --session \
  --dest=org.mindwm.client.manager \
  --type=method_call \
  /service \
  org.mindwm.client.manager.tmux_join \
  string:"$TMUX,$TMUX_PANE"' c-m

