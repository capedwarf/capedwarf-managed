#!/bin/sh

#shortcut to boot WildFly with CapeDwarf configuration
#if you need to set other boot options, please use standalone.sh

DIRNAME=`dirname "$0"`
REALPATH=`cd "$DIRNAME/../bin"; pwd`

if [ -z "$1" ]; then
    ${DIRNAME}/standalone.sh -c standalone-capedwarf.xml
else
    cd "$1"
    ${REALPATH}/standalone.sh -c standalone-capedwarf.xml -DrootDeployment=$1
fi
