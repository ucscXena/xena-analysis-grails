# Apollo3.X
FROM ubuntu:18.04
MAINTAINER Nathan Dunn <nathandunn@lbl.gov>
ENV DEBIAN_FRONTEND noninteractive
ENV LC_CTYPE en_US.UTF-8
#ENV LC_ALL en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE=en_US.UTF-8

# where bin directories are
ENV CATALINA_HOME /usr/share/tomcat9
# where webapps are deployed
ENV CATALINA_BASE /var/lib/tomcat9
ENV CONTEXT_PATH ROOT
ENV JAVA_HOME /usr/lib/jvm/java-11-openjdk-amd64

RUN apt-get -qq update --fix-missing && \
	apt-get --no-install-recommends -y install \
	git build-essential vim net-tools less dirmngr \
	apt-transport-https software-properties-common \
	wget netcat postgresql tomcat9 sudo \
	curl ssl-cert zip unzip openjdk-11-jdk-headless



RUN groupadd docker
RUN useradd -ms /bin/bash -d /xena-analysis-grails xenauser
RUN usermod -aG docker xenauser

USER xenauser
#  install R libraries
RUN sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys E298A3A825C0D65DFD57CBB651716619E084DAB9

COPY gradlew /xena-analysis-grails
COPY gradle.properties /xena-analysis-grails
COPY gradle /xena-analysis-grails/gradle
COPY grails-app /xena-analysis-grails/grails-app
COPY src /xena-analysis-grails/src
#COPY src/main/scripts /xena-analysis-grails/scripts
ADD grails* /xena-analysis-grails/
COPY build.gradle /xena-analysis-grails/build.gradle
#ADD settings.gradle /xena-analysis-grails
RUN ls /xena-analysis-grails


USER root
RUN apt-get -qq update --fix-missing && \
	apt-get --no-install-recommends -y install r-base && \
	apt-get autoremove -y && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* /xena-analysis-grails/

#  install R libraries
#RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys E298A3A825C0D65DFD57CBB651716619E084DAB9
RUN add-apt-repository 'deb https://cloud.r-project.org/bin/linux/ubuntu xenial-cran40/'
COPY src/main/rlang /xena-analysis-grails/src/main/rlang
RUN Rscript /xena-analysis-grails/src/main/rlang/install-packages.R
#
#COPY docker-files/build.sh /bin/build.sh
#RUN ["chmod", "+x", "/bin/build.sh"]
#ADD docker-files/docker-xena-analysis-grails-config.groovy /xena-analysis-grails/xena-analysis-grails-config.groovy
#ADD docker-files/docker.xena-analysis-grails.yml /xena-analysis-grails/xena-analysis-grails.yml
RUN chown -R xena-analysis-grails:xena-analysis-grails /xena-analysis-grails
RUN mkdir -p /data/xena-analysis-grails_data
RUN chown -R xena-analysis-grails:xena-analysis-grails /data/xena-analysis-grails_data


USER xena-analysis-grails
WORKDIR /xena-analysis-grails
RUN ./grailsw clean && rm -rf build/* && ./grailsw war
RUN cp /xena-analysis-grails/build/libs/*.war /tmp/xena-analysis-grails.war && rm -rf /xena-analysis-grails/ || true
#RUN cp /xena-analysis-grails/build/libs/*.war /tmp/xena-analysis-grails.war
RUN mv /tmp/xena-analysis-grails.war /xena-analysis-grails/xena-analysis-grails.war



USER root
##RUN /bin/build.sh
## remove from webapps and copy it into a staging directory
RUN rm -rf ${CATALINA_BASE}/webapps/* && \
	cp /xena-analysis-grails/xena-analysis-grails*.war ${CATALINA_BASE}/xena-analysis-grails.war
#
#ADD docker-files/createenv.sh /createenv.sh
#ADD docker-files/launch.sh /launch.sh

#USER xena-analysis-grails
#WORKDIR /xena-analysis-grails

#USER root
#RUN ${CATALINA_HOME}/bin/catalina.sh stop 5 -force
#RUN ${CATALINA_HOME}/bin/catalina.sh run

#CMD "/launch.sh"
RUN ${CATALINA_HOME}/bin/catalina.sh stop 5 -force
CMD ${CATALINA_HOME}/bin/catalina.sh run
