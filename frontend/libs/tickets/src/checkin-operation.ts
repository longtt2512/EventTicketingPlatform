/** A QR is retained in memory only while its decision is unknown. Never persist it. */
export class CheckinOperation {
 current:{kind:'QR'|'MANUAL';value:string;key:string}|null=null;
 begin(kind:'QR'|'MANUAL',value:string):boolean{if(this.current)return false;this.current={kind,value,key:crypto.randomUUID()};return true;}
 resolved(){this.current=null;}
}
