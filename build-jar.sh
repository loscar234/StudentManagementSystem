#!/usr/bin/env bash
set -e

echo "Compiling Java sources..."
mkdir -p out/production/StudentManagementSystem
find src -name "*.java" > sources.txt
javac --release 17 -cp "lib/*:src" -d out/production/StudentManagementSystem @sources.txt
rm sources.txt

echo "Preparing distribution packaging..."
rm -rf dist out/fatjar_staging
mkdir -p dist out/fatjar_staging

cp -r out/production/StudentManagementSystem/* out/fatjar_staging/

echo "Unpacking library dependencies into staging..."
for jar in lib/*.jar; do
    echo "Unpacking $jar..."
    unzip -q -o "$jar" -d out/fatjar_staging/
done

# Remove signing files to avoid security exceptions
rm -f out/fatjar_staging/META-INF/*.SF
rm -f out/fatjar_staging/META-INF/*.DSA
rm -f out/fatjar_staging/META-INF/*.RSA

echo "Creating dist/StudentManagementSystem.jar..."
jar cfe dist/StudentManagementSystem.jar view.MainFrame -C out/fatjar_staging .

echo "Build complete: dist/StudentManagementSystem.jar"
ls -lh dist/StudentManagementSystem.jar
