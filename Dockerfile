FROM openjdk:19
EXPOSE 8080
EXPOSE 81
EXPOSE 82
EXPOSE 83
ADD target/smb-file-manager.jar smb-file-manager.jar
ENTRYPOINT ["java", "-jar", "/smb-file-manager.jar"]