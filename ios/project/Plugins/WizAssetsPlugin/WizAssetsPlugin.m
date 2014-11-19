/* WizAssetsPlugin - IOS side of the bridge to WizAssetsPlugin JavaScript for PhoneGap
 *
 * @author Ally Ogilvie
 * @copyright Wizcorp Inc. [ Incorporated Wizards ] 2014
 * @file WizAssetsPlugin.m for PhoneGap
 *
 */
#import "WizAssetsPlugin.h"
#import "WizDebugLog.h"

@implementation WizAssetsPlugin 

- (void)backgroundDownload:(CDVInvokedUrlCommand *)command { 

    // Create a pool  
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];  
    // url
    NSString *urlString = [command.arguments objectAtIndex:0];
    // dir store path and name
    NSString *savePath = [command.arguments objectAtIndex:1];
    // split storePath
    NSMutableArray *pathSpliter = [[NSMutableArray alloc] initWithArray:[savePath componentsSeparatedByString:@"/"] copyItems:YES];
    NSString *fileName = [pathSpliter lastObject];
    // remove last object (filename)
    [pathSpliter removeLastObject];
    // join all dir(s)
    NSString *storePath = [pathSpliter componentsJoinedByString:@"/"];
    [pathSpliter release];
    // holds our return data
    NSString *returnString;

    if (urlString) {

        NSFileManager *filemgr;
        filemgr =[NSFileManager defaultManager];
        NSURL  *url = [NSURL URLWithString:urlString];

        WizLog(@"downloading ---------------- >%@", url);

        NSData *urlData = [NSData dataWithContentsOfURL:url];

        if (urlData) {
            // path to library caches
            NSArray  *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);          
            NSString *documentsDirectory = [paths objectAtIndex:0]; 
            NSString *gameDir = [[[NSBundle mainBundle] infoDictionary]   objectForKey:@"CFBundleName"];
            NSString *fullDir = [NSString stringWithFormat:@"%@/%@/%@", documentsDirectory, gameDir, storePath];
            NSString *filePath = [fullDir stringByAppendingPathComponent:fileName];

            if ([filemgr createDirectoryAtPath:fullDir withIntermediateDirectories:YES attributes:nil error: NULL] == YES) {

                // Success to create directory download data to temp and move to library/cache when complete
                [urlData writeToFile:filePath atomically:YES];
                returnString = filePath;
            } else {
                // Failed to download
                returnString = @"error - failed download";
            }
        } else {
            WizLog(@"ERROR: URL no exist");
            returnString = @"error - bad url";
        }        
    } else {
        returnString = @"error - no urlString";
    }

    NSArray *callbackData = [[NSArray alloc] initWithObjects:command.callbackId, returnString, nil];
    // Download complete pass back confirmation to JS 
    [self performSelectorOnMainThread:@selector(completeDownload:) withObject:callbackData waitUntilDone:YES];
    [callbackData release];

    // clean up
    [pool release]; 
}

/*
 * completeDownload - background thread callback to JavaScript
 */
- (void)completeDownload:(NSArray *)callbackdata {
    // Faked the return string for now
    NSString *callbackId = [callbackdata objectAtIndex:0];
    NSString *returnString = [callbackdata objectAtIndex:1];

    WizLog(@"Path: %@", returnString);
    WizLog(@"callbackId ----------> : %@", callbackId);

    if ([returnString rangeOfString:@"error"].location == NSNotFound) {
        // no error
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:returnString];
        [self writeJavascript: [pluginResult toSuccessCallbackString:callbackId]];
    } else {
        // found error
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:returnString];
        [self writeJavascript: [pluginResult toErrorCallbackString:callbackId]];
    }
}

/*
 * downloadFile - download from an HTTP to app folder
 */
- (void)downloadFile:(CDVInvokedUrlCommand *)command {
    WizLog(@"[WizAssetsPlugin] ******* downloadFile-> " );
    int count = [command.arguments count];
    if (count > 0) {
        // start in background, pass though strings
        [self performSelectorInBackground:@selector(backgroundDownload:) withObject:command];
    } else {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"noParam"];
        [self writeJavascript:[pluginResult toErrorCallbackString:command.callbackId]];
        return;
    }   
}

- (void)scanDir:(NSString *)basePath relPath:(NSString *)relPath assetMap:(NSMutableDictionary *)assetMap {
    // absPath is the exact path of where we currently are on the filesystem
    NSString *absPath;

    if ([relPath length] > 0) {
        absPath = [basePath stringByAppendingString:[NSString stringWithFormat:@"/%@", relPath]];
    } else {
        absPath = [basePath stringByAppendingString:relPath];
    }

    // For each file inside this dir
    for (NSString *fileName in [[NSFileManager defaultManager] contentsOfDirectoryAtPath:absPath error:nil]) {
        // Create a new relative path for this file
        NSString *newRelPath;

        if ([relPath length] > 0) {
            newRelPath = [relPath stringByAppendingString:[NSString stringWithFormat:@"/%@", fileName]];
        } else {
            newRelPath = [relPath stringByAppendingString:fileName];
        }

        // Create a new absolute path for this file, based on the basePath and the new relative path
        NSString *newAbsPath = [basePath stringByAppendingString:[NSString stringWithFormat:@"/%@", newRelPath]];

        if ( [[NSFileManager defaultManager] contentsOfDirectoryAtPath:newAbsPath error:NULL] ){
            // The found file is a directory, so we recursively scan it
            [self scanDir:basePath relPath:newRelPath assetMap:assetMap ];
        } else {
            // The found file is a real file, so we add it to the asset map
            // I JUST DELETED HERE file://localhost
            NSString *URIString = [NSString stringWithFormat:@"%@", newAbsPath];
            [assetMap setObject: URIString forKey: newRelPath];
        }
    }
}

/*
 * getFileURI - return a URI to the requested resource
 */
- (void)getFileURI:(CDVInvokedUrlCommand *)command {
    CDVPluginResult *pluginResult;
    NSString *findFile = [command.arguments objectAtIndex:0];
    NSString *returnURI = @"";

    // Example: [0]img, [1]ui, [2]bob.mp3
    NSMutableArray *fileStruct = [[NSMutableArray alloc] initWithArray:[findFile componentsSeparatedByString:@"/"]];

    // Example: bob.mp3
    NSString *fileName = [fileStruct lastObject];
    
    [fileStruct removeLastObject];

    // Example img/ui
    NSString *findFilePath = [fileStruct componentsJoinedByString:@"/"];

    // Cut out suffix from file name, example: [0]bob, [1]mp3,
    NSMutableArray *fileTypeStruct = [[NSMutableArray alloc] initWithArray:[fileName componentsSeparatedByString:@"."]];

    if ([[NSBundle mainBundle] pathForResource:[fileTypeStruct objectAtIndex:0] 
                                        ofType:[fileTypeStruct objectAtIndex:1] 
                                   inDirectory:[@"www" stringByAppendingFormat:@"/assets/%@", findFilePath]]) {
        // Check local path to bundle resources
        NSString *bundlePath = [[NSBundle mainBundle] resourcePath];
        NSString *bundleSearchPath = [NSString stringWithFormat:@"%@/%@/%@/%@", bundlePath , @"www", @"assets", findFile];

        // We have locally return same string
        returnURI = bundleSearchPath;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:returnURI];
    } else {
        // Check in path to app library/caches
        NSString *documentsPath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0];
        NSString *gamePath = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleName"];
        NSString *searchPath = [documentsPath stringByAppendingFormat:@"/%@", gamePath];
        NSMutableDictionary *resourceMap = [NSMutableDictionary dictionary];

        [self scanDir:searchPath relPath:@"" assetMap:resourceMap];

        // Return URI to storage folder
        returnURI = [resourceMap objectForKey:findFile];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:returnURI];
    }
    
    [fileStruct release];
    [fileTypeStruct release];
    [self writeJavascript: [pluginResult toSuccessCallbackString:command.callbackId]];
}

/*
 * getFileURIs - return all resources in app folder
 */
- (void)getFileURIs:(CDVInvokedUrlCommand *)command {
    WizLog(@"[WizAssetsPlugin] ******* getfileURIs-> " );

    CDVPluginResult *pluginResult;

    // Path to bundle resources
    NSString *bundlePath = [[NSBundle mainBundle] resourcePath];
    NSString *bundleSearchPath = [NSString stringWithFormat:@"%@/%@/%@", bundlePath , @"www", @"assets"];

    // Scan bundle assets
    NSMutableDictionary *bundleAssetMap = [NSMutableDictionary dictionary];
    [self scanDir:bundleSearchPath relPath:@"" assetMap:bundleAssetMap];

    // Path to app library caches
    NSString *documentsPath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString *gamePath = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleName"];
    NSString *searchPath = [documentsPath stringByAppendingFormat:@"/%@", gamePath];

    // Scan downloaded assets
    NSMutableDictionary *docAssetMap = [NSMutableDictionary dictionary];
    [self scanDir:searchPath relPath:@"" assetMap:docAssetMap];

    NSMutableDictionary *assetMap = [docAssetMap mutableCopy];
    [assetMap addEntriesFromDictionary:bundleAssetMap];

    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:assetMap];
    [self writeJavascript: [pluginResult toSuccessCallbackString:command.callbackId]];
    
    [assetMap release];
}

/*
 * deleteFile - delete all resources specified in string from app folder
 */
- (void)deleteFile:(CDVInvokedUrlCommand *)command {
    NSFileManager *filemgr = [NSFileManager defaultManager];
    CDVPluginResult *pluginResult;
    NSString *filePath = [command.arguments objectAtIndex:0];

    // Note: if no files sent here, still success (technically it is not an error as we success to delete nothing)
    if (filePath) {        
        // Check if file is in bundle..
        NSString *bundlePath = [[NSBundle mainBundle] resourcePath];
        if ([filePath rangeOfString:bundlePath].location == NSNotFound) {
            if ([filemgr removeItemAtPath:filePath error:nil ]) {
                // Success delete
                WizLog(@"[WizAssetsPlugin] ******* deletingFile > %@", filePath);
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                [self writeJavascript: [pluginResult toSuccessCallbackString:command.callbackId]];
            } else {
                // Cannot delete
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"NotFoundError"];
                [self writeJavascript: [pluginResult toErrorCallbackString:command.callbackId]];
            }
        } else {
            // Cannot delete file in the bundle
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                             messageAsString:@"NoModificationAllowedError"];
            [self writeJavascript: [pluginResult toErrorCallbackString:command.callbackId]];
        }
    } else {
        // Successfully deleted nothing
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self writeJavascript: [pluginResult toSuccessCallbackString:command.callbackId]];
    }
}

/*
 * deleteFiles - delete all resources specified in array from app folder
 */
- (void)deleteFiles:(CDVInvokedUrlCommand *)command {
    NSFileManager *filemgr = [NSFileManager defaultManager];
    CDVPluginResult *pluginResult;
    NSMutableArray *fileArray = [[NSMutableArray alloc] initWithArray:command.arguments copyItems:YES];

    if (fileArray) {
        // Count array
        for (int i = 0; i < [fileArray count]; i++) {
            // Split each URI in array to remove PhoneGap prefix (file://localhost) then delete
            NSString *singleFile = [fileArray objectAtIndex:i];
            WizLog(@"[WizAssetsPlugin] ******* deletingFile > %@", singleFile);
            [filemgr removeItemAtPath:singleFile error:NULL];
        }
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"noParam"];
    }

    [fileArray release];
    [self writeJavascript: [pluginResult toSuccessCallbackString:command.callbackId]];

}

@end