@echo off

start cmd /k gradlew installdist

ping 192.0.0.1 -n 1 > nul

cd /d "build\install\library"

start cmd /k smartrun.cmd dti.bftmap.BFTMapServer 0
start cmd /k smartrun.cmd dti.bftmap.BFTMapServer 1
start cmd /k smartrun.cmd dti.bftmap.BFTMapServer 2
start cmd /k smartrun.cmd dti.bftmap.BFTMapServer 3
start cmd /k smartrun.cmd dti.bftmap.BFTMapInteractiveClient 4
start cmd /k smartrun.cmd dti.bftmap.BFTMapInteractiveClient 5