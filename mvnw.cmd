@echo off
setlocal
set "MAVEN_PROJECTBASEDIR=%~dp0"
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"
set "MAVEN_WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
if defined JAVA_HOME (
    set "MAVEN_JAVA=%JAVA_HOME%\bin\java.exe"
) else (
    set "MAVEN_JAVA=java"
)
if not exist "%MAVEN_WRAPPER_JAR%" (
    echo Maven Wrapper JAR is missing. Download it from the wrapperUrl in .mvn\wrapper\maven-wrapper.properties.
    exit /b 1
)
"%MAVEN_JAVA%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" -classpath "%MAVEN_WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
set MAVEN_EXIT_CODE=%ERRORLEVEL%
endlocal & exit /b %MAVEN_EXIT_CODE%
