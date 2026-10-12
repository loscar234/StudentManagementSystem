# Build script to create standalone executable JAR
Write-Host "Compiling Java sources..."
$javaFiles = Get-ChildItem -Path src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -cp "lib/*;src" -d out/production/StudentManagementSystem $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed."
    exit 1
}

Write-Host "Preparing distribution packaging..."
if (Test-Path dist) { Remove-Item -Path dist -Recurse -Force }
if (Test-Path out/fatjar_staging) { Remove-Item -Path out/fatjar_staging -Recurse -Force }

New-Item -ItemType Directory -Force -Path dist | Out-Null
New-Item -ItemType Directory -Force -Path out/fatjar_staging | Out-Null

Copy-Item -Path out/production/StudentManagementSystem/* -Destination out/fatjar_staging -Recurse -Force

Write-Host "Unpacking library dependencies into staging..."
$jars = Get-ChildItem -Path lib -Filter *.jar
foreach ($jar in $jars) {
    Write-Host "Unpacking $($jar.Name)..."
    tar -xf $jar.FullName -C out/fatjar_staging
}

# Remove signing files to avoid SecurityException: Invalid signature
Remove-Item -Path out/fatjar_staging/META-INF/*.SF -ErrorAction SilentlyContinue
Remove-Item -Path out/fatjar_staging/META-INF/*.DSA -ErrorAction SilentlyContinue
Remove-Item -Path out/fatjar_staging/META-INF/*.RSA -ErrorAction SilentlyContinue

Write-Host "Creating dist/StudentManagementSystem.jar..."
jar cfe dist/StudentManagementSystem.jar view.MainFrame -C out/fatjar_staging .

if (Test-Path dist/StudentManagementSystem.jar) {
    $size = (Get-Item dist/StudentManagementSystem.jar).Length / 1MB
    Write-Host ("Build complete: dist/StudentManagementSystem.jar ({0:N2} MB)" -f $size) -ForegroundColor Green
} else {
    Write-Error "Failed to produce JAR."
    exit 1
}
