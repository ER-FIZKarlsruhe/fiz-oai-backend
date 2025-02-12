#!/bin/bash

if [ -n "$PROXY" ]; then
  export HTTPS_PROXY=$PROXY
  export HTTP_PROXY=$PROXY
  export https_proxy=$PROXY
  export http_proxy=$PROXY
  echo 'Acquire::http::Proxy \"$PROXY\"; Acquire::https::Proxy \"$PROXY\";' > /etc/apt/apt.conf
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

