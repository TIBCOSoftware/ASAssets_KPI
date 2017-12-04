$memoryInfo = gwmi -Query "SELECT TotalVisibleMemorySize, FreePhysicalMemory FROM Win32_OperatingSystem"
$totalVisibleMemorySizeMB   = ($memoryInfo.TotalVisibleMemorySize / 1024)
$availablePhysicalMemoryMB  = ($memoryInfo.FreePhysicalMemory / 1024)
$InUseMemoryMB              = ($TotalVisibleMemorySizeMB - $availablePhysicalMemoryMB)
echo Memory_Total=$totalVisibleMemorySizeMB
echo Memory_In_Use=$InUseMemoryMB
echo Memory_Available=$availablePhysicalMemoryMB
