# JAR 包大小分析脚本
Write-Host "=== JAR 包大小分析 ===" -ForegroundColor Green

$jarPath = "target\ablaze-0.0.1-SNAPSHOT.jar"
if (Test-Path $jarPath) {
    $jarFile = Get-Item $jarPath
    $sizeMB = [math]::Round($jarFile.Length / 1MB, 2)
    Write-Host "`nJAR 文件大小: $sizeMB MB ($($jarFile.Length) bytes)" -ForegroundColor Yellow
    
    # 提取并分析依赖
    Write-Host "`n正在分析依赖库..." -ForegroundColor Cyan
    $tempDir = "temp-jar-extract"
    if (Test-Path $tempDir) {
        Remove-Item -Recurse -Force $tempDir
    }
    New-Item -ItemType Directory -Path $tempDir | Out-Null
    
    # 提取 BOOT-INF/lib 目录下的 jar 文件列表
    jar -xf $jarPath BOOT-INF/lib 2>&1 | Out-Null
    
    if (Test-Path "BOOT-INF\lib") {
        Write-Host "`n依赖库统计:" -ForegroundColor Green
        $libs = Get-ChildItem "BOOT-INF\lib" -Filter "*.jar"
        Write-Host "总依赖数量: $($libs.Count)" -ForegroundColor Yellow
        
        # 按大小排序，显示前20个最大的依赖
        Write-Host "`n前20个最大的依赖库:" -ForegroundColor Green
        $libs | Sort-Object Length -Descending | Select-Object -First 20 | ForEach-Object {
            $sizeKB = [math]::Round($_.Length / 1KB, 2)
            $percent = [math]::Round(($_.Length / $jarFile.Length) * 100, 2)
            Write-Host "  $($_.Name.PadRight(60)) $sizeKB KB ($percent%)"
        }
        
        # 清理
        Remove-Item -Recurse -Force "BOOT-INF" -ErrorAction SilentlyContinue
    }
} else {
    Write-Host "JAR 文件不存在，请先执行 mvn package" -ForegroundColor Red
}
