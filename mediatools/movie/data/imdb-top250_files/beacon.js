if(typeof COMSCORE=="undefined"){
  var COMSCORE={};
}
COMSCORE.beacon=function(d){
  if(!d){return;}
  var a=1.7,e=document,h=e.location,g=512,
  c=function(i,j){
    if(i==null){return "";}
    i=(encodeURIComponent||escape)(i);
    if(j){
      i=i.substr(0,j);
    }
    return i;
  },
  f=[(h.protocol=="https:"?"https://sb":"http://b"),".scorecardresearch.com/b?","c1=",c(d.c1),"&c2=",c(d.c2),"&rn=",Math.random(),"&c7=",c(h.href,g),"&c3=",c(d.c3),"&c4=",c(d.c4,g),"&c5=",c(d.c5),"&c6=",c(d.c6),"&c10=",c(d.c10),"&c15=",c(d.c15),"&c16=",c(d.c16),"&c8=",c(e.title),"&c9=",c(e.referrer,g),"&cv=",a,d.r?"&r="+c(d.r,g):""].join("");
  f=f.length>2080?f.substr(0,2075)+"&ct=1":f;
  var b=new Image();
  b.onload=function(){};
  b.src=f;
  return f;
}; 
