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

@interface WizAssetsPlugin : CDVPlugin <UIWebViewDelegate> {
    int scanCounter;
    NSMutableArray *storePaths;
}

// Exposed to JavaScript
- (void)downloadFile:(CDVInvokedUrlCommand *)command;
- (void)getFileURI:(CDVInvokedUrlCommand *)command;
- (void)getFileURIs:(CDVInvokedUrlCommand *)command;
- (void)deleteFile:(CDVInvokedUrlCommand *)command;
- (void)deleteFiles:(CDVInvokedUrlCommand *)command;

@end