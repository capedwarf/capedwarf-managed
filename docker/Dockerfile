# Copyright 2014 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM google/debian:wheezy
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get -q update && \
  apt-get install --no-install-recommends -y -q ca-certificates curl openjdk-7-jre-headless wget

RUN mkdir -p /var/lib/tmp
# TODO dl CapeDwarf-Managed RUN curl -0 http://repository.jboss.org/nexus/content/groups/public/org/jboss/capedwarf/... -o /var/lib/tmp/capedwarf.zip
ADD capedwarf.zip /var/lib/tmp/

RUN apt-get install unzip
RUN unzip /var/lib/tmp/capedwarf.zip -d /var/lib/tmp
RUN mv /var/lib/tmp/capedwarf /var/lib/capedwarf

RUN ln -s var/lib/capedwarf/standalone/deployments/ROOT.war /app
RUN touch var/lib/capedwarf/standalone/deployments/ROOT.war.dodeploy

WORKDIR /app

# WildFly HTTP port number
EXPOSE 8080
# WildFly management port number
EXPOSE 9990
# WildFly debug port number
EXPOSE 8787
# Env variable used by the jetty_run.sh script
ENV JBOSS_HOME /var/lib/capedwarf

# Clean any CMD that might be inherited from previous image, because that
# will pollute our ENTRYPOINT, see
# http://docs.docker.io/en/latest/reference/builder/#entrypoint.
CMD []
VOLUME ["/var/log/app_engine"]
ENTRYPOINT ["/var/lib/capedwarf/bin/standalone.sh", "-c", "standalone-capedwarf.xml"]
