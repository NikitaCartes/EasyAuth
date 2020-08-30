#!/bin/bash

# Setting the variables
projectId="$(grep 'archives_base_name' gradle.properties | sed 's/archives_base_name = //g;s/ //g;s/,//g;s/"//g')"
version="$(grep 'mod_version' gradle.properties | grep -o '[0-9]*\.[0-9]*\.[0-9]*')"
mcVersion="$(grep 'minecraft_version' gradle.properties | sed 's/minecraft_version=//g;s/ //g;s/,//g;s/"//g')"

cd ./build/libs || exit

buildName="$projectId-$version-devbuild_$GITHUB_RUN_NUMBER-MC_$mcVersion"

echo "Build is going to be renamed: $buildName.jar"
# Renaming the dev build
mv "$projectId-$version-$mcVersion.jar" "$buildName.jar"

#Setting the buildname for GH actions
echo "::set-env name=BUILDNAME::$buildName"
