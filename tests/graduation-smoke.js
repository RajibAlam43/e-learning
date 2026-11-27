// API-mocked browser coverage; run against the local production frontend on port 3024.
// playwright-cli -s=elearning-recorder run-code --filename=tests/graduation-smoke.js
async page => {
  page.setDefaultTimeout(15000);
  await page.unrouteAll({ behavior: 'wait' });
  const origin='http://127.0.0.1:3024',userId='22222222-2222-4222-8222-222222222222';
  const programId='11111111-1111-4111-8111-111111111111',expiredId='44444444-4444-4444-8444-444444444444';
  const core='33333333-3333-4333-8333-333333333333',elective='55555555-5555-4555-8555-555555555555';
  let role='STUDENT',completed=false,issued=false,rejectOnce=true;
  const mutations=[],errors=[],layouts=[];
  page.on('pageerror',e=>errors.push(e.message));
  const report=()=>({programId,title:'Islamic Studies Diploma',programType:'DIPLOMA',enrollmentStatus:'ACTIVE',accessActive:true,
    expiresAt:'2029-11-26T15:00:00Z',completedAt:completed?'2026-11-26T15:00:00Z':null,
    status:completed?'COMPLETED':'IN_PROGRESS',requirementsMet:completed,canCheckCompletion:!completed,canRequestCertificate:completed,
    creditsRemaining:0,creditProgress:{requiredCredits:6,earnedCredits:completed?9:6,electiveCreditLimit:6,electiveCreditsCounted:6,
      mandatoryCoursesCompleted:completed?1:0,mandatoryCoursesRequired:1,complete:completed},
    courses:[{courseTemplateId:core,title:'Required Arabic Foundation',mandatory:true,expectedCredits:3,awardedCredits:completed?3:0,countedCredits:completed?3:0,source:completed?'EXEMPTION':null},
      {courseTemplateId:elective,title:'Islamic History',mandatory:false,expectedCredits:6,awardedCredits:6,countedCredits:6,source:'COMPLETION',achievedAt:'2026-11-26T15:00:00Z'}],
    certificateId:issued?'cert-test':null,certificateCode:issued?'GII-CERT-TEST':null,certificateRevoked:false});
  const expired=()=>({...report(),programId:expiredId,title:'Expired program',status:'ACCESS_INACTIVE',accessActive:false,completedAt:null,canCheckCompletion:false,canRequestCertificate:false,certificateId:null,certificateCode:null});
  const profile=()=>({userId,fullName:'Fatima Ahmed',email:null,phone:'+8801700000000',semesters:[{id:'spring',code:'SP27',name:'Spring 2027'}],
    academicRecord:{userId,totalCredits:completed?9:6,achievements:[{achievementId:'history',courseTemplateId:elective,courseTitle:'Islamic History',credits:6,source:'COMPLETION',semesterId:'spring',achievedAt:'2026-11-26T15:00:00Z'},
      ...(completed?[{achievementId:'waiver',courseTemplateId:core,courseTitle:'Required Arabic Foundation',credits:3,source:'EXEMPTION',semesterId:null,achievedAt:'2026-11-26T15:00:00Z'}]:[])]},programs:[report(),expired()]});
  await page.route('**/*',async route=>{
    const req=route.request(),url=new URL(req.url()),path=url.pathname;
    const reply=(data,status=200)=>route.fulfill({status,contentType:'application/json',body:JSON.stringify(data)});
    if(path==='/api/auth/refresh')return reply({accessToken:'graduation-browser-test'});
    if(path==='/me')return reply({id:userId,fullName:role==='STUDENT'?'Fatima Ahmed':'Academic Admin',roles:[role]});
    if(req.resourceType()==='document'||req.headers().rsc||req.headers()['next-router-prefetch'])return route.continue();
    if(path==='/student/academic-progress'||path===`/admin/students/${userId}/academic-progress`)return reply(profile());
    if(path===`/student/collections/${programId}/graduation`)return reply(report());
    if(path.endsWith('/graduation/check')){
      mutations.push({path,method:req.method()});
      if(rejectOnce){rejectOnce=false;return reply({detail:'The academic record changed. Refresh and check again.'},409);}
      completed=true;return reply(report());
    }
    if(path===`/student/collections/${programId}/certificate/generate`){mutations.push({path,method:req.method()});issued=true;return reply({message:'Certificate issued',certificateId:'cert-test'});}
    if(path===`/student/collections/${programId}`)return reply({collectionId:programId,collectionName:'Islamic Studies Diploma',collectionType:'DIPLOMA',progressPercentage:completed?100:0});
    if(path===`/student/collections/${programId}/curriculum`)return reply({accessActive:true,expiresAt:'2029-11-26T15:00:00Z',availableCourses:[]});
    if(path==='/public/settings')return reply([]);
    return route.continue();
  });
  async function auth(){await page.context().addCookies([{name:'session',value:'1',url:origin},{name:'locale',value:'en',url:origin},{name:'user_roles',value:encodeURIComponent(JSON.stringify([role])),url:origin}]);}
  await auth();await page.goto(origin+`/student/collections/${programId}`);
  await page.getByRole('heading',{name:'Graduation progress',exact:true}).waitFor();
  if(!await page.getByRole('button',{name:'Get program certificate',exact:true}).isDisabled())throw Error('Missing mandatory credit must block certificate');
  await page.getByText('Counted credits and remaining courses',{exact:true}).click();
  await page.getByText('Mandatory course credit still required',{exact:true}).waitFor();
  await page.getByRole('button',{name:'Check graduation',exact:true}).click();
  await page.getByRole('alert').filter({hasText:'The academic record changed'}).waitFor();
  await page.getByRole('button',{name:'Check graduation',exact:true}).click();
  await page.getByText('Program completion is recorded.',{exact:true}).waitFor();
  await page.getByRole('button',{name:'Get program certificate',exact:true}).click();
  await page.getByText('Your certificate is ready in My Certificates.',{exact:true}).waitFor();
  await page.getByRole('button',{name:'Get certificate',exact:true}).waitFor();
  await page.getByRole('link',{name:'Academic record and program progress',exact:true}).click();
  await page.getByRole('heading',{name:'Academic record',exact:true}).waitFor();
  await page.getByText('3 credits · Approved waiver',{exact:true}).waitFor();
  await page.getByText('SP27 · Spring 2027',{exact:false}).waitFor();
  const inactive=page.getByRole('region',{name:'Graduation progress for Expired program',exact:true});
  if(!await inactive.getByRole('button',{name:'Get program certificate',exact:true}).isDisabled())throw Error('Expired program must block new certificate');
  for(const width of [1280,390]){await page.setViewportSize({width,height:900});await page.getByRole('heading',{name:'Academic record',exact:true}).scrollIntoViewIfNeeded();const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth);if(overflow)throw Error('Student record overflow '+width);await page.screenshot({path:`/tmp/gii-step6-ui/student-${width}.png`,animations:'disabled'});layouts.push({role,width,overflow});}
  role='ADMIN';await page.evaluate(()=>localStorage.clear());await auth();await page.goto(origin+`/admin/students/${userId}/academics`);
  await page.getByRole('heading',{name:'Fatima Ahmed',exact:true}).waitFor();
  await page.getByRole('link',{name:'Manual enrollment',exact:true}).waitFor();
  await page.getByRole('link',{name:'Verify certificate',exact:true}).waitFor();
  if(await page.getByRole('button',{name:'Get certificate',exact:true}).count())throw Error('Admin view must not issue a student certificate as admin');
  for(const width of [1280,390]){await page.setViewportSize({width,height:900});const overflow=await page.locator('main').evaluate(e=>e.scrollWidth>e.clientWidth);if(overflow)throw Error('Admin record overflow '+width);await page.screenshot({path:`/tmp/gii-step6-ui/admin-${width}.png`,animations:'disabled'});layouts.push({role,width,overflow});}
  completed=false;issued=false;await page.reload();
  const adminProgram=page.getByRole('region',{name:'Graduation progress for Islamic Studies Diploma',exact:true});
  await adminProgram.getByRole('button',{name:'Check graduation',exact:true}).click();
  await adminProgram.getByText('Program completion is recorded.',{exact:true}).waitFor();
  if(!mutations.some(m=>m.path.startsWith(`/admin/students/${userId}/programs/`)))throw Error('Admin check must use the admin endpoint');
  await adminProgram.getByText('Counted credits and remaining courses',{exact:true}).click();
  await adminProgram.getByRole('heading',{name:'Required Arabic Foundation',exact:true}).scrollIntoViewIfNeeded();
  await page.screenshot({path:'/tmp/gii-step6-ui/admin-breakdown-390.png',animations:'disabled'});
  if(errors.length)throw Error(errors.join('\n'));
  return {mutations,layouts,browserErrors:errors};
}
