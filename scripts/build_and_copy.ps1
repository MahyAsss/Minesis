$gradle = "$env:USERPROFILE\.gradle\gradle-7.6\bin\gradle.bat"
if (-not (Test-Path $gradle)) { Write-Error "Gradle wrapper not found: $gradle"; exit 2 }
& $gradle clean build --refresh-dependencies
if ($LASTEXITCODE -ne 0) { Write-Error "Gradle build failed with exit code $LASTEXITCODE"; exit $LASTEXITCODE }
New-Item -Path libs -ItemType Directory -Force | Out-Null
# Find the built mimesis jar automatically
$built = Get-ChildItem -Path "build\libs\mimesis-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($null -eq $built) { Write-Error "No built mimesis jar found in build\libs"; exit 3 }
$jarName = $built.Name
Copy-Item -Path $built.FullName -Destination (Join-Path -Path "libs" -ChildPath $jarName) -Force
Write-Output "Copied to libs: $(Join-Path -Path 'libs' -ChildPath $jarName)"
Write-Output 'BUILD_AND_COPY_OK'
