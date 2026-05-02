# 本地启动后端（PowerShell）
# 自动设置 JAVA_HOME 为本机 JDK 8，运行打包好的 jar
# 使用：右键以 PowerShell 运行，或在 PowerShell 中：./run-local.ps1
# 端口：8080  ｜  profile：dev

$ErrorActionPreference = 'Stop'

# 1) 自动定位 JDK（按优先级试本机常见路径）
$candidates = @(
    'D:\JDK\jdk1.8.0_171\jdk1.8.0_171',
    'D:\JDK\jdk-17.0.12',
    "$env:JAVA_HOME"
)
$javaHome = $null
foreach ($c in $candidates) {
    if ($c -and (Test-Path "$c\bin\java.exe")) { $javaHome = $c; break }
}
if (-not $javaHome) {
    Write-Host "未找到 JDK。请安装 JDK 8 或 JDK 11，并把路径加入此脚本 `$candidates。" -ForegroundColor Red
    exit 1
}
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;" + $env:Path
Write-Host "[run] JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Green

# 2) 切到脚本所在目录
Set-Location $PSScriptRoot

# 3) 若 jar 不存在则先打包
$jar = 'target\fortune-backend.jar'
if (-not (Test-Path $jar)) {
    Write-Host "[run] jar 不存在，先 mvn package..." -ForegroundColor Yellow
    mvn -B -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[run] mvn package 失败" -ForegroundColor Red
        exit 1
    }
}

# 4) 启动
Write-Host "[run] 启动 fortune-backend  端口=8080  profile=dev" -ForegroundColor Cyan
java -Duser.timezone=Asia/Shanghai -jar $jar --server.port=8080 --spring.profiles.active=dev
