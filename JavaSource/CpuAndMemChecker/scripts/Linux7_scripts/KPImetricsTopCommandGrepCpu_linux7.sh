#!/bin/sh
# Linux 7 command and format
#    Instructions: Modify CIS_HOME variable
if [ "$CIS_HOME" == "" ];
then
   CIS_HOME=/comp/composite/CIS_7.0
   export CIS_HOME
fi
top -b -n 4 | grep Cpu | sed -f $CIS_HOME/bin/KPImetricsCpuFormat_linux7
exit 0
