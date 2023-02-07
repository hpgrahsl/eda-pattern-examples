FROM quay.io/rhdevelopers/eda-claim-check-pattern:v23.02

ADD images.tar.gz /home/data/images

ENTRYPOINT ["java", "-Djava.util.logging.manager=org.jboss.logmanager.LogManager","-jar","quarkus-run.jar"]
