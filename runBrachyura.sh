#!/bin/bash

# Brachyura can be run with `java -jar brachyura-bootstrap-0.jar`
# but this is for people who have potato PC :)

# JVM flags for potato PCs
#
#        ▒▒                          
#      ▒▒░░▒▒  ▒▒▒▒▒▒▒▒▒▒▒▒▒▒  ▒▒▒▒▒▒
#      ▒▒░░░░▒▒░░░░░░░░░░░░░░▒▒░░░░▒▒
#      ▒▒░░▒▒░░░░░░░░░░░░░░░░░░▒▒░░▒▒
#      ▒▒▒▒░░▓▓░░░░░░░░▓▓░░░░░░░░░░▒▒
#      ▒▒░░░░▓▓░░░░░░░░▓▓░░░░░░░░░░▒▒
#    ▒▒░░░░░░░░░░░░░░░░░░░░░░░░░░░░▒▒
#    ▒▒▓▓▓▓░░▓▓░░▓▓░░▓▓░░░░▓▓▓▓░░░░▒▒
#    ▒▒░░▓▓░░░░▓▓░░▓▓░░░░▓▓░░░░░░░░▒▒
#    ▒▒▓▓░░░░░░░░░░░░░░░░░░▓▓▓▓░░▒▒  
#    ▒▒░░░░░░░░░░░░░░░░░░░░░░░░░░░░▒▒
#    ▒▒░░░░░░░░░░░░░░░░░░░░░░░░░░░░▒▒
#    ▒▒░░░░░░░░░░░░░░░░░░░░░░░░░░▒▒  
#      ▒▒░░░░░░░░░░░░░░░░░░░░░░▒▒    
#        ▒▒▒▒▒▒░░░░░░░░░░▒▒▒▒▒▒      
#              ▒▒▒▒▒▒▒▒▒▒            
#

# Needs OpenJ9 VM
/usr/lib/jvm/jdk-17.0.1+12/bin/java -Xquickstart -Xtune:virtualized -XX:+IdleTuningGcOnIdle -XX:+UseAggressiveHeapShrink -Xmn100M -Xmx1G -jar brachyura-bootstrap-0.jar "$@"









