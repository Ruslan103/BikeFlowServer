param(
    [string]$Version = "1.0.1"
)

$ErrorActionPreference = "Stop"

if ($Version -notmatch '^\d+\.\d+\.\d+$') {
    throw "Version must use the format MAJOR.MINOR.PATCH, for example 1.0.0."
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$distributionName = "BikeFlowServer-$Version-linux-amd64"
$distributionRoot = Join-Path $repositoryRoot "dist\$distributionName"
$archivePath = Join-Path $repositoryRoot "dist\$distributionName.zip"
$imageTag = "bikeflowserver:$Version"
$postgresImage = "postgres:16-alpine"
$imagesArchive = Join-Path $distributionRoot "BikeFlowServer-$Version-images.tar"

if (Test-Path $distributionRoot) {
    throw "Output folder already exists: $distributionRoot. Move it before rebuilding the release."
}
if (Test-Path $archivePath) {
    throw "Release ZIP already exists: $archivePath. Move it before rebuilding the release."
}

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Docker Desktop is not ready. Start it and run this script again."
}

$templatePath = Join-Path $PSScriptRoot "..\packaging\windows"
$null = New-Item -ItemType Directory -Path (Split-Path $distributionRoot -Parent) -Force
$null = New-Item -ItemType Directory -Path $distributionRoot

Copy-Item (Join-Path $templatePath "compose.yaml") $distributionRoot
Copy-Item (Join-Path $templatePath "start-windows.bat") $distributionRoot
Copy-Item (Join-Path $templatePath "stop-windows.bat") $distributionRoot
Copy-Item (Join-Path $templatePath "logs.bat") $distributionRoot
Copy-Item (Join-Path $templatePath "start-linux.sh") $distributionRoot
Copy-Item (Join-Path $templatePath "stop-linux.sh") $distributionRoot
Copy-Item (Join-Path $templatePath "logs.sh") $distributionRoot
Copy-Item (Join-Path $templatePath "README.txt") $distributionRoot
Copy-Item (Join-Path $repositoryRoot ".env.example") $distributionRoot

$utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
foreach ($fileName in @("compose.yaml", "start-windows.bat", "start-linux.sh", "README.txt")) {
    $filePath = Join-Path $distributionRoot $fileName
    $content = Get-Content -Raw -Encoding UTF8 $filePath
    $content = $content.Replace("bikeflowserver:1.0.1", $imageTag).Replace("@VERSION@", $Version)
    [System.IO.File]::WriteAllText($filePath, $content, $utf8WithoutBom)
}

Write-Host "Pulling $postgresImage..."
docker pull --platform linux/amd64 $postgresImage
if ($LASTEXITCODE -ne 0) {
    throw "Could not download $postgresImage."
}

Write-Host "Building $imageTag..."
docker build --platform linux/amd64 --tag $imageTag $repositoryRoot
if ($LASTEXITCODE -ne 0) {
    throw "Docker image build failed."
}

Write-Host "Exporting the application and database images..."
docker save --output $imagesArchive $imageTag $postgresImage
if ($LASTEXITCODE -ne 0) {
    throw "Could not export Docker images."
}

Write-Host "Creating $archivePath..."
Compress-Archive -Path (Join-Path $distributionRoot "*") -DestinationPath $archivePath -CompressionLevel Optimal

Write-Host "Release package created: $archivePath"
Write-Host "Package size: $([math]::Round((Get-Item $archivePath).Length / 1GB, 2)) GB"
