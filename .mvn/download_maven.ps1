param(
    [string]$rootPath,
    [string]$version
)

if (-not $version) { $version = '3.9.6' }

$root = [System.IO.Path]::GetFullPath($rootPath)
$mvnDir = Join-Path -Path $root -ChildPath ".mvn\apache-maven"
if (-not (Test-Path -Path $mvnDir)) {
    New-Item -ItemType Directory -Path (Join-Path -Path $root -ChildPath '.mvn') | Out-Null
}

$zipUrl = "https://archive.apache.org/dist/maven/maven-3/$version/binaries/apache-maven-$version-bin.zip"
$out = Join-Path -Path (Join-Path -Path $root -ChildPath '.mvn') -ChildPath 'apache-maven.zip'

Write-Host "Downloading Maven $version from $zipUrl to $out"

Invoke-WebRequest -Uri $zipUrl -OutFile $out -UseBasicParsing

Write-Host "Extracting..."
Expand-Archive -Path $out -DestinationPath (Join-Path -Path $root -ChildPath '.mvn') -Force
Remove-Item $out -Force

$extracted = Join-Path -Path (Join-Path -Path $root -ChildPath '.mvn') -ChildPath "apache-maven-$version"
if (Test-Path $extracted) {
    Rename-Item -Path $extracted -NewName 'apache-maven' -Force
}

Write-Host "Maven downloaded and extracted to $mvnDir"
