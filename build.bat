call gradlew build
if %ERRORLEVEL% neq 0 exit
copy build\libs\tweakception-1.8.9-0.1.0.jar "C:\MultiMC\.instances\1.8.9\.minecraft\mods\" /y
if %ERRORLEVEL% neq 0 exit
copy build\libs\tweakception-1.8.9-0.1.0.jar "C:\MultiMC\.instances\1.8.9 forge\.minecraft\mods\" /y