Write-Output '--- SHA-1 Current Build JAR ---'
Get-FileHash 'build\libs\mimesis-1.0.0.jar' -Algorithm SHA1 | Format-List

Write-Output '--- SHA-1 Deployed JAR ---'
Get-FileHash 'C:\Users\MahyAss\curseforge\minecraft\Instances\LaFriteuse Horror\mods\mimesis-1.0.0.jar' -Algorithm SHA1 | Format-List

Write-Output '--- Content of Deployed JAR mods.toml (credits/authors/displayURL lines) ---'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$deployed = 'C:\Users\MahyAss\curseforge\minecraft\Instances\LaFriteuse Horror\mods\mimesis-1.0.0.jar'
$zip = [System.IO.Compression.ZipFile]::OpenRead($deployed)
try {
    $entry = $zip.GetEntry('META-INF/mods.toml')
    if ($entry -ne $null) {
        $sr = New-Object System.IO.StreamReader($entry.Open())
        $content = $sr.ReadToEnd()
        $sr.Dispose()
        $content | Select-String 'credits|authors|displayURL'
    } else {
        Write-Output 'mods.toml not found in deployed JAR'
    }
} finally {
    $zip.Dispose()
}
