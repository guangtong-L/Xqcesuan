# 本地启动前端（PowerShell）
# 用 npm（你机器没装 pnpm 也能跑），编译目标：微信小程序
# 启动后用微信开发者工具导入 dist/dev/mp-weixin/

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# 检查 node
$nodeVer = (node -v) 2>$null
if (-not $nodeVer) {
    Write-Host "未找到 Node.js，请先安装 Node 18+" -ForegroundColor Red
    exit 1
}
Write-Host "[run] Node $nodeVer" -ForegroundColor Green

# 首次自动 install
if (-not (Test-Path 'node_modules')) {
    Write-Host "[run] 首次安装依赖（约 3-5 分钟）" -ForegroundColor Yellow
    npm config set registry https://registry.npmmirror.com
    npm install --no-audit --no-fund
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

# 启动微信小程序模式
Write-Host "[run] 启动 dev:mp-weixin（产物在 dist/dev/mp-weixin）" -ForegroundColor Cyan
Write-Host "[run] 启动后请用微信开发者工具导入该目录" -ForegroundColor Cyan
npm run dev:mp-weixin
