@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off
set DIRNAME=%%~dp0
if "%%OS%%"=="Windows_NT" setlocal
set JAVA_EXE=%%JAVA_HOME%%/bin/java.exe
if not defined JAVA_EXE set JAVA_EXE=java.exe
set CLASSPATH=%%DIRNAME%%gradle/wrapper/gradle-wrapper.jar
"%JAVA_EXE%" -classpath "%%CLASSPATH%%" org.gradle.wrapper.GradleWrapperMain %%*
