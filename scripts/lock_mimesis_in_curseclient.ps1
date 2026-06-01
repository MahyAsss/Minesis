$configPath = 'C:\Users\MahyAss\curseforge\minecraft\Instances\LaFriteuse Horror\minecraftinstance.json'
$backupPath = "$configPath.$((Get-Date).ToString('yyyyMMddHHmmss')).bak"

Copy-Item $configPath $backupPath
Write-Output "Backup created: $backupPath"

$config = Get-Content $configPath -Raw | ConvertFrom-Json
$beforeCount = $config.installedAddons.Count

$config.installedAddons = @($config.installedAddons | Where-Object { $_.addonID -ne 1546295 })

if ($config.installedAddons.Count -eq $beforeCount) {
    Write-Output 'ERROR: Could not find Mimesis mod in config'
    exit 1
}

$json = $config | ConvertTo-Json -Depth 100
Set-Content $configPath -Value $json

Write-Output 'Removed Mimesis from installedAddons.'
Write-Output "Before: $beforeCount entries"
Write-Output "After: $($config.installedAddons.Count) entries"
Write-Output 'CurseForge will stop managing this JAR as an installed addon.'
