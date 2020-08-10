#!/bin/bash

# Setting the variables
projectId="$(grep 'archives_base_name'  | sed 's/archives_base_name = //g;s/ //g;s/,//g;s/"//g')"
version="$(grep 'mod_version' gradle.properties | grep -o '[0-9]*\.[0-9]*\.[0-9]*')"
mcVersion="$(grep 'minecraft_version' gradle.properties | sed 's/minecraft_version=//g;s/ //g;s/,//g;s/"//g')"

cd ./build/libs || exit

echo "Build is going to be renamed: $projectId-$version-devbuild_$GITHUB_RUN_NUMBER-MC_$mcVersion.jar"
# Renaming the dev build
mv "$projectId-$version.jar" "$projectId-$version-devbuild_$GITHUB_RUN_NUMBER-MC_$mcVersion.jar"
