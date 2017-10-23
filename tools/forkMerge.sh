#!/usr/bin/env bash
git remote add upstream https://github.com/viniciusccarvalho/spring-cloud-sockets.git
git fetch upstream
git checkout master
git merge upstream/master
# git push origin master