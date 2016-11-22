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
@synthesize session = _session;

- (void)pluginInitialize {
    [super pluginInitialize];

    NSArray  *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    NSString *documentsDirectory = [paths objectAtIndex:0];
    NSString *gameDir = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleName"];

    self.cachePath = [documentsDirectory stringByAppendingPathComponent:gameDir];

    NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
    configuration.requestCachePolicy = NSURLRequestReloadIgnoringLocalCacheData;
    configuration.URLCache = nil;
    self.session = [NSURLSession sessionWithConfiguration:configuration];
}

/*
 * initialize - not doing anything (yet) on iOS, just returning true
 */
- (void)initialize:(CDVInvokedUrlCommand *)command {
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

/*
 * downloadFile - download from an HTTP to app folder
 */
- (void)downloadFile:(CDVInvokedUrlCommand *)command {
    WizLog(@"[WizAssetsPlugin] ******* downloadFile-> " );

    NSUInteger count = [command.arguments count];
    if (count < 2) {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self createDownloadFileError:ARGS_MISSING_ERROR]];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }

    // dir store path and name
    NSString *uri = [command.arguments objectAtIndex:1];
    NSString *filePath = [self buildAssetFilePathFromUri:uri];

    if ([[NSFileManager defaultManager] fileExistsAtPath:filePath] == YES) {
        // download complete pass back confirmation to JS
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:filePath];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }

    NSString *urlString = [command.arguments objectAtIndex:0];
    NSURL *URL = [NSURL URLWithString:urlString];
    NSURLRequest *request = [NSURLRequest requestWithURL:URL];

    NSURLSessionDownloadTask *downloadTask = [self.session downloadTaskWithRequest:request
                                                                 completionHandler:
                                              ^(NSURL *location, NSURLResponse *response, NSError *error) {
                                                  CDVPluginResult *result = nil;
                                                  NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *)response;
                                                  NSInteger statusCode = httpResponse.statusCode;
                                                  if (statusCode >= 200 && statusCode < 300) {
                                                      result = [self moveFileAtURL:location toFilePath:filePath];
                                                  } else if (error) {
                                                      result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self createDownloadFileError:HTTP_REQUEST_ERROR status:statusCode message:[error description]]];
                                                  } else {
                                                      result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self createDownloadFileError:HTTP_REQUEST_ERROR status:statusCode]];
                                                  }
                                                  dispatch_async(dispatch_get_main_queue(), ^{
                                                      [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
                                                  });
                                              }];

    [downloadTask resume];
}

- (CDVPluginResult *)moveFileAtURL:(NSURL *)location toFilePath:(NSString *)filePath {
    CDVPluginResult *result = nil;
    NSError *error = nil;
    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSString *fullDir = [filePath stringByDeletingLastPathComponent];
    BOOL isDirectoryCreated = [fileManager createDirectoryAtPath:fullDir withIntermediateDirectories:YES attributes:nil error:&error];
    if (error) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self createDownloadFileError:DIRECTORY_CREATION_ERROR message:[error description]]];
    } else if (!isDirectoryCreated) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self createDownloadFileError:DIRECTORY_CREATION_ERROR]];
    } else {
        NSURL *filePathURL = [NSURL fileURLWithPath:filePath];
        BOOL isFileMoved = [fileManager moveItemAtURL:location toURL:filePathURL error:&error];
        if (error) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self createDownloadFileError:FILE_CREATION_ERROR message:[error description]]];
        } else if (!isFileMoved) {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self createDownloadFileError:FILE_CREATION_ERROR]];
        } else {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:filePath];
        }
    }
    return result;
}

- (NSDictionary *)createDownloadFileError:(int)code {
    return [self createDownloadFileError:code status:-1 message:nil];
}

- (NSDictionary *)createDownloadFileError:(int)code status:(NSInteger)status {
    return [self createDownloadFileError:code status:status message:nil];
}

- (NSDictionary *)createDownloadFileError:(int)code message:(NSString *)message {
    return [self createDownloadFileError:code status:-1 message:message];
}

- (NSDictionary *)createDownloadFileError:(int)code status:(NSInteger)status message:(NSString *)message {
    NSMutableDictionary* error = [NSMutableDictionary dictionaryWithObject:[NSNumber numberWithInt:code] forKey:@"code"];
    if (status != -1) {
        [error setObject:[NSNumber numberWithInteger:status] forKey:@"status"];
    }
    if (message) {
        [error setObject:message forKey:@"message"];
    } else {
        [error setObject:@"No description" forKey:@"message"];
    }
    return error;
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
    @autoreleasepool {
        NSArray *uris = command.arguments;

        NSError *error = nil;
        for (int i = 0; i < [uris count]; i++) {
            NSString *uri = [uris objectAtIndex:i];
            [self deleteAsset:uri error:&error];
        }

        NSArray *callbackData = [[NSArray alloc] initWithObjects:command.callbackId, error, nil];

        // download complete pass back confirmation to JS
        [self performSelectorOnMainThread:@selector(completeDelete:) withObject:callbackData waitUntilDone:YES];
    }
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
