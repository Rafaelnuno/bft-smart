@echo off

cd /d "C:\Users\rafap\OneDrive\Documentos\GitHub\bft-smart\library-master"

gradlew installdist
timeout /t 5
cd /d "C:\Users\rafap\OneDrive\Documentos\GitHub\bft-smart\library-master\build\install\library"

start cmd /k smartrun.cmd dti.bftmap.BFTMapServer 0
start cmd /k smartrun.cmd dti.bftmap.BFTMapServer 1
start cmd /k smartrun.cmd dti.bftmap.BFTMapServer 2
start cmd /k smartrun.cmd dti.bftmap.BFTMapServer 3
start cmd /k smartrun.cmd dti.bftmap.BFTMapInteractiveClient 4
start cmd /k smartrun.cmd dti.bftmap.BFTMapInteractiveClient 5