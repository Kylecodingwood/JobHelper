#!/bin/sh
htpasswd -bc /etc/nginx/.htpasswd "${APP_USERNAME:-admin}" "${APP_PASSWORD:-changeme}"
exec nginx -g "daemon off;"
