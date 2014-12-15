CapeDwarf Managed VM
====================

The aim of this project is to build a Docker image of WildFly application server that can be fully used in Google AppEngine Managed VM.
By "fully used" we mean one can invoke Google AppEngine APIs w/o any problems.

How to build this project?
-----------------------------------

(1) First build CapeDwarf Shared ("master" branch)

    https://github.com/capedwarf/capedwarf-shared

    mvn clean install

(2) Then simply run Maven from the project's root directory

    mvn clean install

(3) Build Docker image (this will build "capedwarf" image -- name can be of course different)

    cd docker

    docker build -t capedwarf .
