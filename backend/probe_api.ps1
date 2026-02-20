try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/supervisor/students?examId=2" -Method Get
} catch {
    Write-Output "Caught Exception:"
    Write-Output $_.Exception.Message
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $responseBody = $reader.ReadToEnd()
        Write-Output "Response Body:"
        Write-Output $responseBody
    }
}
