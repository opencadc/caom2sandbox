FROM images.opencadc.org/library/cadc-tomcat:1

RUN dnf install -y wcslib && dnf clean all

COPY build/libs/sc2links.war /usr/share/tomcat/webapps
