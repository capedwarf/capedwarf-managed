@echo off
rem shortcut to boot WildFly with CapeDwarf configuration
rem if you need to set other boot options, please use standalone.bat

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

if "%1" == "" (
  %DIRNAME%standalone.bat -c standalone-capedwarf.xml
) else (
  cmd /c "cd %1 && %DIRNAME%standalone.bat -c standalone-capedwarf.xml -DrootDeployment=%1"
)
