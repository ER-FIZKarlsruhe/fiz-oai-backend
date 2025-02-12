#!/bin/bash

if [ -n "$PROXY" ]; then
  export HTTPS_PROXY=proxy.fiz-karlsruhe.de:8888
  export HTTP_PROXY=proxy.fiz-karlsruhe.de:8888
  export https_proxy=proxy.fiz-karlsruhe.de:8888
  export http_proxy=proxy.fiz-karlsruhe.de:8888
  echo "Acquire::http::Proxy \"http://proxy.fiz-karlsruhe.de:8888/\"; Acquire::https::Proxy \"http://proxy.fiz-karlsruhe.de:8888/\";" > /etc/apt/apt.conf && \
fi

apt-get update -y && apt-get upgrade -y
apt-get -y install cron
apt-get -y install sudo
apt-get -y install vim
apt-get clean
rm -rf /var/lib/apt/lists/*

if [ -n "$LOGFILE_KEEP_DAYS" ]; then
  set -e
  echo -e '\
  #0 1 * * * find /usr/local/some/logs -type f -ctime +$LOGFILE_KEEP_DAYS -delete\n\
  0 2 * * * find /usr/local/tomcat/logs -type f -ctime +$LOGFILE_KEEP_DAYS -delete\
  ' | crontab -

  crontab -l
  cron
fi

