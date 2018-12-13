FROM openjdk:8u181-jdk-alpine3.8

RUN apk -U add bash haveged

RUN mkdir /nyzoVerifier
WORKDIR /nyzoVerifier
COPY . /nyzoVerifier

RUN ./gradlew build
RUN mkdir -p /var/lib/nyzo/production
COPY trusted_entry_points verifier_private_see* /var/lib/nyzo/production/

CMD ["/usr/bin/java", "-jar", "-Xmx3G", "/nyzoVerifier/build/libs/nyzoVerifier-1.0.jar"]
