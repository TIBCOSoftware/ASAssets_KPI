javac -classpath lib/csext.jar src/*.java -d classes
rm CpuAndMemCheckerCjp.jar
jar -cvf CpuAndMemCheckerCjp.jar -C classes/ .
