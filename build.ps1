# Build script for Mimesis Mod
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Mimesis Mod - Build Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$GRADLE_HOME = Join-Path $env:USERPROFILE ".gradle"
$GRADLE_VERSION = "7.6"
$GRADLE_ZIP = "gradle-$GRADLE_VERSION-bin.zip"
$GRADLE_URL = "https://services.gradle.org/distributions/$GRADLE_ZIP"
$GRADLE_BIN = Join-Path $GRADLE_HOME "gradle-$GRADLE_VERSION\bin\gradle.bat"

# Create .gradle directory if it doesn't exist
if (!(Test-Path $GRADLE_HOME)) {
    New-Item -ItemType Directory -Path $GRADLE_HOME -Force | Out-Null
}

# Download Gradle if not present
if (!(Test-Path $GRADLE_BIN)) {
    Write-Host "Gradle not found. Downloading Gradle $GRADLE_VERSION..." -ForegroundColor Yellow
    
    $GRADLE_ZIP_PATH = Join-Path $GRADLE_HOME $GRADLE_ZIP
    
    try {
        [Net.ServicePointManager]::SecurityProtocol = 'Tls12'
        $WebClient = New-Object System.Net.WebClient
        $WebClient.DownloadFile($GRADLE_URL, $GRADLE_ZIP_PATH)
        Write-Host "Downloaded successfully." -ForegroundColor Green
        
        Write-Host "Extracting Gradle..." -ForegroundColor Yellow
        Add-Type -AssemblyName 'System.IO.Compression.FileSystem'
        [System.IO.Compression.ZipFile]::ExtractToDirectory($GRADLE_ZIP_PATH, $GRADLE_HOME)
        Write-Host "Extracted successfully." -ForegroundColor Green
        
        Remove-Item $GRADLE_ZIP_PATH -Force
    }
    catch {
        Write-Host "Failed to download/extract Gradle: $_" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "Building Mimesis Mod..." -ForegroundColor Cyan
Write-Host "Command: gradle clean build -x test" -ForegroundColor Gray
Write-Host ""

# Run build
Set-Location "C:\Users\MahyAss\Desktop\Mimesis"
& $GRADLE_BIN clean build -x test

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Build Successful!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "JAR file created at:" -ForegroundColor Cyan
    Write-Host "  build/libs/mimesis-1.0.0.jar" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "You can now:" -ForegroundColor Cyan
    Write-Host "  1. Copy to mods folder in Minecraft installation"
    Write-Host "  2. Run ./gradlew runClient to test in-game"
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "Build Failed!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "Exit code: $LASTEXITCODE" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}
