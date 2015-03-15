/*
* Copyright (C) 2014 Opersys inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.opersys.processexplorer.tasks;

import android.content.res.AssetManager;

import java.io.File;

/**
 * Author: François-Denis Gonthier (francois-denis.gonthier@opersys.com)
 * Date: 27/03/14
 * Time: 3:36 PM
 */
public class AssetExtractTaskParams {

    public String assetPath;

    public String assetMd5sumPath;

    public File extractPath;

    public AssetManager assetManager;
}
