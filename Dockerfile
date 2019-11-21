FROM tomcat:9-jdk8

MAINTAINER jeroen@kransen.nl

COPY fcrepo-configs/docker/tomcat-users.xml /usr/local/tomcat/conf/

RUN rm -rf /usr/local/tomcat/webapps/*
COPY fcrepo-webapp/target/fcrepo-webapp-5.1.0.war /usr/local/tomcat/webapps/ROOT.war
