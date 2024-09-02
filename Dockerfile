FROM openjdk:21

COPY team-1.0.0-SNAPSHOT.jar team.jar
COPY /configurations/ /configurations/
COPY /locale/ /locale/

ENTRYPOINT ["java","-jar","team.jar"]