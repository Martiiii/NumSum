# How to change the log4j level
Change `DEBUG` to `INFO` or vice versa in `src/main/resources/log4j.properties` first line.

This change requires building a new WAR file

# How to build the WAR file
In the project root directory, run: ``gradlew war``

This will generate ``NumSum-1.0-SNAPSHOT.war`` into the `build/libs` directory

# How to deploy the WAR file to Apache Tomcat
0) This guide assumes that a basic/fresh Tomcat installation exists.
1) To deploy the application at the root (http://localhost:8080/), rename the generated WAR to `ROOT.war`.
    * To deploy it at something else, rename the WAR file to something else, e.g. `sum-app.war`. This will make the application available at http://localhost:8080/sum-app/
2) Copy the WAR to your Tomcat `webapps` directory.
3) Start Tomcat by running startup.sh (or startup.bat if you are on Windows) located in the `bin` directory.
4) The application should be now running and be ready for use.
5) To shut down Tomcat, run shutdown.sh (or .bat) located in the `bin` directory.

# How to run the application locally
Since this project uses the Gradle WAR plugin, it is not necessary to use an external Tomcat to run it.

1) Run `gradlew tomcatRunWar` in the project root.
2) The application should now be running at http://localhost:8080

# Other info
* Tests can be executed by running `gradlew test` in the project root
* NB! Sometimes when running the application locally and shutting it down, it is possible that the process stays alive and on the next startup will give an error about the port 8080 being in use. If that is the case, manually kill the process in Windows task manager/Linux alternative.
