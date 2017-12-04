#!/bin/sh 
# Linux 7 command and format
export CACHE_INFO1=`free -m | grep Mem | awk '{print   $6}'`
export CACHE_INFO2=`free -m | grep Mem | awk '{print   $7}'`
echo "-/+ buffers/cache:     $CACHE_INFO1    $CACHE_INFO2"
exit 0
