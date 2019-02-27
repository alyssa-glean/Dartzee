# Dartzee
Dartzee rep

Contains 4 projects:

 - Core: Helper code which is compatible with both Android and Desktop apps, e.g. logging methods and standard object extensions (HashMap, ArrayList...)
 - DesktopCore: Helper code which is desktop-specific, e.g. to do with Swing/dialogs, Base64 stuff...
 - AppUpdater: Separate project containing the Java code and batch file used to automatically update desktop applications
 - Darts: All the code for the desktop Dartzee application, which is dependent on Core and DesktopCore.

# Setup

Work to build using a build tool (Gradle/Maven) is on the backlog, however in the meantime you'll have to manually configure dependencies in the IDE of your choice.
Head to the /Dependencies directory to find all the JARs currently required, along with IntelliJ screenshots which should help you to get started.