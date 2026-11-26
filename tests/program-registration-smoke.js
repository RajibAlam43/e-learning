// All API calls are mocked; this test does not change a hosted environment.
// playwright-cli -s=elearning-recorder run-code --filename=tests/program-registration-smoke.js
async page => {
 const origin='http://127.0.0.1:3024',programId='11111111-1111-4111-8111-111111111111',studentId='22222222-2222-4222-8222-222222222222';
 const first='33333333-3333-4333-8333-333333333333',next='44444444-4444-4444-8444-444444444444',planned='55555555-5555-4555-8555-555555555555';
 let role='STUDENT', rejectOnce=true, active=null, history=[], mutations=[], errors=[];
 const offer=(id,name)=>({courseId:id,name,slug:name.toLowerCase().replaceAll(' ','-'),studyMode:'COHORT_BASED',semester:{name:'Spring 2027'},startsAt:'2027-01-01T09:00:00Z',endsAt:'2027-05-31T09:00:00Z',credits:3,accessExpiresAt:'2027-08-31T23:00:00Z',seatsAvailable:5,canRegister:!active,blockedReason:active?'Insufficient elective credit allowance':null,missingPrerequisites:[]});
 const curriculum=()=>({graduationRequirements:{requiredCredits:6,electiveCreditLimit:3},creditProgress:{requiredCredits:6,earnedCredits:0,electiveCreditsCounted:0,electiveCreditLimit:3,mandatoryCoursesCompleted:0,mandatoryCoursesRequired:1},accessActive:true,expiresAt:'2030-11-26T23:00:00Z',electiveAllowance:{limit:3,used:active?3:0,remaining:active?0:3},availableCourses:[{courseId:planned,courseTemplateId:planned,title:'Planned Core',credits:3,mandatoryForGraduation:true,status:'DRAFT',enrolled:false,offerings:[]},...[first,next].map((id,index)=>({courseId:id,courseTemplateId:id,title:index?'Arabic Elective Two':'Arabic Elective One',credits:3,mandatoryForGraduation:false,status:'PUBLISHED',enrolled:active?.courseId===id,registration:active?.courseId===id?active:null,offerings:[offer(id,index?'Evening cohort':'Morning cohort'),...(index?[]:[{...offer('66666666-6666-4666-8666-666666666666','Full cohort'),seatsAvailable:0,canRegister:false,blockedReason:'Course capacity has been reached'}])]}))]});
 page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/*',async route=>{
  const req=route.request(),url=new URL(req.url()),path=url.pathname,method=req.method();
  const reply=(body,status=200)=>route.fulfill({status,contentType:'application/json',headers:{'access-control-allow-origin':origin,'access-control-allow-credentials':'true','access-control-allow-headers':'*','access-control-allow-methods':'GET,POST,OPTIONS'},body:JSON.stringify(body)});
  if(method==='OPTIONS')return reply({});
  if(path==='/api/auth/refresh')return reply({accessToken:'registration-ui-token'});
  if(path==='/me')return reply({id:role==='STUDENT'?studentId:'77777777-7777-4777-8777-777777777777',fullName:role==='STUDENT'?'Fatima Ahmed':'Academic Admin',roles:[role]});
  if(req.resourceType()==='document'||req.headers().rsc||req.headers()['next-router-prefetch'])return route.continue();
  if(path===`/student/collections/${programId}`)return reply({collectionId:programId,collectionName:'Islamic Studies Diploma',collectionType:'DIPLOMA',progressPercentage:0,courses:[]});
  if(path===`/student/collections/${programId}/curriculum`)return reply(curriculum());
  if(path===`/student/collections/${programId}/registrations`){
   const body=JSON.parse(req.postData());mutations.push({path,body});
   if(rejectOnce){rejectOnce=false;return reply({detail:'The last seat was just taken. Choose another offering or try again.'},409);}
   active={id:'88888888-8888-4888-8888-88888888888'+(body.courseId===first?'1':'2'),programId,programTitle:'Islamic Studies Diploma',courseId:body.courseId,courseTemplateId:body.courseId,courseTitle:body.courseId===first?'Arabic Elective One':'Arabic Elective Two',cohortName:body.courseId===first?'Morning cohort':'Evening cohort',credits:3,mandatory:false,status:'ACTIVE',canWithdraw:true,registeredAt:'2026-11-26T12:00:00Z'};history.unshift(active);return reply(active);
  }
  if(path===`/admin/students/${studentId}/program-registrations`)return reply(history);
  if(path.startsWith(`/admin/students/${studentId}/program-registrations/`)&&path.endsWith('/withdraw')){
   const body=JSON.parse(req.postData());mutations.push({path,body});const old=active;Object.assign(old,{status:'WITHDRAWN',canWithdraw:false,withdrawalReason:body.reason,withdrawnByName:'Academic Admin',withdrawnAt:'2026-11-26T13:00:00Z'});active=null;return reply(old);
  }
  if(path==='/public/settings')return reply([]);
  if(url.origin===origin)return route.continue();return route.abort();
 });
 async function cookies(){await page.context().addCookies([{name:'session',value:'1',url:origin},{name:'locale',value:'en',url:origin},{name:'user_roles',value:encodeURIComponent(JSON.stringify([role])),url:origin}]);}
 async function switchRole(value,path){role=value;await page.evaluate(()=>localStorage.clear());await cookies();await page.goto(origin+path);}
 await cookies();await page.goto(origin+`/student/collections/${programId}`);
 await page.getByRole('heading',{name:'Program courses and requirements',exact:true}).waitFor();
 await page.getByText('No published offerings yet. Check back for an upcoming semester.',{exact:true}).waitFor();
 await page.getByText('Choose an offering for Arabic Elective One',{exact:true}).click();
 const full=page.getByRole('button',{name:'Register for Full cohort',exact:true});if(!await full.isDisabled())throw Error('Full cohort must be disabled');
 await page.getByRole('button',{name:'Register for Morning cohort',exact:true}).click();await page.getByRole('alert').filter({hasText:'The last seat was just taken'}).waitFor();
 await page.getByRole('button',{name:'Register for Morning cohort',exact:true}).click();await page.getByRole('link',{name:'Continue course',exact:true}).waitFor();
 await page.getByText('Elective allowance: 3 / 3 credits reserved or credited · 0 remaining.',{exact:true}).waitFor();
 await page.getByText('Choose an offering for Arabic Elective Two',{exact:true}).click();if(!await page.getByRole('button',{name:'Register for Evening cohort',exact:true}).isDisabled())throw Error('Elective allowance must disable extra registration');
 await page.getByRole('link',{name:'Buy this course separately',exact:true}).waitFor();
 const layouts=[];
 for(const width of [1280,390]){await page.setViewportSize({width,height:900});const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth);if(overflow)throw Error('Student overflow '+width);await page.screenshot({path:`/tmp/gii-step4-ui/student-${width}.png`,fullPage:true,animations:'disabled'});layouts.push({screen:'student',width,overflow});}
 await switchRole('ADMIN',`/admin/students/${studentId}/registrations`);await page.getByRole('heading',{name:'Program registrations',exact:true}).waitFor();await page.getByRole('button',{name:'Withdraw Arabic Elective One',exact:true}).click();
 await page.getByLabel('Withdrawal reason',{exact:true}).fill('Requested semester change');await page.getByRole('button',{name:'Withdraw registration',exact:true}).click();await page.getByText('Requested semester change · Academic Admin',{exact:true}).waitFor();
 for(const width of [1280,390]){await page.setViewportSize({width,height:900});const overflow=await page.locator('main').evaluate(e=>e.scrollWidth>e.clientWidth);if(overflow)throw Error('Admin overflow '+width);await page.screenshot({path:`/tmp/gii-step4-ui/admin-${width}.png`,animations:'disabled'});layouts.push({screen:'admin',width,overflow});}
 await switchRole('STUDENT',`/student/collections/${programId}`);await page.getByText('Elective allowance: 0 / 3 credits reserved or credited · 3 remaining.',{exact:true}).waitFor();await page.getByText('Choose an offering for Arabic Elective Two',{exact:true}).click();await page.getByRole('button',{name:'Register for Evening cohort',exact:true}).click();await page.getByRole('link',{name:'Continue course',exact:true}).waitFor();
 if(active?.courseId!==next||history.length!==2||history[1].status!=='WITHDRAWN')throw Error('Withdrawal and re-selection history incorrect');if(errors.length)throw Error(errors.join('\n'));
 return {mutations,layouts,history:history.map(r=>({course:r.courseTitle,status:r.status})),browserErrors:errors};
}
