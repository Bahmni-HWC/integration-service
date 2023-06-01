FROM amazoncorretto:17
COPY integrator/build/libs/integrator-0.0.2-SNAPSHOT.jar /opt/integrator/integrator.jar
COPY int-admin-app-build/ /var/www/avni-int-service/
CMD java -jar /opt/integrator/integrator.jar
