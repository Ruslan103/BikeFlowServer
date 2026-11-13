param(
    [string[]] $DnsName = @(),
    [string[]] $IpAddress = @()
)

$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$configDirectory = Join-Path $projectDirectory 'config'
$settingsPath = Join-Path $configDirectory 'server-tls.properties'
$keyStorePath = Join-Path $configDirectory 'server-tls.p12'
$certificatePath = Join-Path $configDirectory 'server-tls.crt'

New-Item -ItemType Directory -Path $configDirectory -Force | Out-Null

$dnsNames = @('localhost', $env:COMPUTERNAME) + $DnsName
$dnsNames = @($dnsNames | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique)
$ipAddresses = @('127.0.0.1', '::1') + $IpAddress
$ipAddresses = @($ipAddresses | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique)

$san = [System.Security.Cryptography.X509Certificates.SubjectAlternativeNameBuilder]::new()
foreach ($name in $dnsNames) {
    $san.AddDnsName($name)
}
foreach ($address in $ipAddresses) {
    $san.AddIpAddress([System.Net.IPAddress]::Parse($address))
}

$rsa = [System.Security.Cryptography.RSA]::Create(3072)
try {
    $subject = "CN=$($dnsNames[0])"
    $request = [System.Security.Cryptography.X509Certificates.CertificateRequest]::new(
        $subject,
        $rsa,
        [System.Security.Cryptography.HashAlgorithmName]::SHA256,
        [System.Security.Cryptography.RSASignaturePadding]::Pkcs1
    )
    $request.CertificateExtensions.Add(
        [System.Security.Cryptography.X509Certificates.X509KeyUsageExtension]::new(
            [System.Security.Cryptography.X509Certificates.X509KeyUsageFlags]::DigitalSignature -bor
                [System.Security.Cryptography.X509Certificates.X509KeyUsageFlags]::KeyEncipherment,
            $true
        )
    )
    $serverAuth = [System.Security.Cryptography.Oid]::new('1.3.6.1.5.5.7.3.1')
    $extendedKeyUsage = [System.Security.Cryptography.OidCollection]::new()
    [void]$extendedKeyUsage.Add($serverAuth)
    $request.CertificateExtensions.Add(
        [System.Security.Cryptography.X509Certificates.X509EnhancedKeyUsageExtension]::new(
            $extendedKeyUsage,
            $true
        )
    )
    $request.CertificateExtensions.Add($san.Build())
    $certificate = $request.CreateSelfSigned([DateTimeOffset]::UtcNow.AddMinutes(-5), [DateTimeOffset]::UtcNow.AddYears(3))

    $passwordAlphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@$%_-'
    $randomBytes = [System.Security.Cryptography.RandomNumberGenerator]::GetBytes(48)
    try {
        $storePassword = -join ($randomBytes | ForEach-Object { $passwordAlphabet[[int]$_ % $passwordAlphabet.Length] })
    } finally {
        [System.Security.Cryptography.CryptographicOperations]::ZeroMemory($randomBytes)
    }

    $pfxBytes = $certificate.Export(
        [System.Security.Cryptography.X509Certificates.X509ContentType]::Pfx,
        $storePassword
    )
    [System.IO.File]::WriteAllBytes($keyStorePath, $pfxBytes)
    [System.Security.Cryptography.CryptographicOperations]::ZeroMemory($pfxBytes)

    $certificatePem = "-----BEGIN CERTIFICATE-----`r`n$([Convert]::ToBase64String($certificate.RawData, [Base64FormattingOptions]::InsertLineBreaks))`r`n-----END CERTIFICATE-----`r`n"
    [System.IO.File]::WriteAllText($certificatePath, $certificatePem, [System.Text.Encoding]::ASCII)
} finally {
    $rsa.Dispose()
    if ($certificate) { $certificate.Dispose() }
}

$tlsProperties = @"
# BEGIN GENERATED HTTPS SETTINGS
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=file:./config/server-tls.p12
server.ssl.key-store-password=$storePassword
server.ssl.key-store-type=PKCS12
server.servlet.session.cookie.secure=true
# END GENERATED HTTPS SETTINGS
"@

$settingsText = if (Test-Path -LiteralPath $settingsPath) {
    [System.IO.File]::ReadAllText($settingsPath)
} else {
    ''
}
$blockPattern = '(?ms)^# BEGIN GENERATED HTTPS SETTINGS\r?\n.*?^# END GENERATED HTTPS SETTINGS\r?\n?'
if ($settingsText -match $blockPattern) {
    $settingsText = [regex]::Replace($settingsText, $blockPattern, '')
}
$settingsText = $settingsText.TrimEnd() + "`r`n`r`n" + $tlsProperties.Trim() + "`r`n"
[System.IO.File]::WriteAllText($settingsPath, $settingsText, [System.Text.UTF8Encoding]::new($false))

$currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
& icacls $settingsPath /inheritance:r /grant:r "${currentUser}:(F)" 'SYSTEM:(F)' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Не удалось ограничить доступ к файлу HTTPS-настроек.' }
& icacls $keyStorePath /inheritance:r /grant:r "${currentUser}:(F)" 'SYSTEM:(F)' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Не удалось ограничить доступ к закрытому ключу.' }

Write-Output "HTTPS настроен для: $($dnsNames -join ', '), $($ipAddresses -join ', ')"
Write-Output 'Адрес сервера: https://<имя-или-IP-сервера>:8443'
Write-Output "Открытый сертификат для импорта в Java-клиенты: $certificatePath"
Write-Output 'Перезапустите Spring Boot. Не передавайте файл server-tls.p12 клиентам.'
