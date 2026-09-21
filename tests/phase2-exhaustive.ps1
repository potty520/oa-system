param([string]$BaseUrl='http://localhost:18081/api')
$ErrorActionPreference='Stop'
$suffix=[DateTimeOffset]::Now.ToUnixTimeSeconds()
$results=New-Object System.Collections.Generic.List[object]
function Check($name,$condition,$detail=''){ $state=$(if($condition){'PASS'}else{'FAIL'}); Write-Host "$state $name $detail"; $results.Add([pscustomobject]@{Test=$name;Result=$state;Detail=$detail})|Out-Null; if(-not $condition){throw "FAILED: $name $detail"} }
function Api($method,$path,$body,$token){$headers=@{};if($token){$headers.Authorization='Bearer '+$token};$p=@{Uri=$BaseUrl+$path;Method=$method;Headers=$headers;TimeoutSec=15};if($null -ne $body){$p.ContentType='application/json';$p.Body=$body|ConvertTo-Json -Depth 12 -Compress};try{Invoke-RestMethod @p}catch{$status=[int]$_.Exception.Response.StatusCode; [pscustomobject]@{code=$status;msg=$_.Exception.Message}}}
$admin=Api Post '/login' @{username='admin';password='admin123'} $null; $token=$admin.data.token
$register=Api Post '/register' @{userName="anonymous$suffix";nickName='匿名';password='Test123456'} $null; Check '匿名注册拦截' ($register.code -eq 403) $register.msg
$probeUser="missing-user-$suffix"; $probeCode=0; foreach($attempt in 1..5){$probe=Api Post '/login' @{username=$probeUser;password='wrong-password'} $null; $probeCode=$probe.code}; Check '登录失败计数' ($probeCode -eq 400) "code=$probeCode"
$locked=Api Post '/login' @{username=$probeUser;password='wrong-password'} $null; Check '登录失败锁定' ($locked.code -eq 429) $locked.msg
# Duplicate and empty data validation
$dupUser=Api Post '/system/user' @{userName='admin';nickName='Duplicate';password='Test123456'} $token; Check '重复账号拦截' ($dupUser.code  -eq  409) $dupUser.msg
$emptyUser=Api Post '/system/user' @{userName='';password=''} $token; Check '空用户数据拦截' ($emptyUser.code  -eq  400) $emptyUser.msg
$emptyDoc=Api Post '/oa/incomingDoc' @{docNumber='';docTitle=''} $token; Check '空公文数据拦截' ($emptyDoc.code  -eq  400) $emptyDoc.msg
# Incoming workflow
$inNum="P2-IN-$suffix"; $a=Api Post '/oa/incomingDoc' @{docNumber=$inNum;docTitle="P2收文$suffix";sourceUnit='测试单位';receiveDate='2026-09-20';docLevel='1';docSecret='1';summary='测试';status='0'} $token; Check '收文创建' ($a.code -eq  200)
$inList=Api Get ("/oa/incomingDoc/list?pageNum=1&pageSize=20&docNumber=$inNum") $null $token; $inDoc=$inList.rows|Select-Object -First 1; $inId=$inDoc.docId
$badArchive=Api Post ("/oa/doc/action/1/$inId") @{action='archive'} $token; Check '非法状态归档拦截' ($badArchive.code  -eq  400) $badArchive.msg
Api Post ("/oa/doc/action/1/$inId") @{action='receive'} $token|Out-Null
$ret=Api Post ("/oa/doc/action/1/$inId") @{action='return';opinion='退回修改'} $token; Check '收文退回' ($ret.code -eq  200)
$trans=Api Post ("/oa/doc/action/1/$inId") @{action='transfer';targetUserId=2;targetUserName='张三';opinion='转办'} $token; Check '收文转办' ($trans.code -eq  200)
$add=Api Post ("/oa/doc/action/1/$inId") @{action='add_sign';targetUserIds=@(2,3);opinion='加签'} $token; Check '多用户加签' ($add.code -eq  200)
$tl=Api Get ("/oa/doc/timeline/1/$inId") $null $token; $sign=@($tl.data.approvals|Where-Object {$_.approveResult -eq '7'})|Select-Object -First 1
$cancel=Api Post ("/oa/doc/action/1/$inId") @{action='cancel_sign';approvalId=$sign.approvalId;opinion='撤签'} $token; Check '撤签' ($cancel.code -eq  200)
$counter=Api Post ("/oa/doc/action/1/$inId") @{action='countersign';targetUserIds=@(2,3);opinion='多人会签'} $token; Check '多用户会签' ($counter.code -eq  200)
$delegate=Api Post ("/oa/doc/action/1/$inId") @{action='delegate';targetUserId=2;targetUserName='张三';opinion='委托'} $token; Check '委托' ($delegate.code -eq  200)
$agent=Api Post ("/oa/doc/action/1/$inId") @{action='agent';targetUserId=3;targetUserName='李四';opinion='代理'} $token; Check '代理' ($agent.code -eq  200)
$withdraw=Api Post ("/oa/doc/action/1/$inId") @{action='withdraw';opinion='撤回'} $token; Check '收文撤回' ($withdraw.code -eq  200)
Api Post ("/oa/doc/action/1/$inId") @{action='receive'} $token|Out-Null
Api Post ("/oa/doc/action/1/$inId") @{action='complete'} $token|Out-Null
Api Post ("/oa/doc/action/1/$inId") @{action='archive';archiveCode="P2-ARCH-IN-$suffix";archiveLocation='档案室'} $token|Out-Null
$tl2=Api Get ("/oa/doc/timeline/1/$inId") $null $token; Check '收文流程记录完整' (@($tl2.data.approvals).Count  -ge  5 -and @($tl2.data.delegates).Count  -eq  2) "approvals=$(@($tl2.data.approvals).Count) delegates=$(@($tl2.data.delegates).Count)"
# Outgoing workflow
$outNum="P2-OUT-$suffix"; Api Post '/oa/outgoingDoc' @{docNumber=$outNum;docTitle="P2发文$suffix";draftUnit='测试部门';draftDate='2026-09-20';docLevel='1';docSecret='1';mainSend='测试单位';summary='测试';status='0'} $token|Out-Null
$outList=Api Get ("/oa/outgoingDoc/list?pageNum=1&pageSize=20&docNumber=$outNum") $null $token; $outDoc=$outList.rows|Select-Object -First 1; $outId=$outDoc.docId
$badSign=Api Post ("/oa/doc/action/2/$outId") @{action='sign'} $token; Check '非法状态签发拦截' ($badSign.code -eq  400) $badSign.msg
Api Post ("/oa/doc/action/2/$outId") @{action='submit'} $token|Out-Null
$ow=Api Post ("/oa/doc/action/2/$outId") @{action='withdraw'} $token; Check '发文撤回' ($ow.code -eq  200)
Api Post ("/oa/doc/action/2/$outId") @{action='submit'} $token|Out-Null
$or=Api Post ("/oa/doc/action/2/$outId") @{action='return'} $token; Check '发文退回' ($or.code -eq  200)
Api Post ("/oa/doc/action/2/$outId") @{action='submit'} $token|Out-Null
Api Post ("/oa/doc/action/2/$outId") @{action='approve';opinion='审核通过'} $token|Out-Null
$outSign=Api Post ("/oa/doc/action/2/$outId") @{action='sign'} $token; Check '发文签发' ($outSign.code -eq 200) $outSign.msg
Api Post ("/oa/doc/action/2/$outId") @{action='archive';archiveCode="P2-ARCH-OUT-$suffix";archiveLocation='档案室'} $token|Out-Null
$otl=Api Get ("/oa/doc/timeline/2/$outId") $null $token; Check '发文全流程完成' (@($otl.data.approvals).Count  -ge  3 -and @($otl.data.archives).Count  -eq  1) "approvals=$(@($otl.data.approvals).Count)"
# Duplicate document number
$dupDoc=Api Post '/oa/incomingDoc' @{docNumber=$inNum;docTitle='重复编号';sourceUnit='测试';receiveDate='2026-09-20';docLevel='1';docSecret='1';status='0'} $token; Check '重复公文编号拦截' ($dupDoc.code  -eq  409) $dupDoc.msg
# Illegal IDs
$badId=Api Post '/oa/doc/action/1/99999999' @{action='receive'} $token; Check '不存在公文ID拦截' ($badId.code -eq  400) $badId.msg
# Data scopes through contacts endpoint
$usersToCreate=@(
 @{userName="scope103$suffix";nickName='范围103';password='Test123456';deptId=103;roleIds=@(2)},
 @{userName="scope101$suffix";nickName='范围101';password='Test123456';deptId=101;roleIds=@(2)}
); $createdUsers=@(); foreach($u in $usersToCreate){ $r=Api Post '/system/user' $u $token; $ul=Api Get ("/system/user/list?pageNum=1&pageSize=20&userName="+$u.userName) $null $token; $createdUsers+=($ul.rows|Select-Object -First 1) }
$commonLogin=Api Post '/login' @{username='zhangsan';password='admin123'} $null; $zh=$commonLogin.data.token
$unauthorizedAdd=Api Post '/oa/incomingDoc' @{docNumber="DENIED-IN-$suffix";docTitle='越权收文';sourceUnit='测试';receiveDate='2026-09-20';docLevel='1';docSecret='1';status='0'} $zh; Check '普通用户收文新增权限拦截' ($unauthorizedAdd.code -eq 403) $unauthorizedAdd.msg
$unauthorizedDelete=Api Delete ("/oa/incomingDoc/"+$inId) $null $zh; Check '普通用户收文删除权限拦截' ($unauthorizedDelete.code -eq 403) $unauthorizedDelete.msg
$unauthNum="DENIED-ACTION-$suffix"; Api Post '/oa/incomingDoc' @{docNumber=$unauthNum;docTitle='越权审批测试';sourceUnit='测试';receiveDate='2026-09-20';docLevel='1';docSecret='1';docTypeId=1000;status='0'} $token|Out-Null
$unauthList=Api Get ("/oa/incomingDoc/list?pageNum=1&pageSize=20&docNumber=$unauthNum") $null $token; $unauthDocId=($unauthList.rows|Select-Object -First 1).docId
$unauthorizedAction=Api Post ("/oa/doc/action/1/$unauthDocId") @{action='receive';opinion='越权签收'} $zh; Check '普通用户公文处理人校验' ($unauthorizedAction.code -eq 403) $unauthorizedAction.msg
$flowName="AUTO-FLOW-$suffix"; Api Post '/oa/docFlow' @{flowName=$flowName;flowType='2';description='自动流转回归';status='0';steps=@(@{stepName='领导签发';stepOrder=1;roleId=2;isSign='1'})} $token|Out-Null
$flowList=Api Get ("/oa/docFlow/list?pageNum=1&pageSize=20&flowName=$flowName") $null $token; $deniedFlowId=($flowList.rows|Select-Object -First 1).flowId
$flowInfo=Api Get ("/oa/docFlow/$deniedFlowId") $null $token; $deniedStepId=($flowInfo.data.steps|Select-Object -First 1).stepId
$flowDocNum="AUTO-FLOW-DOC-$suffix"; Api Post '/oa/outgoingDoc' @{docNumber=$flowDocNum;docTitle='自动流转测试';draftUnit='测试部门';draftDate='2026-09-21';docLevel='1';docSecret='1';docTypeId=1000;flowId=$deniedFlowId;mainSend='测试单位';status='0'} $token|Out-Null
$flowDocList=Api Get ("/oa/outgoingDoc/list?pageNum=1&pageSize=20&docNumber=$flowDocNum") $null $token; $flowDocId=($flowDocList.rows|Select-Object -First 1).docId
Api Post ("/oa/doc/action/2/$flowDocId") @{action='submit'} $token|Out-Null
$todoList=Api Get '/oa/todo/list?pageNum=1&pageSize=100&todoType=outgoing_doc' $null $zh
Check '提交后自动流转到对应角色' (@($todoList.rows|Where-Object {$_.relatedId -eq $flowDocId}).Count -ge 1) "count=$(@($todoList.rows).Count)"
$flowApprove=Api Post ("/oa/doc/action/2/$flowDocId") @{action='approve';opinion='同意'} $zh
Check '角色待办审批处理' ($flowApprove.code -eq 200) $flowApprove.msg
$flowDocAfter=Api Get ("/oa/outgoingDoc/$flowDocId") $null $token
Check '流程末步骤自动完成' ($flowDocAfter.data.status -eq '2' -and $flowDocAfter.data.signUserId -eq 2) "status=$($flowDocAfter.data.status) signUser=$($flowDocAfter.data.signUserId)"

$adminFlowName="BINDING-FLOW-$suffix"; Api Post '/oa/docFlow' @{flowName=$adminFlowName;flowType='2';description='绑定回归';status='0';steps=@(@{stepName='管理员步骤';stepOrder=1;roleId=1;isSign='1'})} $token|Out-Null
$adminFlowList=Api Get ("/oa/docFlow/list?pageNum=1&pageSize=20&flowName=$adminFlowName") $null $token; $adminFlowId=($adminFlowList.rows|Select-Object -First 1).flowId
$bindingDocNum="BINDING-FLOW-DOC-$suffix"; Api Post '/oa/outgoingDoc' @{docNumber=$bindingDocNum;docTitle='流程绑定越权测试';draftUnit='测试部门';draftDate='2026-09-21';docLevel='1';docSecret='1';docTypeId=1000;flowId=$adminFlowId;mainSend='测试单位';status='0'} $token|Out-Null
$bindingList=Api Get ("/oa/outgoingDoc/list?pageNum=1&pageSize=20&docNumber=$bindingDocNum") $null $token; $bindingDocId=($bindingList.rows|Select-Object -First 1).docId
Api Post ("/oa/doc/action/2/$bindingDocId") @{action='submit'} $token|Out-Null
$flowBypass=Api Post ("/oa/doc/action/2/$bindingDocId") @{action='sign';flowId=$deniedFlowId;stepId=$deniedStepId;opinion='流程角色越权'} $zh
Check '流程模板角色不能绕过公文分派' ($flowBypass.code -eq 403) $flowBypass.msg
# self scope (5)
Api Put '/system/role' @{roleId=2;roleName='普通用户';roleKey='common';roleSort=2;dataScope='5';deptIds=@();status='0'} $token|Out-Null
$selfScope=Api Get '/system/user/contacts' $null $zh; Check '仅本人数据范围' (@($selfScope.data).Count  -eq  1) "count=$(@($selfScope.data).Count)"
# dept scope (3): zhangsan + scope103
Api Put '/system/role' @{roleId=2;roleName='普通用户';roleKey='common';roleSort=2;dataScope='3';deptIds=@();status='0'} $token|Out-Null
$deptScope=Api Get '/system/user/contacts' $null $zh; Check '本部门数据范围' (@($deptScope.data|Where-Object {$_.deptId -eq  103}).Count  -ge  2) "count=$(@($deptScope.data).Count)"
# dept and below (4)
Api Put '/system/role' @{roleId=2;roleName='普通用户';roleKey='common';roleSort=2;dataScope='4';deptIds=@();status='0'} $token|Out-Null
$belowScope=Api Get '/system/user/contacts' $null $zh; Check '本部门及以下数据范围' (@($belowScope.data|Where-Object {$_.deptId -eq  103}).Count  -ge  2) "count=$(@($belowScope.data).Count)"
# custom dept (2): self + dept101
Api Put '/system/role' @{roleId=2;roleName='普通用户';roleKey='common';roleSort=2;dataScope='2';deptIds=@(101);status='0'} $token|Out-Null
$customScope=Api Get '/system/user/contacts' $null $zh; Check '自定义部门数据范围' (@($customScope.data|Where-Object {$_.deptId -eq  101}).Count  -ge  1 -and @($customScope.data|Where-Object {$_.userId -eq  2}).Count  -eq  1) "count=$(@($customScope.data).Count)"
# multi-role union: self scope + current department scope
Api Put '/system/role' @{roleId=2;roleName='普通用户';roleKey='common';roleSort=2;dataScope='5';deptIds=@();status='0'} $token|Out-Null
Api Put '/system/role' @{roleId=3;roleName='部门经理';roleKey='manager';roleSort=3;dataScope='3';deptIds=@();status='0'} $token|Out-Null
Api Put '/system/user' @{userId=2;userName='zhangsan';nickName='张三';deptId=103;roleIds=@(2,3);status='0'} $token|Out-Null
$combinedLogin=Api Post '/login' @{username='zhangsan';password='admin123'} $null
$combinedScope=Api Get '/system/user/contacts' $null $combinedLogin.data.token
Check '多角色数据范围并集' (@($combinedScope.data|Where-Object {$_.deptId -eq 103}).Count -ge 2) "count=$(@($combinedScope.data).Count)"
# restore role scopes and user role
Api Put '/system/role' @{roleId=3;roleName='部门经理';roleKey='manager';roleSort=3;dataScope='4';deptIds=@();status='0'} $token|Out-Null
Api Put '/system/user' @{userId=2;userName='zhangsan';nickName='张三';deptId=103;roleIds=@(2);status='0'} $token|Out-Null
# State transition rejection matrix
$matrixDocs=@()
foreach($status in 0..3){
  $num="P2-MATRIX-IN-$suffix-$status"
  $created=Api Post '/oa/incomingDoc' @{docNumber=$num;docTitle="矩阵收文$status";sourceUnit='测试';receiveDate='2026-09-20';docLevel='1';docSecret='1';status='0'} $token
  $list=Api Get ("/oa/incomingDoc/list?pageNum=1&pageSize=20&docNumber=$num") $null $token
  $id=($list.rows|Select-Object -First 1).docId
  if($status -ge 1){Api Post ("/oa/doc/action/1/$id") @{action='receive'} $token|Out-Null}
  if($status -ge 2){Api Post ("/oa/doc/action/1/$id") @{action='complete'} $token|Out-Null}
  if($status -ge 3){Api Post ("/oa/doc/action/1/$id") @{action='archive';archiveCode="P2-M-ARCH-IN-$suffix-$status"} $token|Out-Null}
  $matrixDocs += [pscustomobject]@{Type='1';Id=$id;Status="$status"}
}
foreach($status in 0..3){
  $num="P2-MATRIX-OUT-$suffix-$status"
  Api Post '/oa/outgoingDoc' @{docNumber=$num;docTitle="矩阵发文$status";draftUnit='测试部门';draftDate='2026-09-20';docLevel='1';docSecret='1';mainSend='测试单位';status='0'} $token|Out-Null
  $list=Api Get ("/oa/outgoingDoc/list?pageNum=1&pageSize=20&docNumber=$num") $null $token
  $id=($list.rows|Select-Object -First 1).docId
  if($status -ge 1){Api Post ("/oa/doc/action/2/$id") @{action='submit'} $token|Out-Null}
  if($status -ge 2){Api Post ("/oa/doc/action/2/$id") @{action='sign'} $token|Out-Null}
  if($status -ge 3){Api Post ("/oa/doc/action/2/$id") @{action='archive';archiveCode="P2-M-ARCH-OUT-$suffix-$status"} $token|Out-Null}
  $matrixDocs += [pscustomobject]@{Type='2';Id=$id;Status="$status"}
}
$allActions=@('receive','submit','approve','countersign','add_sign','cancel_sign','return','withdraw','transfer','delegate','agent','circulate','urge','complete','sign','archive')
$legalActions=@{
  '1'=@{
    '0'=@('receive')
    '1'=@('approve','countersign','add_sign','cancel_sign','return','withdraw','transfer','delegate','agent','circulate','urge','complete')
    '2'=@('archive','circulate','urge')
    '3'=@()
  }
  '2'=@{
    '0'=@('submit')
    '1'=@('approve','countersign','add_sign','cancel_sign','return','withdraw','transfer','delegate','agent','circulate','urge','sign')
    '2'=@('archive','circulate','urge')
    '3'=@()
  }
}
$illegalChecked=0
foreach($entry in $matrixDocs){
  foreach($action in $allActions){
    if($action -in @($legalActions[$entry.Type][$entry.Status])){continue}
    $illegal=Api Post ("/oa/doc/action/$($entry.Type)/$($entry.Id)") @{action=$action;opinion='矩阵测试';targetUserId=2;targetUserIds=@(2,3);approvalId=1} $token
    $illegalChecked++
    Check "状态$($entry.Status)非法操作$action" ($illegal.code -eq 400) "type=$($entry.Type) code=$($illegal.code) msg=$($illegal.msg)"
  }
}
Check '非法状态矩阵完整执行' ($illegalChecked -gt 90) "checked=$illegalChecked"
$blankAction=Api Post ("/oa/doc/action/1/$inId") @{action=''} $token; Check '空动作拦截' ($blankAction.code -eq 400) $blankAction.msg
$badDocType=Api Post ("/oa/doc/action/9/$inId") @{action='receive'} $token; Check '非法公文类型拦截' ($badDocType.code -eq 400) $badDocType.msg

# File upload, download and ownership checks
$tempFile=Join-Path (Resolve-Path 'ruoyi-admin\target').Path ("phase2-upload-$suffix.txt")
[System.IO.File]::WriteAllText($tempFile,'phase2 upload permission test')
$fileName=[System.IO.Path]::GetFileName($tempFile)
$upload=Invoke-RestMethod -Uri ($BaseUrl+'/oa/document/upload') -Method Post -Headers @{Authorization='Bearer '+$token} -Form @{file=Get-Item $tempFile;documentType='phase2'} -TimeoutSec 20
Check '文件上传成功' ($upload.code -eq 200) $upload.msg
$blockedFile=Join-Path (Resolve-Path 'ruoyi-admin\target').Path ("phase2-blocked-$suffix.html")
[System.IO.File]::WriteAllText($blockedFile,'<script>alert(1)</script>')
$blockedUpload=Invoke-RestMethod -Uri ($BaseUrl+'/oa/document/upload') -Method Post -Headers @{Authorization='Bearer '+$token} -Form @{file=Get-Item $blockedFile;documentType='phase2'} -TimeoutSec 20
Check '危险文件类型拦截' ($blockedUpload.code -eq 400) $blockedUpload.msg
[System.IO.File]::Delete($blockedFile)
$adminFiles=Api Get ("/oa/document/list?pageNum=1&pageSize=100&documentName=$fileName") $null $token
$fileDoc=$adminFiles.rows|Select-Object -First 1
Check '管理员可见上传文件' ($null -ne $fileDoc) "count=$($adminFiles.total)"
$userFiles=Api Get ("/oa/document/list?pageNum=1&pageSize=100&documentName=$fileName") $null $zh
Check '普通用户不可见他人文件' (@($userFiles.rows).Count -eq 0) "count=$($userFiles.total)"
$adminDownload=Invoke-WebRequest -Uri ($BaseUrl+'/oa/document/download/'+$fileDoc.documentId) -Headers @{Authorization='Bearer '+$token} -TimeoutSec 20
Check '管理员下载文件成功' ($adminDownload.StatusCode -eq 200 -and $adminDownload.RawContentLength -gt 0) "length=$($adminDownload.RawContentLength)"
$userDownloadStatus=0
try{Invoke-WebRequest -Uri ($BaseUrl+'/oa/document/download/'+$fileDoc.documentId) -Headers @{Authorization='Bearer '+$zh} -TimeoutSec 20|Out-Null}catch{$userDownloadStatus=[int]$_.Exception.Response.StatusCode}
Check '普通用户下载他人文件被拦截' ($userDownloadStatus -eq 404) "status=$userDownloadStatus"
$userDelete=Api Delete ("/oa/document/"+$fileDoc.documentId) $null $zh
Check '普通用户删除他人文件被拦截' ($userDelete.code -eq 403) $userDelete.msg
$adminDeleteFile=Api Delete ("/oa/document/"+$fileDoc.documentId) $null $token
Check '管理员删除文件成功' ($adminDeleteFile.code -eq 200) $adminDeleteFile.msg
[System.IO.File]::Delete($tempFile)

# audit logs
$logs=Api Get '/system/log/list?pageNum=1&pageSize=100' $null $token; Check '审计日志生成' (@($logs.rows).Count  -gt  0) "count=$($logs.total)"
$loginLogs=Api Get '/system/log/list?pageNum=1&pageSize=100&title=SysLoginController' $null $token
$loginLog=$loginLogs.rows|Where-Object {$_.operParam -match 'password'}|Select-Object -First 1; Check '登录日志存在' ($null -ne $loginLog) "count=$($loginLogs.total)"
Check '登录参数密码脱敏' ($loginLog.operParam -match '\*\*\*' -and $loginLog.operParam -notmatch 'admin123') $loginLog.operParam
Check '登录响应token脱敏' ($loginLog.jsonResult -match '\*\*\*' -and $loginLog.jsonResult -notmatch 'eyJ') $loginLog.jsonResult
$badActionLogs=Api Get '/system/log/list?pageNum=1&pageSize=500&title=OaDocProcessController' $null $token
$badActionLog=$badActionLogs.rows|Where-Object {$_.operUrl -eq '/oa/doc/action/1/99999999'}|Select-Object -First 1
Check '失败操作审计准确' ($null -ne $badActionLog -and $badActionLog.status -eq '1' -and $badActionLog.operatorName -eq 'admin' -and $badActionLog.errorMsg -match '公文不存在') "log=$($badActionLog|ConvertTo-Json -Compress)"
$export=Invoke-WebRequest -Uri ($BaseUrl+'/system/log/export') -Headers @{Authorization='Bearer '+$token} -UseBasicParsing -TimeoutSec 20; Check '审计日志导出' ($export.StatusCode -eq  200 -and $export.RawContentLength -gt  0) "length=$($export.RawContentLength)"
Check '审计归档不含明文凭证' ($export.Content -notmatch 'admin123' -and $export.Content -notmatch 'eyJ') "length=$($export.RawContentLength)"
$clean=Api Delete '/system/log/clean?days=3650' $null $token; Check '审计日志清理接口' ($clean.code -eq  200) $clean.msg
# cleanup documents/users
foreach($matrix in $matrixDocs){ if($matrix.Id){Api Delete ("/oa/"+($(if($matrix.Type -eq '1'){'incomingDoc'}else{'outgoingDoc'}))+"/"+$matrix.Id) $null $token|Out-Null} }
foreach($u in $createdUsers){ if($u){Api Delete ("/system/user/"+$u.userId) $null $token|Out-Null} }
if($unauthDocId){Api Delete ("/oa/incomingDoc/"+$unauthDocId) $null $token|Out-Null}
if($flowDocId){Api Delete ("/oa/outgoingDoc/"+$flowDocId) $null $token|Out-Null}
if($bindingDocId){Api Delete ("/oa/outgoingDoc/"+$bindingDocId) $null $token|Out-Null}
if($deniedFlowId){Api Delete ("/oa/docFlow/"+$deniedFlowId) $null $token|Out-Null}
if($adminFlowId){Api Delete ("/oa/docFlow/"+$adminFlowId) $null $token|Out-Null}
Api Delete ("/oa/incomingDoc/"+$inId) $null $token|Out-Null; Api Delete ("/oa/outgoingDoc/"+$outId) $null $token|Out-Null
$results|Format-Table -AutoSize; $pass=@($results|Where-Object Result -eq 'PASS').Count; $fail=@($results|Where-Object Result -eq 'FAIL').Count; "SUMMARY PASS=$pass FAIL=$fail"
