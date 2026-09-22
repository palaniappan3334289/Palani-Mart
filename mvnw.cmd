@echo off
set "JAVA_HOME=C:\Java\jdk-17.0.20.1+1"
set "PATH=C:\apache-maven-3.9.16\bin;%JAVA_HOME%\bin;%PATH%"
if exist "C:\apache-maven-3.9.16\bin\mvn.cmd" (
    call "C:\apache-maven-3.9.16\bin\mvn.cmd" %*
) else (
    mvn %*
)
