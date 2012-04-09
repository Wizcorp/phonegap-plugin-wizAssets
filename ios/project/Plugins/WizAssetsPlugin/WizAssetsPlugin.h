/* WizAssetsPlugin - IOS side of the bridge to WizAssetsPlugin JavaScript for PhoneGap
 *
 * @author Ally Ogilvie
 * @copyright WizCorp Inc. [ Incorporated Wizards ] 2011
 * @file WizAssetsPlugin.h for PhoneGap
 *
 *
 */

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <PhoneGap/PGPlugin.h>


@interface WizAssetsPlugin : PGPlugin <UIWebViewDelegate> {
    
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
- (void)purgeEmptyDirectories:(NSArray*)arguments withDict:(NSDictionary*)optioURIError()

- (void)backgroundDownload:(NSArray*)arguments;



@end
