/* WizAssetsPlugin - IOS side of the bridge to WizAssetsPlugin JavaScript for PhoneGap
 *
 * @author Ally Ogilvie
 * @copyright Wizcorp Inc. [ Incorporated Wizards ] 2014
 * @file WizAssetsPlugin.m for PhoneGap
 *
 */
#import "WizAssetsPlugin.h"
#import "WizDebugLog.h"

NSString *const assetsErrorKey = @"plugins.wizassets.errors";

@implementation WizAssetsPlugin

@synthesize cachePath = _cachePath;

- (void)pluginInitialize {
    [super pluginInitialize];

    NSArray  *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    NSString *documentsDirectory = [paths objectAtIndex:0];
    NSString *gameDir = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleName"];

    self.cachePath = [documentsDirectory stringByAppendingPathComponent:gameDir];
}

- (void)dealloc {
    [_cachePath release];

    [super dealloc];
}

- (void)backgroundDownload:(NSDictionary *)args {
    // Create a pool
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    CDVInvokedUrlCommand *command = [args objectForKey:@"command"];
    NSString *filePath = [args objectForKey:@"filePath"];
    NSString *fullDir = [filePath stringByDeletingLastPathComponent];

    // url
    NSString *urlString = [command.arguments objectAtIndex:0];
    // holds our return data
    NSString *returnString;

    if (urlString) {

        NSFileManager *filemgr;
        filemgr =[NSFileManager defaultManager];
        NSURL  *url = [NSURL URLWithString:urlString];

        WizLog(@"downloading ---------------- >%@", url);

        NSError *error = nil;
        NSData *urlData = [NSData dataWithContentsOfURL:url options:NSDataReadingUncached error:&error];

        if (error) {
            returnString = [NSString stringWithFormat:@"error - %@", error];
        } else if (urlData) {
            // Check if we didn't received a 401
            // TODO: We might want to find another solution to check for this kind of error, and check other possible errors
            NSString *dataContent = [[NSString alloc] initWithBytes:[urlData bytes] length:12 encoding:NSUTF8StringEncoding];
            bool urlUnauthorized = [dataContent isEqualToString:@"Unauthorized"];
            [dataContent release];

            NSError *directoryError = nil;
            if (urlUnauthorized) {
                returnString = @"error - url unauthorized";
            } else {
                bool isDirectoryCreated = [filemgr createDirectoryAtPath:fullDir withIntermediateDirectories:YES attributes:nil error: &directoryError];

                if (!isDirectoryCreated) {
                    if ([[directoryError domain] isEqualToString:NSCocoaErrorDomain] && [directoryError code] == NSFileWriteFileExistsError) {
                        // Directory already exists, it's not an error
                        isDirectoryCreated = true;
                    } else {
                        returnString = [NSString stringWithFormat:@"error - unable to create directory (code: %ld - domain: %@)", [directoryError code], [directoryError domain]];
                    }
                }
                if (isDirectoryCreated) {
                    // Success to create directory download data to temp and move to library/cache when complete
                    [urlData writeToFile:filePath atomically:YES];
                    returnString = filePath;
                }
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
    [self sendCallback:callbackId returnString:returnString];
}


- (void)sendCallback:(NSString *)callbackId returnString:(NSString *)returnString {
    WizLog(@"Path: %@", returnString);
    WizLog(@"callbackId ----------> : %@", callbackId);

    if ([returnString rangeOfString:@"error"].location == NSNotFound) {
        // no error
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:returnString];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    } else {
        // found error
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:returnString];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    }
}

/*
 * downloadFile - download from an HTTP to app folder
 */
- (void)downloadFile:(CDVInvokedUrlCommand *)command {
    WizLog(@"[WizAssetsPlugin] ******* downloadFile-> " );

    NSUInteger count = [command.arguments count];
    if (count == 0) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"noParam"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }

    // dir store path and name
    NSString *uri = [command.arguments objectAtIndex:1];
    NSString *filePath = [self buildAssetFilePathFromUri:uri];

    NSFileManager *fileManager = [NSFileManager defaultManager];
    if ([fileManager fileExistsAtPath:filePath] == YES) {
        // download complete pass back confirmation to JS
        [self sendCallback:command.callbackId returnString:filePath];
        return;
    }

    NSDictionary *args = [NSDictionary dictionaryWithObjectsAndKeys:
                          command, @"command",
                          filePath, @"filePath",
                          nil];
    [self performSelectorInBackground:@selector(backgroundDownload:) withObject:args];
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
        NSMutableDictionary *resourceMap = [NSMutableDictionary dictionary];

        [self scanDir:self.cachePath relPath:@"" assetMap:resourceMap];

        // Return URI to storage folder
        returnURI = [resourceMap objectForKey:findFile];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:returnURI];
    }

    [fileStruct release];
    [fileTypeStruct release];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
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

    // Scan downloaded assets
    NSMutableDictionary *docAssetMap = [NSMutableDictionary dictionary];
    [self scanDir:self.cachePath relPath:@"" assetMap:docAssetMap];

    NSMutableDictionary *assetMap = [docAssetMap mutableCopy];
    [assetMap addEntriesFromDictionary:bundleAssetMap];

    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:assetMap];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

    [assetMap release];
}

/*
 * deleteFile - delete all resources specified in string from app folder
 */
- (void)deleteFile:(CDVInvokedUrlCommand *)command {
    CDVPluginResult *pluginResult;
    NSString *uri = [command.arguments objectAtIndex:0];

    if ([self deleteAsset:uri error:nil]) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Deleting file failed."];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/*
 * deleteFiles - delete all resources specified in array from app folder
 */
- (void)deleteFiles:(CDVInvokedUrlCommand *)command {
    [self performSelectorInBackground:@selector(backgroundDelete:) withObject:command];
}

- (void)backgroundDelete:(CDVInvokedUrlCommand *)command {
    // Create a pool
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    NSMutableArray *uris = [[NSMutableArray alloc] initWithArray:command.arguments copyItems:YES];

    NSError *error = nil;
    for (int i=0; i< [uris count]; i++) {
        NSString *uri = [uris objectAtIndex:i];
        [self deleteAsset:uri error:&error];
    }
    [uris release];

    NSArray *callbackData = [[NSArray alloc] initWithObjects:command.callbackId, error, nil];

    // download complete pass back confirmation to JS
    [self performSelectorOnMainThread:@selector(completeDelete:) withObject:callbackData waitUntilDone:YES];

    [callbackData release];

    // clean up
    [pool release];
}

/*
 * completeDelete - callback after delete
 */
- (void)completeDelete:(NSArray *)callbackdata {
    CDVPluginResult *pluginResult;
    NSString *callbackId = [callbackdata objectAtIndex:0];
    NSError *error = nil;
    if ([callbackdata count] > 1) {
        error = [callbackdata objectAtIndex:1];
    }

    if (error) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Deleting files failed."];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

/*
 * deleteAsset - delete resource specified in string from app folder
 */
- (BOOL)deleteAsset:(NSString *)uri error:(NSError **)error {
    NSFileManager *fileManager = [NSFileManager defaultManager];

    if (!uri || [uri length] == 0) {
        if (error != nil) {
            *error = [NSError errorWithDomain:assetsErrorKey code:200 userInfo:nil];
        }
        return NO;
    }

    NSError *localError = nil;
    NSString *filePath = [self buildAssetFilePathFromUri:uri];
    if (![fileManager removeItemAtPath:filePath error:&localError]) {
        // File didn't exist in the first place, it's not an error
        if ([[localError domain] isEqualToString:NSCocoaErrorDomain] && [localError code] == NSFileNoSuchFileError) {
            return YES;
        }
        // File deletion failed, it's an error
        if (error != nil) {
            *error = localError;
        }
        return NO;
    }
    return YES;
}

- (NSString *)buildAssetFilePathFromUri:(NSString *)uri {
    return [self.cachePath stringByAppendingPathComponent:uri];
}

@end