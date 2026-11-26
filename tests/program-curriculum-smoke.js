// Run against the local production frontend on :3024. All APIs are mocked.
// playwright-cli -s=elearning-recorder run-code --filename=tests/program-curriculum-smoke.js
async page => {
 const origin='http://127.0.0.1:3024', id='11111111-1111-4111-8111-111111111111';
 const core='22222222-2222-4222-8222-222222222222', elective='33333333-3333-4333-8333-333333333333', future='44444444-4444-4444-8444-444444444444';
 let role='ADMIN', mutations=[], errors=[];
 const courses=[{courseId:core,courseTemplateId:'55555555-5555-4555-8555-555555555555',title:'Foundation',credits:3,status:'DRAFT'},{courseId:elective,courseTemplateId:'66666666-6666-4666-8666-666666666666',title:'Arabic Elective',credits:3,status:'PUBLISHED'},{courseId:future,courseTemplateId:'77777777-7777-4777-8777-777777777777',title:'Future Arabic',credits:3,status:'DRAFT'}];
 let program={collectionId:id,title:'Islamic Studies Diploma',slug:'diploma',collectionType:'DIPLOMA',priceBdt:12000,status:'DRAFT',courses:[],programRequirements:{requiredCredits:null,electiveCreditLimit:0,expectedDurationMonths:null,accessDurationMonths:null,courses:[]}};
 let original;
 page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/*',async route=>{
  const request=route.request(), url=new URL(request.url()),path=url.pathname, method=request.method();
  const reply=(body,status=200)=>route.fulfill({status,contentType:'application/json',headers:{'access-control-allow-origin':origin,'access-control-allow-credentials':'true','access-control-allow-headers':'*','access-control-allow-methods':'GET,POST,PUT,PATCH,OPTIONS'},body:JSON.stringify(body)});
  if(method==='OPTIONS')return reply({});
  if(path==='/api/auth/refresh')return reply({accessToken:'program-ui-token'});
  if(path==='/me')return reply({id:'88888888-8888-4888-8888-888888888888',fullName:'Academic Admin',email:'academic@example.com',roles:[role]});
  if(request.resourceType()==='document'||request.headers().rsc||request.headers()['next-router-prefetch'])return route.continue();
  if(path==='/admin/collections')return reply([{...program,courseCount:program.courses.length}]);
  if(path==='/admin/collections/'+id)return reply(program);
  if(path==='/admin/courses')return reply(courses);
  if(path==='/admin/collections/'+id+'/curriculum'){
   const body=JSON.parse(request.postData());mutations.push({path,body});
   if(body.requiredCredits===99)return reply({detail:'Required credits cannot be reached within the elective credit allowance'},400);
   program={...program,accessDurationMonths:body.accessDurationMonths??null,courses:body.items.map(i=>({...i,courseTitle:courses.find(c=>c.courseId===i.courseId).title,courseTemplateId:courses.find(c=>c.courseId===i.courseId).courseTemplateId,credits:3,status:courses.find(c=>c.courseId===i.courseId).status})),programRequirements:{...body,accessDurationMonths:body.accessDurationMonths??body.expectedDurationMonths+24,courses:body.items.map(i=>({courseId:i.courseId,courseTemplateId:courses.find(c=>c.courseId===i.courseId).courseTemplateId,title:courses.find(c=>c.courseId===i.courseId).title,credits:3,mandatory:i.isMandatory,position:i.position}))}};
   original??=structuredClone(program.programRequirements);return reply(program);
  }
  if(path==='/admin/collections/'+id+'/publish'){program.status='PUBLISHED';return reply({});}
  if(path==='/student/collections/'+id)return reply({collectionId:id,collectionName:program.title,collectionType:'DIPLOMA',courses:[],progressPercentage:50});
  if(path==='/student/collections/'+id+'/curriculum')return reply({graduationRequirements:original,creditProgress:{requiredCredits:6,earnedCredits:3,electiveCreditsCounted:0,electiveCreditLimit:3,mandatoryCoursesCompleted:1,mandatoryCoursesRequired:1},expiresAt:'2030-05-26T12:00:00Z',accessActive:true,availableCourses:courses.map(c=>({courseId:c.courseId,courseTemplateId:c.courseTemplateId,title:c.title,slug:c.status==='PUBLISHED'?'arabic-elective':null,credits:3,status:c.status,mandatoryForGraduation:c.courseId===core,enrolled:false}))});
  if(path==='/public/settings')return reply([]);
  if(url.origin===origin)return route.continue();return route.abort();
 });
 async function saveCurriculum() {
  const response=page.waitForResponse(r=>new URL(r.url()).pathname==='/admin/collections/'+id+'/curriculum'&&r.request().method()==='PUT');
  await page.getByRole('button',{name:'Save curriculum',exact:true}).click(); await response;
 }
 const cookies=async()=>page.context().addCookies([{name:'session',value:'1',url:origin},{name:'locale',value:'en',url:origin},{name:'user_roles',value:encodeURIComponent(JSON.stringify([role])),url:origin}]);
 await cookies();await page.goto(origin+'/admin/programs');await page.getByRole('link',{name:'Manage program',exact:true}).click();
 await page.getByRole('heading',{name:'Curriculum and graduation requirements',exact:true}).waitFor();
 for(const course of [core,elective]) {await page.getByRole('combobox',{name:'Add a course',exact:true}).selectOption(course);await page.getByRole('button',{name:'Add course',exact:true}).click();}
 await page.getByLabel('Requirement for Arabic Elective',{exact:true}).selectOption('elective');
 await page.getByLabel('Credits required to graduate',{exact:true}).fill('6');await page.getByLabel('Elective credit allowance',{exact:false}).fill('3');await page.getByLabel('Expected duration (months)',{exact:true}).fill('18');
 await saveCurriculum();await page.getByText('Program curriculum saved. Existing purchasers keep their original graduation requirements.',{exact:true}).waitFor();
 if(program.programRequirements.accessDurationMonths!==42)throw Error('Incorrect default access duration');
 await page.getByRole('button',{name:'Publish program',exact:true}).click();await page.getByRole('button',{name:'Unpublish program',exact:true}).waitFor();
 await page.getByRole('combobox',{name:'Add a course',exact:true}).selectOption(future);await page.getByRole('button',{name:'Add course',exact:true}).click();await page.getByLabel('Requirement for Future Arabic',{exact:true}).selectOption('elective');await page.getByRole('button',{name:'Move Future Arabic up',exact:true}).click();
 await page.getByLabel('Access duration (months, optional)',{exact:false}).fill('60');await saveCurriculum();
 await page.waitForFunction(()=>document.querySelector('form button')!==null);
 await page.reload(); await page.getByLabel('Expected duration (months)',{exact:true}).fill('36'); await saveCurriculum();
 await page.getByText('Program curriculum saved. Existing purchasers keep their original graduation requirements.',{exact:true}).first().waitFor(); await page.reload();
 if(await page.getByLabel('Access duration (months, optional)',{exact:false}).inputValue()!=='60')throw Error('Explicit access override equal to default must survive reload');
 await page.getByLabel('Credits required to graduate',{exact:true}).fill('99');await saveCurriculum();await page.getByRole('alert').filter({hasText:'Required credits cannot be reached'}).waitFor();await page.getByLabel('Credits required to graduate',{exact:true}).fill('6');
 const layouts=[];
 for(const width of [1280,390]) {await page.setViewportSize({width,height:900});await page.getByRole('heading',{name:'Curriculum and graduation requirements',exact:true}).scrollIntoViewIfNeeded();const overflow=await page.locator('main').evaluate(el=>el.scrollWidth>el.clientWidth);if(overflow)throw Error('Program editor overflow '+width);await page.screenshot({path:`/tmp/gii-step3-ui/admin-${width}.png`});layouts.push({screen:'admin',width,overflow});}
 role='STUDENT';await page.evaluate(()=>localStorage.clear());await cookies();await page.goto(origin+'/student/collections/'+id);
 await page.getByRole('heading',{name:'Program courses and requirements',exact:true}).waitFor();await page.getByText('3 / 6 credits',{exact:false}).waitFor();
 if(await page.getByRole('link',{name:'Continue course',exact:true}).count())throw Error('Unregistered course should not offer study access');
 for(const width of [1280,390]) {await page.setViewportSize({width,height:900});const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth);if(overflow)throw Error('Student program overflow '+width);await page.screenshot({path:`/tmp/gii-step3-ui/student-${width}.png`,fullPage:true});layouts.push({screen:'student',width,overflow});}
 if(original.courses.length!==2||program.courses.length!==3)throw Error('Original and live course catalog were not separated');if(errors.length)throw Error(errors.join('\n'));
 return {saves:mutations.length,originalCourses:original.courses.length,liveCourses:program.courses.length,layouts,browserErrors:errors};
}
