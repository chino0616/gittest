import groovy.json.JsonSlurper

import org.apache.http.HttpEntity
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import groovy.sql.Sql



PoolingClientConnectionManager cm = new PoolingClientConnectionManager();

cm.setDefaultMaxPerRoute(1);
cm.setMaxTotal(1);
HttpClient httpClient = new DefaultHttpClient(cm);
String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36";
httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, userAgent);
accessToken=getAccess(httpClient)
String baseurl = 'https://graph.facebook.com/'
String TargetName = 'ppssw'
TargetName= 'rubberduck.zh'
String Argument ='?fields=posts.fields(id,comments.fields(comments.fields(id,message,created_time,from),id,message,created_time,from),message,created_time,from)'

url=baseurl+TargetName+Argument+'&access_token='+accessToken


def content = getConn(url,httpClient)
def slurper = new JsonSlurper()
result = slurper.parseText(content)
println result.id    //post id
def al = new ArrayList()
def set = new HashSet()
//parse this page
//ParseUser(result)

def nextPage = result.posts.paging.next
//parse first nextPage
parseNextPost(nextPage,httpClient)
println al.size()

println ''


//Connection
def String getConn(url,httpClient){
    BufferedReader reader = null;
    StringBuilder builder
    url = url.replace('|',URLEncoder.encode('|', 'UTF-8'))
    for(int i=0;i<15;i++)
    try {
        HttpResponse respons = httpClient.execute(new HttpGet(url));
        InputStream is = respons.getEntity().getContent();
        reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
        builder = new StringBuilder(4096);
        //println respons.getStatusLine()
        // Read the content and create a single string of it
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString()
    }catch(Exception e){
    
        println 'getConnectError:'+e
    }finally{
        if(reader!=null)
        reader.close();
    }
    return builder.toString()
}
//get Access_Token by AppID and AppSecret
def String getAccess(httpClient){
    AppID = '173074406062666'
    AppSecret = 'c521df92a1dddb00ad190103c566057d'
    url = 'https://graph.facebook.com/oauth/access_token?client_id=' + AppID + '&client_secret=' + AppSecret + '&grant_type=client_credentials'
    return getConn(url,httpClient).split('=')[1]
}

def ParseUser(result){
    ParseUser(result,null)
}
//def sql = Sql.newInstance( 'jdbc:postgresql://192.168.1.11/webContent', 'trinity', 'trinity', 'org.postgresql.Driver' )
//finally{
//        if(sql!=null)sql.close()
def ParseUser(result,sql){
    def posts = result.posts.data
    def comments
    def replys
    posts.each{
            pkey ->
            //insertPost(pkey,sql)
    /*
           println pkey.id            //post id
           println pkey.created_time  //post created_time
           println pkey.message       //post message
           println pkey.from.id        
    */
        
        comments = pkey.comments.data
        comments.each{
            ckey ->
            //insertComment(ckey,sql)
/*
            println '\t'+ckey.id            //comment id
            println '\t'+ckey.created_time  //comment created_time
            println '\t'+ckey.message       //comment message 
*/            
            replys = pkey.comments.data
            replys.each{
                rkey ->
                //insertReply(rkey,sql)
/*
                println '\t\t'+rkey.id            //reply id
                println '\t\t'+rkey.created_time  //reply created_time
                println '\t\t'+rkey.message       //reply message 
*/                
       
            } 
            
        }
    
    }
}
def ParsePost(result){
    posts = result.data
    //println posts.size()
    posts.each{
        pkey ->
                println pkey.comments.paging.next

                println pkey.comments.data.size()
    }
}
def parseNextPost(nextPage,httpClient){
    boolean hasData = true
    while(hasData){
        content =  getConn(nextPage,httpClient)
        result = new JsonSlurper().parseText(content)
        //parse each nextPage
        if(result.data.size()>0){
            hasData = true
            ParsePost(result)
            nextPage = result.paging.next
        }else
            hasData = false
    }
}

//SQL etc
def insertPost(Map input,sql){
    def id = input.id
    def created_time = input.created_time
    def message = input.message.replace("'","''")
    def from_id = input.from.id
    hasDuplicate = false
    sql.eachRow( "select * from fb_post where id='"+id+"' limit 1" ){
        hasDuplicate = true
    }
    if(!hasDuplicate)
        sql.execute("insert into fb_post (id, message, from_id, created_time) values (\'"+id+"\',\'"+message+"\',\'"+from_id+"\',\'"+created_time+"\')")
}
def insertComment(Map input,sql){
//insert into fb_post (id, message, uid, created_time) values ('515037491904826','充氣的汽球拼出來的彩虹跟小鴨色彩很繽紛可愛唷！','515037491904826_581948225213752','2014-01-09T02:00:00+0000')
    def id = input.id
    def created_time = input.created_time
    def message = input.message.replace("'","''")
    def from_id = input.from.id
    hasDuplicate = false
    sql.eachRow( "select * from fb_comment where id='"+id+"' limit 1" ){
        hasDuplicate = true
    }
    if(!hasDuplicate)    
    sql.execute("insert into fb_comment (id, message, from_id, created_time) values (\'"+id+"\',\'"+message+"\',\'"+from_id+"\',\'"+created_time+"\')")      
}
def insertReply(Map input,sql){
    def id = input.id
    def created_time = input.created_time
    def message = input.message.replace("'","''")
    def from_id = input.from.id
    hasDuplicate = false
    sql.eachRow( "select * from fb_reply where id='"+id+"' limit 1" ){
        hasDuplicate = true
    }
    if(!hasDuplicate)    
    sql.execute("insert into fb_reply (id, message, from_id, created_time) values (\'"+id+"\',\'"+message+"\',\'"+from_id+"\',\'"+created_time+"\')")      
}



