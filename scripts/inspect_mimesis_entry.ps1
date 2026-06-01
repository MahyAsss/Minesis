$configPath = 'C:\Users\MahyAss\curseforge\minecraft\Instances\LaFriteuse Horror\minecraftinstance.json'
$config = Get-Content $configPath -Raw | ConvertFrom-Json
$m = $config.installedAddons | Where-Object { $_.addonID -eq 1546295 }
$m | Select-Object addonID, name, isLocked, isModified, preferenceAutoInstallUpdates, preferenceIsIgnored, fileNameOnDisk, filePaths | Format-List
