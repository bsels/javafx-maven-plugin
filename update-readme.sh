#!/usr/bin/env bash
if [ "$DRY_RUN" = "true" ]
then
  sed "s/<version>[0-9]\+[.][0-9]\+[.][0-9]\+<\/version>/<version>$NEW_VERSION<\/version>/g" README.md
else
  sed -i "s/<version>[0-9]\+[.][0-9]\+[.][0-9]\+<\/version>/<version>$NEW_VERSION<\/version>/g" README.md
  if [ "$GIT_STASH" = "true" ]
  then
    git add README.md
  fi
fi
