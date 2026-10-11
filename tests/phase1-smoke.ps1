param(
  [string]$BaseUrl='http://localhost:18081/api',
  # 仓库不再内置默认口令，admin 初始密码由启动时生成并写入 app.init-password-file（默认 ./oa-init-passwords.txt），或由 OA_INIT_ADMIN_PASSWORD 提供
  [string]$AdminPassword=$env:OA_INIT_ADMIN_PASSWORD,
  [string]$CommonPassword=$env:OA_INIT_ZHANGSAN_PASSWORD
)
$ErrorActionPreference='Stop'
if(-not $AdminPassword){throw '请用 -AdminPassword 或环境变量 OA_INIT_ADMIN_PASSWORD 提供 admin 密码'}
$suffix=[DateTimeOffset]::Now.ToUnixTimeSeconds()
function Api($method,$path,$body,$token) {
  $headers=@{}
  if($token){$headers.Authorization='Bearer '+$token}
  $params=@{Uri=$BaseUrl+$path;Method=$method;Headers=$headers;TimeoutSec=10}
  if($null -ne $body){$params.ContentType='application/json; charset=utf-8';$params.Body=[System.Text.Encoding]::UTF8.GetBytes(($body|ConvertTo-Json -Depth 10 -Compress))}
  try {
    Invoke-RestMethod @params
  } catch {
    # 4xx/5xx 会被 Invoke-RestMethod 抛成异常；优先取响应体里的 code，否则用 HTTP 状态码。
    $status=0; $text=$_.Exception.Message
    try {
      $status=[int]$_.Exception.Response.StatusCode
      $stream=$_.Exception.Response.GetResponseStream()
      if($stream){ $sr=New-Object System.IO.StreamReader($stream,[System.Text.Encoding]::UTF8); $bt=$sr.ReadToEnd(); if($bt){$text=$bt} }
    } catch {}
    try { $o=$text|ConvertFrom-Json; if($o -and $o.code){ return $o } } catch {}
    return [pscustomobject]@{code=$status;msg=$text}
  }
}
$login=Api Post '/login' @{username='admin';password=$AdminPassword} $null
if($login.code -ne 200){throw 'login failed'}
$token=$login.data.token

# 前置：启用中的流程会让"没带 flowId 的公文"在提交时被自动绑定并分派给流程角色，
# 使本脚本的流程断言失真（环境问题，非产品缺陷）。临时停用，结束时还原。
$ambientFlows=@()
$flowList=Api Get '/oa/docFlow/list?pageNum=1&pageSize=200' $null $token
foreach($f in @($flowList.rows)){
  if("$($f.status)" -eq '0'){
    $ambientFlows+=$f
    Api Put '/oa/docFlow' @{flowId=$f.flowId;flowName=$f.flowName;flowType=$f.flowType;status='1'} $token|Out-Null
  }
}
$incoming=Api Post '/oa/incomingDoc' @{docNumber="G6-IN-$suffix";docTitle="G6收文流程测试$suffix";sourceUnit='测试单位';receiveDate='2026-09-20';docLevel='1';docSecret='1';summary='流程测试';status='0'} $token
$inList=Api Get ("/oa/incomingDoc/list?pageNum=1&pageSize=20&docNumber=G6-IN-$suffix") $null $token
$inDoc=$inList.rows|Select-Object -First 1
if(-not $inDoc){throw 'incoming create failed'}
$inId=$inDoc.docId
Api Post ("/oa/doc/action/1/$inId") @{action='receive';opinion='签收'} $token|Out-Null
Api Post ("/oa/doc/action/1/$inId") @{action='countersign';opinion='会签'} $token|Out-Null
Api Post ("/oa/doc/action/1/$inId") @{action='circulate';targetUserId=2;targetUserIds=@(2);opinion='请传阅'} $token|Out-Null
Api Post ("/oa/doc/action/1/$inId") @{action='urge';targetUserId=2;targetUserName='张三';opinion='请尽快办理'} $token|Out-Null
Api Post ("/oa/doc/action/1/$inId") @{action='complete';opinion='办结'} $token|Out-Null
Api Post ("/oa/doc/action/1/$inId") @{action='archive';archiveCode="ARCH-IN-$suffix";archiveLocation='档案室';opinion='归档'} $token|Out-Null
$timeline=Api Get ("/oa/doc/timeline/1/$inId") $null $token
$circulation=Api Get '/oa/doc/my/circulation' $null $token
$archive=Api Get '/oa/doc/archive/list?pageNum=1&pageSize=100' $null $token
$outgoing=Api Post '/oa/outgoingDoc' @{docNumber="G6-OUT-$suffix";docTitle="G6发文流程测试$suffix";draftUnit='测试部门';draftDate='2026-09-20';docLevel='1';docSecret='1';mainSend='测试单位';summary='流程测试';status='0'} $token
$outList=Api Get ("/oa/outgoingDoc/list?pageNum=1&pageSize=20&docNumber=G6-OUT-$suffix") $null $token
$outDoc=$outList.rows|Select-Object -First 1
if(-not $outDoc){throw 'outgoing create failed'}
$outId=$outDoc.docId
Api Post ("/oa/doc/action/2/$outId") @{action='submit';opinion='提交'} $token|Out-Null
Api Post ("/oa/doc/action/2/$outId") @{action='countersign';opinion='会签'} $token|Out-Null
Api Post ("/oa/doc/action/2/$outId") @{action='approve';opinion='通过'} $token|Out-Null
Api Post ("/oa/doc/action/2/$outId") @{action='archive';archiveCode="ARCH-OUT-$suffix";archiveLocation='档案室';opinion='归档'} $token|Out-Null
$outTimeline=Api Get ("/oa/doc/timeline/2/$outId") $null $token
$dashboard=Api Get '/oa/dashboard/stats' $null $token
$result=[pscustomobject]@{
  incomingId=$inId; outgoingId=$outId;
  approvalCount=@($timeline.data.approvals).Count;
  circulationCount=@($timeline.data.circulations).Count;
  reminderCount=@($timeline.data.reminders).Count;
  archiveCount=@($timeline.data.archives).Count;
  myCirculation=@($circulation.data).Count;
  archiveRows=@($archive.rows).Count;
  outgoingApprovals=@($outTimeline.data.approvals).Count;
  outgoingArchives=@($outTimeline.data.archives).Count;
  dashboardCode=$dashboard.code
}
foreach($f in $ambientFlows){ Api Put '/oa/docFlow' @{flowId=$f.flowId;flowName=$f.flowName;flowType=$f.flowType;status='0'} $token|Out-Null }
$result|Format-List