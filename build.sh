#!/bin/sh

# https://github.com/SessionHu/psb4j

psb4j_args="--extra-packin ./README.md,./LICENSE --jar ./build/sessverhttp.jar --clear"

if [ -s ~/.sessx/lib/psb4j.jar ]; then
    java -jar ~/.sessx/lib/psb4j.jar ${psb4j_args}
else
    if [ -s ./psb4j.jar ] ; then
        java -jar ./psb4j.jar ${psb4j_args}
    else
        wget https://github.com/SessionHu/psb4j/releases/latest/download/psb4j.jar -O ~/.sessx/lib/psb4j.jar
        java -jar ~/.sessx/lib/psb4j.jar ${psb4j_args}
    fi
fi
