/* WizAssetsPlugin - IOS side of the bridge to WizAssetsPlugin JavaScript for PhoneGap
 *
 * @author Ally Ogilvie
 * @copyright WizCorp Inc. [ Incorporated Wizards ] 2011
 * @file WizAssetsPlugin.m for PhoneGap
 *
 *
 */

#import "WizAssetsPlugin.h"
#import "WizDebugLog.h"

@implementation WizAssetsPlugin 



/*
 *
 * Methods
 *
 */

- (void)backgroundDownload:(NSArray*)arguments { 

    // Create a pool  
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];  
    
    // callback
    NSString *callbackId = [arguments objectAtIndex:0];
    
    // url
    NSString *urlString = [arguments objectAtIndex:1];
    // dir store path and name
    NSString *savePath = [arguments objectAtIndex:2];
    
    
    
    // split storePath
    NSMutableArray *pathSpliter = [[NSMutableArray alloc] initWithArray:[savePath componentsSeparatedByString:@"/"] copyItems:YES];
    NSString *fileName = [pathSpliter lastObject];
    // remove last object (filename)
    [pathSpliter removeLastObject];
    // join all dir(s)
    NSString *storePath = [pathSpliter componentsJoinedByString:@"/"];
    [pathSpliter release];
    
    
    // holds our return data
    NSString* returnString;

    
    if (urlString) {
        
        NSFileManager *filemgr;
        filemgr =[NSFileManager defaultManager];
        
        
        NSURL  *url = [NSURL URLWithString:urlString];
        
        WizLog(@"downloading ---------------- >%@", url);
        
        
        NSData *urlData = [NSData dataWithContentsOfURL:url];
        
        if ( urlData )
        {
            
            // path to library caches
            NSArray  *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
            
            NSString *documentsDirectory = [paths objectAtIndex:0]; 
            NSString *gameDir = [[[NSBundle mainBundle] infoDictionary]   objectForKey:@"CFBundleName"];
            NSString *fullDir = [NSString stringWithFormat:@"%@/%@/%@", documentsDirectory, gameDir, storePath];
            NSString *filePath = [fullDir stringByAppendingPathComponent:fileName];
            
            if ([filemgr createDirectoryAtPath:fullDir withIntermediateDirectories:YES attributes:nil error: NULL] == YES)
            {
                
                // Success to create directory download data to temp and move to library/cache when complete
                [urlData writeToFile:filePath atomically:YES];
                          
                returnString = filePath;
                               
            } else {
                
                // Fail to download
                
                returnString = @"error - failed download";

            }
            
        } else {

            WizLog(@"ERROR: URL no exist");
            returnString = @"error - bad url";
        }
        
    } else {
        returnString = @"error - no urlString";
    }

    NSArray* callbackData = [[NSArray alloc] initWithObjects:callbackId, returnString, nil];
    
    
    // download complete pass back confirmation to JS 
    [self performSelectorOnMainThread:@selector(completeDownload:) withObject:callbackData waitUntilDone:YES];
    
    [callbackData release];
    
    // clean up
    [pool release]; 



}

/*
 * downloadFile - download from an HTTP to app folder
 */
- (void)completeDownload:(NSArray*)callbackdata
{
    // faked the return string for now
    NSString* callbackId = [callbackdata objectAtIndex:0];
    NSString* returnString = [callbackdata objectAtIndex:1];

    WizLog(@"Path: %@", returnString);
    WizLog(@"callbackId ----------> : %@", callbackId);
    
    if ([returnString rangeOfString:@"error"].location == NSNotFound) {
        // no error
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:returnString];
        [self writeJavascript: [pluginResult toSuccessCallbackString:callbackId]];
        
    } else {
        // found error
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:returnString];
        [self writeJavascript: [pluginResult toErrorCallbackString:callbackId]];

    }
    
    
}



/*
 * downloadFile - download from an HTTP to app folder
 */
- (void)downloadFile:(NSArray*)arguments withDict:(NSDictionary*)options
{
    WizLog(@"[WizAssetsPlugin] ******* downloadFile-> " );
    NSString *callbackId = [arguments objectAtIndex:0];
    
    int count = [arguments count];
	if(count > 1) {
              
        // start in background, pass though strings
        [self performSelectorInBackground:@selector(backgroundDownload:) withObject:arguments];
        
    } else {
        
        CDVPluginResult* pluginResult;
        NSString *returnString;
        
        

        returnString = @"noParam";
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:returnString];
        [self writeJavascript: [pluginResult toErrorCallbackString:callbackId]];
        return;
    }
    

    
    
    
}



/*
 * purgeEmptyDirectories - purge that which is most unclean
 */
- (void)purgeEmptyDirectories:(NSArray*)arguments withDict:(NSDictionary*)options
{
    
    
    
    
}






- (void)scanDir:(NSString*)basePath relPath:(NSString*)relPath assetMap:(NSMutableDictionary*)assetMap
{
    // absPath is the exact path of where we currently are on the filesystem
    
    NSString * absPath;

    if ([relPath length] > 0) {
        absPath = [basePath stringByAppendingString:[NSString stringWithFormat:@"/%@", relPath]];
    } else {
        absPath = [basePath stringByAppendingString:relPath];
    }

    //WizLog(@"[WizAssetsPlugin] ******* scanning path %@", absPath );

    // for each file inside this dir

    for (NSString* fileName in [[NSFileManager defaultManager] contentsOfDirectoryAtPath:absPath error:nil]) {
        // create a new relative path for this file
        
        NSString * newRelPath;

        if ([relPath length] > 0) {
            newRelPath = [relPath stringByAppendingString:[NSString stringWithFormat:@"/%@", fileName]];
        } else {
            newRelPath = [relPath stringByAppendingString:fileName];
        }

        // create a new absolute path for this file, based on the basePath and the new relative path
        
        NSString * newAbsPath = [basePath stringByAppendingString:[NSString stringWithFormat:@"/%@", newRelPath]];

        if ( [[NSFileManager defaultManager] contentsOfDirectoryAtPath:newAbsPath error:NULL] ){
            // the found file is a directory, so we recursively scan it

            [self scanDir:basePath relPath:newRelPath assetMap:assetMap ];
        } else {
            // the found file is a real file, so we add it to the asset map
            // I JUST DELETED HERE file://localhost
            NSString * URIString = [NSString stringWithFormat:@"%@", newAbsPath];

            // WizLog(@"[WizAssetsPlugin] ******* newRelPath URI %@", newRelPath );
            // WizLog(@"[WizAssetsPlugin] ******* assetMap URI %@", URIString );

            [assetMap setObject: URIString forKey: newRelPath];
            

            
        }
    }
}



/*
 * getFileURI - return a URI to the requested resource
 */
- (void)getFileURI:(NSArray*)arguments withDict:(NSDictionary*)options
{
    CDVPluginResult* pluginResult;
    
    NSString *callbackId = [arguments objectAtIndex:0];
    NSString *findFile = [arguments objectAtIndex:1];
    NSString *returnURI = @"";
    
    
    NSMutableArray *fileStruct = [[NSMutableArray alloc] initWithArray:[findFile componentsSeparatedByString:@"/"]];
    // ie [0]img, [1]ui, [2]bob.mp3
    
    NSString * fileName = [fileStruct lastObject];
    // ie bob.mp3
    
    [fileStruct removeLastObject];
    NSString * findFilePath = [fileStruct componentsJoinedByString:@"/"];
    // ie img/ui
    
    // cut out suffix from file name
    NSMutableArray *fileTypeStruct = [[NSMutableArray alloc] initWithArray:[fileName componentsSeparatedByString:@"."]];
    // ie [0]bob, [1]mp3,
    
    
    
    if([[NSBundle mainBundle] pathForResource:[fileTypeStruct objectAtIndex:0] ofType:[fileTypeStruct objectAtIndex:1] inDirectory:[@"www" stringByAppendingFormat:@"/assets/%@", findFilePath]])
    {   // check local
        
        // path to bundle resources
        NSString *bundlePath = [[NSBundle mainBundle] resourcePath];
        NSString *bundleSearchPath = [NSString stringWithFormat:@"%@/%@/%@/%@", bundlePath , @"www", @"assets", findFile];
        
        // we have locally return same string
        returnURI = bundleSearchPath;
        
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:returnURI];
    } else {
        // check in app docs folder
        
        // path to app library/caches
        NSString * documentsPath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0];
        NSString * gamePath = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleName"];
        NSString * searchPath = [documentsPath stringByAppendingFormat:@"/%@", gamePath];
        
        NSMutableDictionary * resourceMap = [NSMutableDictionary dictionary];
        [self scanDir:searchPath relPath:@"" assetMap:resourceMap];
        
        // return URI to storage folder
        returnURI = [resourceMap objectForKey:findFile];
        
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:returnURI];
    }
    
    [fileStruct release];
    [fileTypeStruct release];
    [self writeJavascript: [pluginResult toSuccessCallbackString:callbackId]];
}



/*
 * getFileURIs - return all resources in app folder
 */
- (void)getFileURIs:(NSArray*)arguments withDict:(NSDictionary*)options
{
    WizLog(@"[WizAssetsPlugin] ******* getfileURIs-> " );
    // [self.appDelegate updateLoaderLabel:@"Checking for updates..."];
    
    
    NSString *callbackId = [arguments objectAtIndex:0];
    
    CDVPluginResult* pluginResult;

    // path to bundle resources
    NSString *bundlePath = [[NSBundle mainBundle] resourcePath];
    NSString *bundleSearchPath = [NSString stringWithFormat:@"%@/%@/%@", bundlePath , @"www", @"assets"];
    
    // scan bundle assets
    NSMutableDictionary * bundleAssetMap = [NSMutableDictionary dictionary];
    [self scanDir:bundleSearchPath relPath:@"" assetMap:bundleAssetMap];
    
    // WizLog(@"[WizAssetsPlugin] ******* bundleAssetMap-> %@  ", bundleAssetMap );

    
    // path to app library caches
    NSString * documentsPath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString * gamePath = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleName"];
    NSString * searchPath = [documentsPath stringByAppendingFormat:@"/%@", gamePath];
    
    // scan downloaded assets
    NSMutableDictionary * docAssetMap = [NSMutableDictionary dictionary];
    [self scanDir:searchPath relPath:@"" assetMap:docAssetMap];
    
    // WizLog(@"[WizAssetsPlugin] ******* docAssetMap-> %@  ", docAssetMap );
    
    
    
    NSMutableDictionary *assetMap = [docAssetMap mutableCopy];
    [assetMap addEntriesFromDictionary:bundleAssetMap];
    
    // WizLog(@"[WizAssetsPlugin] ******* final assetMap-> %@  ", assetMap );
    
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:assetMap];
    [self writeJavascript: [pluginResult toSuccessCallbackString:callbackId]];
    
    [assetMap release];
    
}



/*
 * deleteFile - delete all resources specified in string from app folder
 */
- (void)deleteFile:(NSArray*)arguments withDict:(NSDictionary*)options
{
    NSFileManager *filemgr;
    filemgr =[NSFileManager defaultManager];
    
    CDVPluginResult* pluginResult;
    
    NSString *callbackId = [arguments objectAtIndex:0];
    NSString *filePath = [arguments objectAtIndex:1];
    // example filePath -
    // file://localhost/Users/WizardBookPro/Library/Application%20Support/iPhone%20Simulator/4.3.2/Applications/AD92CAB6-C364-4536-A4F5-E8333CB9F054/Documents/ZombieBoss/img/ui/logo-v1-g.jpg
    
    
    // note: if no files sent here, still success (technically it is not an error as we success to delete nothing)

    if (filePath) {
        
        
        // check not file in bundle..
        
        NSString *bundlePath = [[NSBundle mainBundle] resourcePath];
        if ([filePath rangeOfString:bundlePath].location == NSNotFound) {
            
            if ([filemgr removeItemAtPath:filePath error:nil ]) {
                // success delete
                WizLog(@"[WizAssetsPlugin] ******* deletingFile > %@", filePath);
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                [self writeJavascript: [pluginResult toSuccessCallbackString:callbackId]];
                
            } else {
                // cannot delete
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"NotFoundError"];
                [self writeJavascript: [pluginResult toErrorCallbackString:callbackId]];
            }
        } else {
            // cannot delete file in the bundle
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                             messageAsString:@"NoModificationAllowedError"];
            [self writeJavascript: [pluginResult toErrorCallbackString:callbackId]];
        }
        
        
    } else {
        // successfully deleted nothing
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self writeJavascript: [pluginResult toSuccessCallbackString:callbackId]];
    }
}




/*
 * deleteFiles - delete all resources specified in array from app folder
 */
- (void)deleteFiles:(NSArray*)arguments withDict:(NSDictionary*)options
{
    NSFileManager *filemgr;
    filemgr =[NSFileManager defaultManager];
    
    CDVPluginResult* pluginResult;
    
    NSString *callbackId = [arguments objectAtIndex:0];
    

    NSMutableArray *fileArray = [[NSMutableArray alloc] initWithArray:arguments copyItems:YES];
    // example filePath[] -
    // [file://localhost/Users/WizardBookPro/Library/Application%20Support/iPhone%20Simulator/4.3.2/Applications/AD92CAB6-C364-4536-A4F5-E8333CB9F054/Documents/ZombieBoss/img/ui/logo-v1-g.jpg, file://localhost/Users/WizardBookPro/Library/Application%20Support/iPhone%20Simulator/4.3.2/Applications/AD92CAB6-C364-4536-A4F5-E8333CB9F054/Documents/ZombieBoss/img/ui/logo2-v1-g.jpg ]
    
    if (fileArray) {

        // count array
        for (int i=0; i< [fileArray count]; i++){
            
            /* 
             was using file:// locahost 
             
            // split each URI in array to remove PhoneGap prefix (file://localhost) then delete
            NSString *singleFile = [fileArray objectAtIndex:i];
                        
            NSMutableArray *pathSpliter = [[NSMutableArray alloc] initWithArray:[singleFile componentsSeparatedByString:@"localhost"] copyItems:YES];
            NSString *iphonePath = [pathSpliter lastObject];
            WizLog(@"[WizAssetsPlugin] ******* deletingFile > %@", iphonePath);
            
            [filemgr removeItemAtPath:iphonePath error:NULL];
            [pathSpliter release];
             
             */
            
            // split each URI in array to remove PhoneGap prefix (file://localhost) then delete
            NSString *singleFile = [fileArray objectAtIndex:i];
            
            WizLog(@"[WizAssetsPlugin] ******* deletingFile > %@", singleFile);
            
            [filemgr removeItemAtPath:singleFile error:NULL];
            
        }
        
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"noParam"];
    }
    
    [fileArray release];
    [self writeJavascript: [pluginResult toSuccessCallbackString:callbackId]];
    
}


@end