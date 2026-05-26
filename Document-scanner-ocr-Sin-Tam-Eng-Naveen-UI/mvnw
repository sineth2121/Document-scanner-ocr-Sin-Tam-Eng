#!/usr/bin/env bash
ROOT="$(cd "$(dirname "$0")" && pwd -P)"
MAVEN_DIR="$ROOT/.mvn/apache-maven"
if [ ! -x "$MAVEN_DIR/bin/mvn" ]; then
  echo "Maven runtime not found in $MAVEN_DIR. Downloading Apache Maven..."
  MAVEN_VERSION="3.9.6"
  ZIP_URL="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.zip"
  mkdir -p "$ROOT/.mvn"
  tmpzip="$ROOT/.mvn/apache-maven.zip"
  curl -L -o "$tmpzip" "$ZIP_URL" || { echo "Download failed"; exit 1; }
  unzip -o "$tmpzip" -d "$ROOT/.mvn" || { echo "Unzip failed"; exit 1; }
  rm -f "$tmpzip"
  if [ -d "$ROOT/.mvn/apache-maven-$MAVEN_VERSION" ]; then
    mv "$ROOT/.mvn/apache-maven-$MAVEN_VERSION" "$ROOT/.mvn/apache-maven"
  fi
fi
"$MAVEN_DIR/bin/mvn" "$@"
