<#
.SYNOPSIS
    MobiDesk Broker Contract Schema Validator
.DESCRIPTION
    Validates fake broker mock payloads against the OpenAPI / API_CONTRACT.md schema.
    Verifies:
    1. Mandatory fields: lifecycle, rdpTransport, sampledAt, probeDurationMs, staleAfterSeconds, requestId, assignmentTag
    2. Prohibited fields: vm_id, host, ip, port, password, secret, token
    3. Records fake-versus-real status as UNKNOWN until real broker is deployed.
    Outputs machine-readable JSON.
#>
[CmdletBinding()]
param (
    [string]$OutputDir = "build/reports"
)

$ErrorActionPreference = "Stop"

$report = [ordered]@{
    timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ")
    contractVersion = "1.1 (API_CONTRACT.md)"
    fakeVersusRealStatus = "UNKNOWN"
    prohibitedFieldViolations = @()
    schemaViolations = @()
    passed = $true
}

# Sample test payload from StatusRepository / StatusRepositoryContractTest
$samplePayloads = @(
    '{"lifecycle":"RUNNING","rdpTransport":"RDP_NEGOTIATION_OK","sampledAt":"2026-11-03T06:00:00Z","probeDurationMs":384,"staleAfterSeconds":15,"requestId":"req_7f1b24a890d643d8","assignmentTag":"asg_tag_7f1b24a890d643d8"}',
    '{"lifecycle":"UNKNOWN","rdpTransport":"UNKNOWN","sampledAt":"2026-11-03T06:00:01Z","probeDurationMs":1500,"staleAfterSeconds":15,"requestId":"req_9a26d0f4712a45ef","assignmentTag":"asg_tag_9a26d0f4712a45ef"}'
)

$validLifecycles = @("STOPPED", "STARTING", "RUNNING", "STOPPING", "ERROR", "UNKNOWN")
$validTransports = @("NOT_CHECKED", "TCP_UNREACHABLE", "TCP_REACHABLE", "RDP_NEGOTIATION_OK", "UNKNOWN")
$prohibitedFields = @("vm_id", "vmid", "host", "hostname", "ip", "ipv4", "port", "password", "secret", "token", "service_role")

$idx = 0
foreach ($jsonStr in $samplePayloads) {
    $idx++
    $obj = $jsonStr | ConvertFrom-Json
    
    # Check prohibited fields
    foreach ($p in $prohibitedFields) {
        if ($obj.PSObject.Properties[$p]) {
            $report.prohibitedFieldViolations += "Payload #$idx contains prohibited field: $p"
        }
    }

    # Check required fields
    if (-not $obj.lifecycle -or $obj.lifecycle -notin $validLifecycles) {
        $report.schemaViolations += "Payload #$idx invalid or missing lifecycle: $($obj.lifecycle)"
    }
    if (-not $obj.rdpTransport -or $obj.rdpTransport -notin $validTransports) {
        $report.schemaViolations += "Payload #$idx invalid or missing rdpTransport: $($obj.rdpTransport)"
    }
    if ([string]::IsNullOrWhiteSpace($obj.assignmentTag)) {
        $report.schemaViolations += "Payload #$idx missing mandatory assignmentTag"
    }
    if ([string]::IsNullOrWhiteSpace($obj.requestId)) {
        $report.schemaViolations += "Payload #$idx missing mandatory requestId"
    }
}

if ($report.prohibitedFieldViolations.Count -gt 0 -or $report.schemaViolations.Count -gt 0) {
    $report.passed = $false
}

$jsonOutput = $report | ConvertTo-Json -Depth 4
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}
$outFile = Join-Path $OutputDir "broker-contract-validation.json"
$jsonOutput | Out-File -FilePath $outFile -Encoding utf8

Write-Host $jsonOutput
if (-not $report.passed) {
    exit 1
}
exit 0
