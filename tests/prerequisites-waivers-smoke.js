// Local UI smoke test: all API/storage calls are intercepted, no live writes.
// playwright-cli -s=elearning-recorder run-code --filename=tests/prerequisites-waivers-smoke.js
async page => {
 const origin = 'http://127.0.0.1:3024';
 const studentId='11111111-1111-4111-8111-111111111111', adminId='22222222-2222-4222-8222-222222222222';
 const courseId='33333333-3333-4333-8333-333333333333', templateId='44444444-4444-4444-8444-444444444444';
 const requiredId='55555555-5555-4555-8555-555555555555', waiverId='66666666-6666-4666-8666-666666666666';
 let role='STUDENT', requests=[], required=[], mutations=[], errors=[];
 const user=()=>({id:role==='STUDENT'?studentId:adminId,fullName:role==='STUDENT'?'Fatima Ahmed':'Academic Admin',email:'academic@example.com',roles:[role]});
 page.on('pageerror', e=>errors.push(e.message));
 const courses=[{courseId,title:'Arabic II',slug:'arabic-ii',courseTemplateId:templateId,credits:3,status:'PUBLISHED',studyMode:'SELF_PACED',sections:[],instructors:[],categories:[]},{courseId:'77777777-7777-4777-8777-777777777777',title:'Arabic I',courseTemplateId:requiredId,credits:3,status:'DRAFT'}];
 await page.route('**/*',async route=>{
  const request=route.request(),url=new URL(request.url()),path=url.pathname,method=request.method();
  const reply=(body,status=200)=>route.fulfill({status,contentType:'application/json',headers:{'access-control-allow-origin':origin,'access-control-allow-credentials':'true','access-control-allow-headers':'*','access-control-allow-methods':'GET,POST,PUT,PATCH,OPTIONS'},body:JSON.stringify(body)});
  if(method==='OPTIONS')return reply({});
  if(path==='/api/auth/refresh')return reply({accessToken:'waiver-test-token'});
  if(path==='/me')return reply(user());
  if(request.resourceType()==='document'||request.headers().rsc||request.headers()['next-router-prefetch'])return route.continue();
  if(path==='/student/waivers/courses')return reply([{courseTemplateId:requiredId,title:'Arabic I',credits:3}]);
  if(path==='/student/waivers/evidence-upload')return reply({objectKey:`waiver-evidence/${studentId}/88888888-8888-4888-8888-888888888888.pdf`,uploadUrl:origin+'/mock-private-upload',expiresAt:'2026-11-27T00:00:00Z'});
  if(path==='/mock-private-upload'){mutations.push({path,method});return reply({});}
  if(path==='/student/waivers'){
   if(method==='GET')return reply(requests);
   const body=JSON.parse(request.postData());mutations.push({path,body});
   if(!body.evidenceObjectKey?.startsWith(`waiver-evidence/${studentId}/`))throw Error('Private evidence not attached');
   const item={id:waiverId,userId:studentId,studentName:'Fatima Ahmed',studentContact:'fatima@example.com',courseTemplateId:requiredId,courseTitle:'Arabic I',requestedCredits:3,currentCredits:3,reason:body.reason,hasEvidence:true,status:'PENDING',createdAt:'2026-11-26T12:00:00Z'};requests=[item];return reply(item);
  }
  if(path==='/admin/waivers')return reply(requests.filter(item=>item.status===url.searchParams.get('status')));
  if(path===`/admin/waivers/${waiverId}/review`){const body=JSON.parse(request.postData());mutations.push({path,body});requests[0]={...requests[0],status:body.decision,reviewReason:body.reason,reviewerName:'Academic Admin',awardedCredits:3};return reply(requests[0]);}
  if(path===`/admin/courses/${courseId}/prerequisites`){if(method==='PUT'){const body=JSON.parse(request.postData());mutations.push({path,body});required=body.courseTemplateIds.map(id=>({courseTemplateId:id,title:'Arabic I',credits:3}));}return reply({courseTemplateId:templateId,requiredCourses:required});}
  if(path===`/admin/courses/${courseId}`)return reply(courses[0]);
  if(path==='/admin/courses')return reply(courses);
  if(path==='/admin/semesters'||path==='/admin/instructors'||path==='/public/settings')return reply([]);
  if(url.origin===origin)return route.continue();return route.abort();
 });
 const cookies=async()=>page.context().addCookies([{name:'session',value:'1',url:origin},{name:'locale',value:'en',url:origin},{name:'user_roles',value:encodeURIComponent(JSON.stringify([role])),url:origin}]);
 await cookies();await page.goto(origin+`/student/waivers?courseTemplateId=${requiredId}`);
 await page.getByRole('heading',{name:'Credit waivers',exact:true}).waitFor();
 if(await page.getByRole('combobox',{name:'Course',exact:true}).inputValue()!==requiredId)throw Error('Course preselection failed');
 await page.getByLabel('Reason',{exact:true}).fill('I completed an equivalent Arabic curriculum and have attached my transcript.');
 await page.getByLabel('Supporting file (optional)').setInputFiles({name:'transcript.pdf',mimeType:'application/pdf',buffer:Buffer.from('%PDF-1.7 test supporting file')});
 await page.getByRole('button',{name:'Submit waiver request',exact:true}).click();
 await page.getByText('PENDING · 3 credits requested',{exact:false}).waitFor();
 const layouts=[];
 for(const width of [1280,390]){await page.setViewportSize({width,height:900});const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth);if(overflow)throw Error('Student overflow '+width);await page.screenshot({path:`/tmp/gii-step2-ui/student-${width}.png`,fullPage:true});layouts.push({screen:'student',width,overflow});}
 role='ADMIN';await page.evaluate(()=>localStorage.clear());await cookies();await page.goto(origin+'/admin/waivers');
 await page.getByRole('button',{name:'Review request',exact:true}).click();
 await page.getByLabel('Review reason',{exact:true}).fill('Equivalent course outcomes and transcript verified.');
 await page.getByRole('button',{name:'Approve and award credits',exact:true}).click();
 await page.getByText('No pending waiver requests on this page.').waitFor();
 await page.getByLabel('Request status').selectOption('APPROVED');
 await page.getByText('Approved · 3 course credits on record',{exact:false}).waitFor();
 for(const width of [1280,390]){await page.setViewportSize({width,height:900});const overflow=await page.locator('main').evaluate(el=>el.scrollWidth>el.clientWidth);if(overflow)throw Error('Admin overflow '+width);await page.screenshot({path:`/tmp/gii-step2-ui/admin-${width}.png`});layouts.push({screen:'admin',width,overflow});}
 await page.setViewportSize({width:1280,height:900});await page.goto(origin+'/admin/courses/'+courseId);
 await page.getByRole('heading',{name:'Required courses',exact:true}).waitFor();
 await page.getByRole('checkbox',{name:'Arabic I · 3 credits'}).check();
 await page.getByRole('button',{name:'Save prerequisites',exact:true}).click();
 await page.getByText('Prerequisites saved.',{exact:true}).waitFor();
 if(required.length!==1)throw Error('Prerequisite save failed');
 await page.getByRole('heading',{name:'Required courses',exact:true}).scrollIntoViewIfNeeded();await page.screenshot({path:'/tmp/gii-step2-ui/prerequisites.png'});
 if(errors.length)throw Error(errors.join('\n'));
 return {mutations,layouts,browserErrors:errors};
}
