$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
Write-Host "Java 21 has been set for this session."
java -version
