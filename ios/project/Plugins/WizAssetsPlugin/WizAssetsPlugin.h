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
#ifdef CORDOVA_FRAMEWORK
#import <Cordova/CDVPlugin.h>
#else
#import "CDVPlugin.h"
#endif


@interface WizAssetsPlugin : CDVPlugin <UIWebViewDelegate> {
    
    int scanCounter;
    NSMutableArray *storePaths;
}

/* 
 *  WizAssetsPlugin methods
 */
- (void)downloadFile:(NSArray*)arguments withDict:(NSDictionary*)options;
- (void)getFileURI:(NSArray*)arguments withDict:(NSDictionary*)options;
- (void)getFileURIs:(NSArray*)arguments withDict:(NSDictionary*)options;
- (void)deleteFile:(NSArray*)arguments withDict:(NSDictionary*)options;
- (void)deleteFiles:(NSArray*)arguments withDict:(NSDictionary*)options;
// - (void)purgeEmptyDirectories:(NSArray*)arguments withDict:(NSDictionary*)options();

- (void)backgroundDownload:(NSArray*)arguments;



@end
