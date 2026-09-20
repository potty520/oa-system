param([string]$BaseUrl='http://localhost:18081/api')
$ErrorActionPreference='Stop'
$suffix=[DateTimeOffset]::Now.ToUnixTimeSeconds()
function Api($method,$path,$body,$token) {
  $headers=@{}
  if($token){$headers.Authorization='Bearer '+$token}
  $params=@{Uri=$BaseUrl+$path;Method=$method;Headers=$headers;TimeoutSec=10}
  if($null -ne $body){$params.ContentType='application/json';$params.Body=$body|ConvertTo-Json -Depth 10 -Compress}
  Invoke-RestMethod @params
}
$login=Api Post '/login' @{username='admin';password='admin123'} $null
if($login.code -ne 200){throw 'login failed'}
$token=$login.data.token
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
$result|Format-List