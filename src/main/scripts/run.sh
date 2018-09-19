#!/bin/bash

JAVA_EXECUTABLE="`which java`"

DIST_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
LIB_DIR="$DIST_DIR/lib"
CONF_DIR="$DIST_DIR/conf"

APP_CLASSPATH=$CONF_DIR
for i in `ls $LIB_DIR/*.jar`
do
  APP_CLASSPATH=${THE_CLASSPATH}:${i}
done

echo "Starting Fast DNS Filter."

$JAVA_EXECUTABLE -Xms64M -Xmx256M -cp "$CONF_DIR:$LIB_DIR/${project.build.finalName}.${project.packaging}" \
net.yam.fastdnsfilter.App

