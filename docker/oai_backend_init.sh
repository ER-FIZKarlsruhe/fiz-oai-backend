#!/bin/bash

if [ -n "$PROXY" ]; then
  ENV HTTPS_PROXY=proxy.fiz-karlsruhe.de:8888
  ENV HTTP_PROXY=proxy.fiz-karlsruhe.de:8888
  ENV https_proxy=proxy.fiz-karlsruhe.de:8888
  ENV http_proxy=proxy.fiz-karlsruhe.de:8888
  RUN echo "Acquire::http::Proxy \"http://proxy.fiz-karlsruhe.de:8888/\"; Acquire::https::Proxy \"http://proxy.fiz-karlsruhe.de:8888/\";" > /etc/apt/apt.conf && \
fi

apt-get update -y && apt-get upgrade -y
apt-get -y install cron
apt-get -y install sudo
apt-get -y install vim
apt-get clean
rm -rf /var/lib/apt/lists/*
chmod +x /usr/local/bin/oai_backend_init.sh

set -e

echo -e "\
#0 1 * * * find /usr/local/some/logs -type f -ctime +200 -delete\n\
0 2 * * * find /usr/local/tomcat/logs -type f -ctime +60 -delete\
" | crontab -

crontab -l
cron
