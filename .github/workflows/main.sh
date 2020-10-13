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

# Setting the buildname for GH actions
echo "BUILDNAME=$buildName" >> $GITHUB_ENV
echo "PROJECT_ID=$projectId" >> $GITHUB_ENV
echo "VERSION=$version" >> $GITHUB_ENV
echo "MC_VERSION=$mcVersion" >> $GITHUB_ENV


# Checks if build is stable (I always bump version when I release stable, uploadable version)
latestRelease=$(curl -s "https://api.github.com/repos/$GITHUB_REPOSITORY/releases/latest" | grep -oP '"tag_name": "\K(.*)(?=")')
echo "Latest release is: $latestRelease"


if [ "$latestRelease" == "$version" ]; then
        echo "No need to publish release. Not necesarry stable yet."
else
        echo "Hooray! New release!"
        echo "SHOULD_PUBLISH=true" >> $GITHUB_ENV
fi