/* WizAssetsPlugin - IOS side of the bridge to WizAssetsPlugin JavaScript for Cordova
 *
 * @author Ally Ogilvie
 * @copyright WizCorp Inc. [ Incorporated Wizards ] 2011
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

/* 
 *  WizAssetsPlugin methods
 */
- (void)downloadFile:(CDVInvokedUrlCommand*)command;
- (void)getFileURI:(CDVInvokedUrlCommand*)command;
- (void)getFileURIs:(CDVInvokedUrlCommand*)command;
- (void)deleteFile:(CDVInvokedUrlCommand*)command;
- (void)deleteFiles:(CDVInvokedUrlCommand*)command;
// - (void)purgeEmptyDirectories:(CDVInvokedUrlCommand*)command;

- (void)backgroundDownload:(CDVInvokedUrlCommand*)command;



@end
