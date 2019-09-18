importPackage( Packages.org.openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.java.io );
importPackage( Packages.org.entermediadb.modules.update );




var name = "app-site-manager";

var war = "http://dev.entermediasoftware.com/jenkins/job/@BRANCH@" + name + "/lastSuccessfulBuild/artifact/deploy/" + name + ".zip";

var root = moduleManager.getBean("root").getAbsolutePath();
var web = root + "/WEB-INF";
var tmp = web + "/tmp";

log.info("1. GET THE LATEST WAR FILE");



log.info("1. GET THE LATEST WAR FILE");
var downloader = new Downloader();
downloader.download( war, tmp + "/app-site-manager.zip");

log.info("2. UNZIP WAR FILE");
var unziper = new ZipUtil();
unziper.unzip(  tmp + "/app-site-manager.zip",  tmp );


//log.info("3. Copy Over Site " + tmp + "/unzip/" + " " + "to " + root);
var files = new FileUtils();
//files.deleteMatch( web + "/lib/@BRANCH@extension-saml*.jar");
//files.deleteMatch( web + "/lib/extension-saml*.jar");
//files.deleteMatch( web + "/lib/java-saml*.jar");
//files.deleteMatch( web + "/lib/joda-time*.jar");
//files.deleteMatch( web + "/lib/onelogin*.jar");
//files.deleteMatch( web + "/lib/stax*.jar");
//files.deleteMatch( web + "/lib/woodstock*.jar");
//files.deleteMatch( web + "/lib/xmlsec*.jar");
files.deleteMatch( web + "/lib/@BRANCH@app-site-manager*.jar");
files.deleteMatch( web + "/lib/stripe*.jar");

//
//
//
files.copyFileByMatch( tmp + "/lib/*.jar", web + "/lib/");


files.deleteMatch( web + "/WEB-INF/base/entermediadb/")
files.copyFileByMatch( tmp + "/base/entermediadb/", root + "/WEB-INF/base/entermediadb/");

files.deleteMatch( web + "/WEB-INF/base/eminstitute/")
files.copyFileByMatch( tmp + "/base/eminstitute/", root + "/WEB-INF/base/eminstitute/");


files.deleteMatch( web + "/WEB-INF/base/oi/")
files.deleteMatch( web + "/WEB-INF/base/oi-admin/")
files.copyFileByMatch( tmp + "/base/oi/", root + "/WEB-INF/base/oi/");
files.copyFileByMatch( tmp + "/base/oi-admin/", root + "/WEB-INF/base/oi-admin/");
files.copyFileByMatch(tmp + "/app-site-manager.zip", "/media/services/extensions/");


log.info("5. CLEAN UP");
files.deleteAll(tmp);

log.info("6. UPGRADE COMPLETED");
