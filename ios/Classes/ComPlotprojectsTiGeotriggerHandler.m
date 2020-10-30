/**
 * Copyright 2015 Floating Market B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Appcelerator Titanium is Copyright (c) 2009-2010 by Appcelerator, Inc.
 * and licensed under the Apache Public License (version 2)
 */

#import <UIKit/UIKit.h>
#import "ComPlotprojectsTiGeotriggerHandler.h"
#import <UserNotifications/UserNotifications.h>
#import <PlotProjects/Plot.h>
#import "ComPlotprojectsTiImport.h"

static NSString* const geotriggerHandlerUrl = @"plotgeotriggerhandler.js";

@implementation ComPlotprojectsTiGeotriggerHandler

-(void)startHandler {
    TiApp* app = [TiApp app];
    bridge = [[KrollBridge alloc] initWithHost:app];
    
    NSURL *fileURL = [TiUtils toURL:geotriggerHandlerUrl relativeToURL:app.baseURL];
    NSURL *url = [TiUtils toURL:geotriggerHandlerUrl relativeToURL:fileURL];
    
    NSDictionary *preload = [NSDictionary dictionary];
    [bridge boot:nil url:url preload:preload];
}

-(void)shutdown {
    [bridge shutdown:nil];
}

@end
