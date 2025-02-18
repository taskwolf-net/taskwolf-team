FROM eclipse-temurin:21.0.6_7-jdk

COPY /build/libs/team-1.0.0-SNAPSHOT.jar team.jar
COPY /locale/ /locale/
COPY /geo/ /geo/

ENTRYPOINT ["java", "-jar", "team.jar"]