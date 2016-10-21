/* WizAssetsPlugin - IOS side of the bridge to WizAssetsPlugin JavaScript for Cordova
 *
 * @author Ally Ogilvie
 * @copyright Wizcorp Inc. [ Incorporated Wizards ] 2014
 * @file WizAssetsPlugin.h for PhoneGap
 *
 *
 */

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <Cordova/CDVPlugin.h>

enum CDVWizAssetsError {
    NO_ERROR = 0,
    ARGS_MISSING_ERROR = 1,
    INVALID_URL_ERROR = 2,
    CONNECTIVITY_ERROR = 3,
    HTTP_REQUEST_ERROR = 4,
    HTTP_REQUEST_CONTENT_ERROR = 5,
    DIRECTORY_CREATION_ERROR = 6,
    FILE_CREATION_ERROR = 7,
    JSON_CREATION_ERROR = 8,
    INITIALIZATION_ERROR = 9,
    UNREFERENCED_ERROR = 10
};
typedef int CDVWizAssetsError;

@interface WizAssetsPlugin : CDVPlugin <UIWebViewDelegate> {
    NSString *_cachePath;
    NSURLSession *_session;
}

- (void)pluginInitialize;

// Exposed to JavaScript
- (void)isReady:(CDVInvokedUrlCommand *)command;
- (void)downloadFile:(CDVInvokedUrlCommand *)command;
- (void)getFileURI:(CDVInvokedUrlCommand *)command;
- (void)getFileURIs:(CDVInvokedUrlCommand *)command;
- (void)deleteFile:(CDVInvokedUrlCommand *)command;
- (void)deleteFiles:(CDVInvokedUrlCommand *)command;

@property (nonatomic, retain) NSString *cachePath;
@property (nonatomic, retain) NSURLSession *session;

@end
