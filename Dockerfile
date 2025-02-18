FROM amazoncorretto:21.0.6

RUN yum update -y && yum install -y tar
RUN yum clean all

COPY /build/libs/team-1.0.0-SNAPSHOT.jar team.jar
COPY /locale/ /locale/
COPY /geo/ /geo/

ENTRYPOINT ["java", "-jar", "team.jar"]