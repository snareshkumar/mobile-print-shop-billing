export const money=n=>new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format(Number(n)||0);
export function today(){return new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Kolkata',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());}
export const billNo=n=>`NP-${String(n).padStart(6,'0')}`;
export function periodRange(kind, date=today()){
 const d=new Date(date+'T12:00:00Z');let from=date,to=date;
 if(kind==='week'){d.setUTCDate(d.getUTCDate()-((d.getUTCDay()+6)%7));from=d.toISOString().slice(0,10);}
 if(kind==='month')from=date.slice(0,7)+'-01';
 return {from,to};
}
export function estimate(items){
 let subtotal=0,discount=0,tax=0;
 for(const i of items){const p=Math.round(Number(i.price)*100),d=Math.round(Number(i.discount||0)*100);const sub=p*Number(i.quantity);subtotal+=sub;discount+=d;tax+=Math.round((sub-d)*Number(i.tax_rate||0)/100);}
 return {subtotal:subtotal/100,discount:discount/100,tax:tax/100,total:(subtotal-discount+tax)/100};
}
export function csv(rows){if(!rows.length)return '';const cell=x=>'"'+String(x??'').replace(/^[=+@\-\t\r]/,"'$&").replaceAll('"','""')+'"';return Object.keys(rows[0]).map(cell).join(',')+'\r\n'+rows.map(r=>Object.values(r).map(cell).join(',')).join('\r\n');}
export function downloadCSV(name,rows){const u=URL.createObjectURL(new Blob(['\uFEFF'+csv(rows)],{type:'text/csv;charset=utf-8;'}));const a=document.createElement('a');a.href=u;a.download=name;a.click();URL.revokeObjectURL(u);}
