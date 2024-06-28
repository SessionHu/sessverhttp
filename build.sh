#!/bin/sh

# https://github.com/SessionHu/psb4j

psb4j_args="--extra-packin ./README.md,./LICENSE --jar ./build/sessverhttp.jar --clear"
psb4j_jar_path="${HOME}/.sessx/lib/psb4j.jar"

run_psb4j() {
    java -jar $1 ${psb4j_args} || echo "Error: Failed to run psb4j with JAR $1"
}

if [ -s ${psb4j_jar_path} ]; then
    run_psb4j ${psb4j_jar_path}
else
    if [ -s ./psb4j.jar ]; then
        run_psb4j ./psb4j.jar
    else
        mkdir -p ${HOME}/.sessx/lib
        wget https://github.com/SessionHu/psb4j/releases/latest/download/psb4j.jar -O ${psb4j_jar_path} && \
        run_psb4j ${psb4j_jar_path} || echo "Error: Failed to download psb4j.jar"
    fi
fi
