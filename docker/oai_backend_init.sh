#!/bin/bash

set -e

echo -e "\
#0 1 * * * find /usr/local/some/logs -type f -ctime +200 -delete\n\
0 2 * * * find /usr/local/tomcat/logs -type f -ctime +60 -delete\
" | crontab -

crontab -l
cron
tail -f /dev/null
